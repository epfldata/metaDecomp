package decompositions

import decompositions.Hypergraph.{Component, Hyperedge, Separator}
import scala.collection.mutable

object SharedWorkCyclicOptimizer {
  case class Statistics(dpCalls: Long, heuristicCalls: Long, cacheHits: Long, cacheMisses: Long, cacheEntries: Int)
  private case class Atom(relations: Long, cost: Double, cardinality: Double)
  private sealed trait Recipe
  private case class Input(atom: Atom) extends Recipe
  private case class Join(left: Recipe, right: Recipe) extends Recipe
  private case class Entry(cost: Double, recipe: Recipe)
}

/** The legacy optimizer remains in MetaDecompCyclicOptimizer.
  * Shares immutable local-DP recipes within one run, never mutable output plans.
  * Relation masks support up to 64 relations; larger queries use the legacy path.
  */
class SharedWorkCyclicOptimizer(val maxCacheEntries: Int = 50000)(implicit sqlIR: sql.IR)
    extends MetaDecompCyclicOptimizer {
  import SharedWorkCyclicOptimizer.*
  require(maxCacheEntries >= 0)

  private var statistics = Statistics(0, 0, 0, 0, 0)
  def lastStatistics: Statistics = statistics
  private var active: Option[Context] = None

  private class Context {
    val relations = sqlIR.hyperedges.toVector.sortBy(e => (e.alias, e.tableName, e.nodes.map(_.name).toVector.sorted.mkString(";")))
    val ids = relations.zipWithIndex.toMap
    val recipes = mutable.HashMap.empty[Vector[Atom], Entry]
    val cardinalities = mutable.LongMap.empty[Option[Double]]
    val planMasks = new java.util.IdentityHashMap[PlanNode, java.lang.Long]()
    var dpCalls = 0L
    var heuristicCalls = 0L
    var hits = 0L
    var misses = 0L

    def mask(edges: Iterable[Hyperedge]): Long = edges.foldLeft(0L)((m, e) => m | (1L << ids(e)))
    def planMask(plan: PlanNode): Long = {
      val known = planMasks.get(plan)
      if known != null then known.longValue
      else {
        val result = mask(plan.allJoinedRelations)
        planMasks.put(plan, result)
        result
      }
    }
    def relationSet(mask: Long): Set[Hyperedge] = {
      val result = Set.newBuilder[Hyperedge]
      var remaining = mask
      while remaining != 0 do {
        val bit = java.lang.Long.numberOfTrailingZeros(remaining)
        result += relations(bit)
        remaining &= remaining - 1
      }
      result.result()
    }
    def cardinality(mask: Long): Option[Double] =
      cardinalities.getOrElseUpdate(mask, sqlIR.cardinalities.get(relationSet(mask)))
    def statistics: Statistics = Statistics(dpCalls, heuristicCalls, hits, misses, recipes.size)
  }

  // A new context for every invocation also handles cardinality changes when an
  // optimizer instance is reused. Direct local calls receive their own context.
  private def inContext[A](f: Context => A): A = active match {
    case Some(context) => f(context)
    case None =>
      val context = new Context
      active = Some(context)
      try f(context)
      finally {
        statistics = context.statistics
        active = None
      }
  }

  override def optimizeLocalHeuristic(subplans: Set[PlanNode]): PlanNode = {
    active.foreach(c => c.heuristicCalls += 1)
    // Keep the original greedy rule, connectivity checks and orientation.
    super.optimizeLocalHeuristic(subplans)
  }

  override def optimizeLocalDP(subplans: Set[PlanNode]): PlanNode = {
    if sqlIR.hyperedges.size > 64 then return super.optimizeLocalDP(subplans)
    require(subplans.nonEmpty && subplans.size < 10, "Exact local DP requires 1–9 inputs")
    inContext { context =>
      context.dpCalls += 1
      val inputs = subplans.toVector.map { p =>
        Atom(context.planMask(p), p.cumulativeCost, p.cardinality) -> p
      }.sortWith { (a, b) =>
        val comparison = java.lang.Long.compareUnsigned(a._1.relations, b._1.relations)
        if comparison != 0 then comparison < 0
        else if a._1.cost != b._1.cost then a._1.cost < b._1.cost
        else a._1.cardinality < b._1.cardinality
      }
      val atoms = inputs.map(_._1)
      val available = mutable.Map.empty[Atom, mutable.Queue[PlanNode]]
      inputs.foreach((atom, plan) => available.getOrElseUpdate(atom, mutable.Queue.empty).enqueue(plan))
      def build(recipe: Recipe): PlanNode = recipe match {
        case Input(atom) => available(atom).dequeue()
        case Join(left, right) =>
          val lhs = build(left)
          val rhs = build(right)
          if lhs.cardinality > rhs.cardinality then new JoinNode(lhs, rhs) else new JoinNode(rhs, lhs)
      }
      context.recipes.get(atoms) match {
        case Some(entry) =>
          context.hits += 1
          build(entry.recipe)
        case None => {
          val size = 1 << inputs.size
          val union = new Array[Long](size)
          val cards = new Array[Double](size)
          val entries = new Array[Entry](size)
          var subset = 1
          while subset < size do {
            val bit = java.lang.Integer.numberOfTrailingZeros(subset)
            val rest = subset & (subset - 1)
            union(subset) = union(rest) | atoms(bit).relations
            context.cardinality(union(subset)).foreach { cardinality =>
              cards(subset) = cardinality
              if rest == 0 then entries(subset) = Entry(atoms(bit).cost, Input(atoms(bit)))
              else {
                // A multiset of atomic inputs, not just their union. Include costs
                // so a recipe cannot return a cost from a different child plan.
                val key = atoms.indices.iterator.filter(i => (subset & (1 << i)) != 0).map(atoms).toVector
                context.recipes.get(key) match {
                  case Some(entry) =>
                    context.hits += 1
                    entries(subset) = entry
                  case None =>
                    context.misses += 1
                    var bestCost = Double.PositiveInfinity
                    var bestLeft = 0
                    var left = (subset - 1) & subset
                    val anchor = subset & -subset
                    while left != 0 do {
                      val right = subset ^ left
                      // One orientation of each unordered partition is sufficient.
                      if (left & anchor) != 0 && entries(left) != null && entries(right) != null then {
                        val cost = entries(left).cost + entries(right).cost + cards(left) + cards(right)
                        if bestLeft == 0 || cost < bestCost then {
                          bestCost = cost
                          bestLeft = left
                        }
                      }
                      left = (left - 1) & subset
                    }
                    if bestLeft != 0 then {
                      val entry = Entry(bestCost, Join(entries(bestLeft).recipe, entries(subset ^ bestLeft).recipe))
                      entries(subset) = entry
                      // Stop admitting entries when full; correctness never depends
                      // on cache residency. No cross-query cache is retained.
                      if context.recipes.size < maxCacheEntries then context.recipes(key) = entry
                    }
                }
              }
            }
            subset += 1
          }
          require(entries(size - 1) != null, "No valid local join plan for these inputs")
          build(entries(size - 1).recipe)
        }
      }
    }
  }

  override def runOptimalDecomposition(hypergraph: Hypergraph, graph: MetaDecompGraph): (PlanNode, MetaDecompGraph) = {
    if sqlIR.hyperedges.size > 64 then super.runOptimalDecomposition(hypergraph, graph)
    else inContext(_ => super.runOptimalDecomposition(hypergraph, graph))
  }

  override def run(hypergraph: Hypergraph, graph: MetaDecompGraph): PlanNode = {
    if sqlIR.hyperedges.size > 64 then {
      statistics = Statistics(0, 0, 0, 0, 0)
      return super.run(hypergraph, graph)
    }
    inContext { context =>
      val edgeToPlan = mutable.Map.empty[(Separator, Separator), PlanNode]
      val bestChildren = mutable.Map.empty[(Separator, Component), PlanNode]
      val coveredRelations = mutable.Map.empty[Separator, Long]
      val components = mutable.Map.empty[(Separator, Component), Vector[Component]]
      def best(s: Separator, c: Component): PlanNode = bestChildren.getOrElseUpdate((s, c), {
        // Preserve the legacy selection expression, including its unspecified
        // identity-set tie order. Coverage-distinct ties are not interchangeable.
        graph.adjList(s)(c).map(t => edgeToPlan((s, t))).minBy(_.cumulativeCost)
      })
      graph.sortedEdges.foreach { (r, s, c) =>
        val childComponents = components.getOrElseUpdate((s, c),
          graph.adjList(s).keysIterator.filter(_.subsetOf(c)).toVector)
        val children = childComponents.map(child => best(s, child)).toSet
        val covered = coveredRelations.getOrElseUpdate(s, {
          val vertices = s.nodes
          context.mask(hypergraph.edges.filter(_.nodes.subsetOf(vertices)))
        })
        val remaining = covered & ~children.foldLeft(0L)((m, p) => m | context.planMask(p))
        val scans = context.relationSet(remaining).map(ScanNode(_))
        edgeToPlan((r, s)) = optimizeLocal(children ++ scans)
      }
      val root = Set.empty[Hyperedge]
      val plan = best(root, hypergraph.vertices)
      plan.projectTo = sqlIR.outputAttributes
      plan
    }
  }
}
