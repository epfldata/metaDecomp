package decompositions

import decompositions.CostModel.getCumulativeCost
import decompositions.Hypergraph.{Vertex, Hyperedge}
import scala.collection.mutable
import scala.util.matching.Regex

case class QualifiedCol(hyperedge: Hyperedge, column: String)
case class OutputItem(qualifiedCol: QualifiedCol, aggregation: Option[String] = None) {
	def withoutAggregation: OutputItem = OutputItem(qualifiedCol)
}
case class FilterCondition(conditionText: String, referencedCols: Set[QualifiedCol])

trait PlanNode()(implicit sqlIR: sql.IR) {
	var projectTo: Set[OutputItem] = Set.empty
	val allJoinedRelations: Set[Hyperedge]
	val cumulativeCost: Double
	val cardinality: Double
	val inputCost: Double
	val intermediateCost: Double

	var joinTree: TreeNode = null

	override def toString: String = toString(0)
	def toString(depth: Int): String

	def generateSqlWithViews()(implicit sqlIR: sql.IR, createdViews: mutable.Set[String] = mutable.Set.empty): (String, String, String) // views, final, group by

	// Columns that will be used in some filter condition later (hence need to be kept)
	def columnsInFutureFilters: Iterable[OutputItem] =
		sqlIR.filterConditions.values // Set[Set[FilterCondition[V]]
			.filter(conditions => {
				val hyperedges = conditions.flatMap(_.referencedCols).map(_.hyperedge)
				this.allJoinedRelations.intersect(hyperedges).nonEmpty && !hyperedges.subsetOf(this.allJoinedRelations)
			})
			.flatten // Set[FilterCondition[V]]
			.flatMap(_.referencedCols)
			.filter(qualifiedCol => this.allJoinedRelations.contains(qualifiedCol.hyperedge))
			.map(OutputItem(_, None))

	def toDot(implicit metadata: sql.IR): String
	def toDotNonRoot(parentName: String)(implicit metadata: sql.IR): String
}

class JoinNode(lhs: PlanNode, rhs: PlanNode)(implicit sqlIR: sql.IR) extends PlanNode {

	val allJoinedRelations: Set[Hyperedge] = lhs.allJoinedRelations ++ rhs.allJoinedRelations
	val cardinality: Double = if sqlIR.cardinalities.isEmpty then 0 else sqlIR.cardinalities(allJoinedRelations)
	val cumulativeCost: Double = getCumulativeCost(lhs, rhs)
	val inputCost: Double = lhs.inputCost + rhs.inputCost
	val intermediateCost: Double = lhs.intermediateCost + rhs.intermediateCost + this.cardinality

	override def generateSqlWithViews()(implicit sqlIR: sql.IR, createdViews: mutable.Set[String] = mutable.Set.empty): (String, String, String) = {

		// Add the attributes that will be used in some filter condition later
		this.projectTo ++= columnsInFutureFilters
		
		val joinConditionColumnPairs =
			lhs.allJoinedRelations.flatMap(_.nodes).map(_.name).filter(!_.endsWith("_unique_attribute"))
				.intersect(rhs.allJoinedRelations.flatMap(_.nodes).map(_.name).filter(!_.endsWith("_unique_attribute"))) // The intersecting hypergraph nodes
				.flatMap(n => {
					val nodeRealColNames = sqlIR.vertexIdToColumnName(n) // Map hyperedge -> real column name in that table
					val lhsAttributesInJoin = lhs.allJoinedRelations.intersect(nodeRealColNames.keySet).map(h => OutputItem(QualifiedCol(h, nodeRealColNames(h))))
					val rhsAttributesInJoin = rhs.allJoinedRelations.intersect(nodeRealColNames.keySet).map(h => OutputItem(QualifiedCol(h, nodeRealColNames(h))))
					Seq((lhsAttributesInJoin.head, rhsAttributesInJoin.head))
				})

		// Output columns that have already appeared + columns used in the join conditions
		lhs.projectTo = this.projectTo.filter(outputCol => lhs.allJoinedRelations.contains(outputCol.qualifiedCol.hyperedge)).map(_.withoutAggregation) ++ joinConditionColumnPairs.map(_._1)
		rhs.projectTo = this.projectTo.filter(outputCol => rhs.allJoinedRelations.contains(outputCol.qualifiedCol.hyperedge)).map(_.withoutAggregation) ++ joinConditionColumnPairs.map(_._2)

		val dedupLeft =
			!(lhs.isInstanceOf[ScanNode] && lhs.projectTo != joinConditionColumnPairs.map(_._1).toSet) &&
			lhs.projectTo.size <= 2 &&
			lhs.cardinality > 1e6 &&
			lhs.cardinality * rhs.cardinality > 1e10 &&
			this.projectTo.size >= 4
		val dedupRight =
			!(rhs.isInstanceOf[ScanNode]) && 
			rhs.projectTo.size <= 2 && 
			rhs.cardinality > 1e6 && 
			lhs.cardinality * rhs.cardinality > 1e10 && 
			this.projectTo.size >= 4
		
		val lhsViewName =
			// lhs.allJoinedRelations.map(_.alias).mkString("", "_", "_view")
			lhs match {
				case scanNode: ScanNode if !dedupLeft => scanNode.hyperedge.tableName
				case _ => lhs.allJoinedRelations.map(_.alias).mkString("", "_", "_view")
			}
		val rhsViewName =
			// rhs.allJoinedRelations.map(_.alias).mkString("", "_", "_view")
			rhs match {
				case scanNode: ScanNode if !dedupRight => scanNode.hyperedge.tableName
				case _ => rhs.allJoinedRelations.map(_.alias).mkString("", "_", "_view")
			}

		val joinConditions =
			joinConditionColumnPairs
				.map((lhsAttr, rhsAttr) =>
					// s"${lhsViewName}.${lhsAttr.qualifiedCol.hyperedge.alias}_${lhsAttr.qualifiedCol.column} = ${rhsViewName}.${rhsAttr.qualifiedCol.hyperedge.alias}_${rhsAttr.qualifiedCol.column}"
					s"${lhs match { case scanNode: ScanNode if !dedupLeft => s"${scanNode.hyperedge.alias}." case _ => s"${lhsViewName}.${lhsAttr.qualifiedCol.hyperedge.alias}_"}}${lhsAttr.qualifiedCol.column}" +
					" = " +
					s"${rhs match { case scanNode: ScanNode if !dedupRight => s"${scanNode.hyperedge.alias}." case _ => s"${rhsViewName}.${rhsAttr.qualifiedCol.hyperedge.alias}_"}}${rhsAttr.qualifiedCol.column}"
				)
				.mkString(" AND ")

		// Filter conditions that can be applied but not yet covered by the lhs or rhs
		val filterConditions = 
			sqlIR.filterConditions.keySet
				.filter(hyperedges => hyperedges.subsetOf(this.allJoinedRelations) && !hyperedges.subsetOf(lhs.allJoinedRelations) && !hyperedges.subsetOf(rhs.allJoinedRelations))
				.flatMap(sqlIR.filterConditions(_))

		val (leftViewSqls, leftFinalSql, lhsGroupBy) = lhs.generateSqlWithViews()
		val (rightViewSqls, rightFinalSql, rhsGroupBy) = rhs.generateSqlWithViews()

		val viewSqls =
			leftViewSqls +
				rightViewSqls +
				(if createdViews.contains(lhsViewName)
					|| lhs.isInstanceOf[ScanNode] && !dedupLeft
					then ""
					else 
						"CREATE OR REPLACE TEMP VIEW " + lhsViewName + " AS \n" +
						leftFinalSql +
						(if dedupLeft then "\n" + lhsGroupBy else "") 
						+
						";\n" 
				) + 
				(if createdViews.contains(rhsViewName)
					|| rhs.isInstanceOf[ScanNode] && !dedupRight
					then ""
					else 
						"CREATE OR REPLACE TEMP VIEW " + rhsViewName + " AS \n" +
						rightFinalSql +
						(if dedupRight then "\n" + rhsGroupBy else "") 
						+
						";\n"
				)

		createdViews += lhsViewName
		createdViews += rhsViewName

		val filterConditionsText =
			(filterConditions.map(_.conditionText).map(c => {
					if (allJoinedRelations.size > 1) then (new Regex("""([a-zA-Z]\w*)\.([a-zA-Z]\w*)""")).replaceAllIn(c, m => {
						val outputItem = OutputItem(QualifiedCol(sqlIR.hyperedges.find(_.alias == m.group(1)).get, m.group(2)))
						if lhs.isInstanceOf[ScanNode] && !dedupLeft && lhs.projectTo.contains(outputItem) || rhs.isInstanceOf[ScanNode] && !dedupRight && rhs.projectTo.contains(outputItem)
						then m.group(1) + "." + m.group(2)
						else
							m.group(1) + "_" + m.group(2) // "$1_$2"
					}) else c
				})
				++
				(lhs match {
					case scanNode: ScanNode if !dedupLeft => scanNode.filterConditions.map(_.conditionText)
					case _ => Set.empty
				})
				++
				(rhs match {
					case scanNode: ScanNode if !dedupRight => scanNode.filterConditions.map(_.conditionText)
					case _ => Set.empty
				})
			).mkString(" AND ")

		val finalSql =
			"SELECT "
				+ projectTo.map(outputColumn => outputColumn.aggregation match {
				case Some(aggregation) => s"${outputColumn.qualifiedCol.hyperedge.alias}_${outputColumn.qualifiedCol.column}"
				case None => {
					val renamedColumnName =
						if lhs.isInstanceOf[ScanNode] && !dedupLeft && lhs.projectTo.contains(outputColumn) || rhs.isInstanceOf[ScanNode] && !dedupRight && rhs.projectTo.contains(outputColumn)
						then s"${outputColumn.qualifiedCol.hyperedge.alias}.${outputColumn.qualifiedCol.column} AS ${outputColumn.qualifiedCol.hyperedge.alias}_${outputColumn.qualifiedCol.column}"
						else
							s"${outputColumn.qualifiedCol.hyperedge.alias}_${outputColumn.qualifiedCol.column}"
					if (lhs.projectTo.contains(outputColumn) && rhs.projectTo.contains(outputColumn)) {
						s"${lhsViewName}.${renamedColumnName} AS ${renamedColumnName}"
					} else {
						renamedColumnName
					}
				}
			}).mkString(", ")
				+ "\nFROM " + lhsViewName 
				+ (lhs match { case scanNode: ScanNode if !dedupLeft => " AS " + scanNode.hyperedge.alias case _ => "" }) 
				+ ", "
				+ rhsViewName
				+ (rhs match { case scanNode: ScanNode if !dedupRight => " AS " + scanNode.hyperedge.alias case _ => "" })
				+ (if filterConditionsText.isEmpty && joinConditions.isEmpty then "" else "\nWHERE " + Seq(filterConditionsText, joinConditions).filter(_.nonEmpty).mkString(" AND "))

		val groupBy = "GROUP BY " + projectTo.map(outputColumn => outputColumn.aggregation match {
			case Some(aggregation) => s"${outputColumn.qualifiedCol.hyperedge.alias}_${outputColumn.qualifiedCol.column}"
			case None => {
				val renamedColumnName =
					if lhs.isInstanceOf[ScanNode] && !dedupLeft && lhs.projectTo.contains(outputColumn) || rhs.isInstanceOf[ScanNode] && !dedupRight && rhs.projectTo.contains(outputColumn)
					then s"${outputColumn.qualifiedCol.hyperedge.alias}.${outputColumn.qualifiedCol.column}"
					else
						s"${outputColumn.qualifiedCol.hyperedge.alias}_${outputColumn.qualifiedCol.column}"
				if (lhs.projectTo.contains(outputColumn) && rhs.projectTo.contains(outputColumn)) {
					s"${lhsViewName}.${renamedColumnName}"
				} else {
					renamedColumnName
				}
			}
		}).mkString(", ")

		(viewSqls, finalSql, groupBy)
	}

	override def toString(depth: Int): String = {
		val outerIndent = "| " * (2 * depth)
		val innerIndent = "| " * (2 * depth + 1)
		s"""
${outerIndent}JoinNode {
${innerIndent}lhs: ${lhs.toString(depth + 1)}
${innerIndent}rhs: ${rhs.toString(depth + 1)}
$outerIndent}"""
	}

	def toDot(implicit metadata: sql.IR): String = {
		val id = allJoinedRelations.map(_.alias).mkString("_")
		s"""graph\"\" {
	$id ;
	$id [label = \"⋈ ${if metadata.cardinalities != null then metadata.cardinalities.getOrElse(allJoinedRelations, "") else ""}\"] ;
${lhs.toDotNonRoot(id)}${rhs.toDotNonRoot(id)}}
"""
	}

	def toDotNonRoot(parentId: String)(implicit metadata: sql.IR): String = {
		val id = allJoinedRelations.map(_.alias).mkString("_")
		s"""  $parentId -- $id ;
	$id [label = \"⋈ ${if metadata.cardinalities != null then metadata.cardinalities.getOrElse(allJoinedRelations, "") else ""}\"] ;
${lhs.toDotNonRoot(id)}${rhs.toDotNonRoot(id)}
"""
	}
}

class ScanNode(val hyperedge: Hyperedge)(implicit sqlIR: sql.IR) extends PlanNode {

	val allJoinedRelations: Set[Hyperedge] = Set(hyperedge)
	val cardinality: Double = if sqlIR.cardinalities.isEmpty then 0.0 else sqlIR.cardinalities(allJoinedRelations)
	val cumulativeCost: Double = 0.0
	val inputCost: Double = cardinality
	val intermediateCost: Double = 0.0

	val filterConditions = sqlIR.filterConditions.keySet.filter(_ == Set(hyperedge)).flatMap(sqlIR.filterConditions)

	override def generateSqlWithViews()(implicit sqlIR: sql.IR, createdViews: mutable.Set[String] = mutable.Set.empty): (String, String, String) = {
		
		this.projectTo ++= columnsInFutureFilters

		(
			"",
			// "",
			// ""
			"SELECT " + projectTo.map(outputColumn => s"${outputColumn.qualifiedCol.hyperedge.alias}.${outputColumn.qualifiedCol.column} AS ${outputColumn.qualifiedCol.hyperedge.alias}_${outputColumn.qualifiedCol.column}").mkString(", ") + "\n"
				+ "FROM " + hyperedge.tableName + " AS " + hyperedge.alias
				+ (if filterConditions.isEmpty then "" else "\n" + "WHERE " + filterConditions.map(_.conditionText).mkString(" AND ")),
			"GROUP BY " + projectTo.map(outputColumn => s"${outputColumn.qualifiedCol.hyperedge.alias}.${outputColumn.qualifiedCol.column}").mkString(", ")
		)
	}

	override def toString(depth: Int): String = f"Scan ${hyperedge.nodes.mkString(", ")}"

	def toDot(implicit metadata: sql.IR): String = {
		val id = hyperedge.alias
		s"""graph\"\" {
	$id ;
	$id [label = \"$id\"] ;
}
"""
	}

	def toDotNonRoot(parentId: String)(implicit metadata: sql.IR): String = {
		val id = hyperedge.alias
		s"""  $parentId -- $id ;
	$id [label = \"$id\"] ;
"""
	}
}
