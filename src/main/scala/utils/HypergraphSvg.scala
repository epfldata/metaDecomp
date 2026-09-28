package utils

import decompositions.Hypergraph
import decompositions.Hypergraph.{Hyperedge, Vertex}

import scala.collection.mutable

/** Draws a hypergraph directly as SVG.
  *
  * Unlike Graphviz clusters, SVG contours may overlap. Consequently every
  * vertex is drawn exactly once and a shared vertex lies inside every incident
  * hyperedge contour.
  */
object HypergraphSvg {
  private case class Point(x: Double, y: Double) {
    def +(other: Point): Point = Point(x + other.x, y + other.y)
    def -(other: Point): Point = Point(x - other.x, y - other.y)
    def *(factor: Double): Point = Point(x * factor, y * factor)
    def length: Double = math.hypot(x, y)
  }

  private val Palette = Vector(
    ("#2563eb", "#93c5fd"),
    ("#dc2626", "#fca5a5"),
    ("#059669", "#6ee7b7"),
    ("#7c3aed", "#c4b5fd"),
    ("#d97706", "#fcd34d"),
    ("#0891b2", "#67e8f9"),
    ("#db2777", "#f9a8d4"),
    ("#4d7c0f", "#bef264")
  )

  def render(
      hypergraph: Hypergraph,
      vertexDescriptions: Map[String, String] = Map.empty
  ): String = {
    val vertices = hypergraph.vertices.toVector.sortBy(_.name)
    val edges = hypergraph.edges.toVector.sortBy(edge => (edge.alias, edge.tableName))
    val rawPositions = orientLandscape(layout(vertices, edges))
    val scale = math.max(1.0, math.sqrt(math.max(1, vertices.size).toDouble / 10.0))
    val width = 900.0 * scale
    val height = 650.0 * scale
    val positions = fitToCanvas(rawPositions, width, height, 90.0)
    val orderedEdges = edges.zipWithIndex.sortBy { case (edge, _) => -edge.nodes.size }
    val builder = StringBuilder()

    builder ++= s"<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"${fmt(width)}\" " +
      s"height=\"${fmt(height)}\" viewBox=\"0 0 ${fmt(width)} ${fmt(height)}\">\n"
    builder ++= "  <rect width=\"100%\" height=\"100%\" fill=\"white\"/>\n"
    builder ++= "  <style>text { font-family: Helvetica, Arial, sans-serif; }</style>\n"

    orderedEdges.foreach { case (edge, originalIndex) =>
      val (stroke, fill) = Palette(originalIndex % Palette.size)
      val memberPoints = edge.nodes.toVector.flatMap(positions.get)
      val contour = bubbleContour(memberPoints, 42.0 + (originalIndex % 3) * 2.0)
      builder ++= s"  <path d=\"$contour\" fill=\"$fill\" fill-opacity=\"0.20\" " +
        s"stroke=\"$stroke\" stroke-width=\"3\" stroke-linejoin=\"round\">" +
        s"<title>${escape(edge.alias)}</title></path>\n"
      if (memberPoints.nonEmpty) {
        val labelX = memberPoints.map(_.x).min - 28.0 + (originalIndex % 4) * 18.0
        val labelY = memberPoints.map(_.y).min - 27.0 - (originalIndex % 3) * 9.0
        val label = if (edge.alias.nonEmpty) edge.alias else edge.tableName
        builder ++= s"  <text x=\"${fmt(labelX)}\" y=\"${fmt(labelY)}\" " +
          s"font-size=\"14\" font-weight=\"bold\" fill=\"$stroke\" " +
          s"paint-order=\"stroke\" stroke=\"white\" stroke-width=\"4\">" +
          s"${escape(label)}</text>\n"
      }
    }

    vertices.foreach { vertex =>
      val point = positions(vertex)
      val description = vertexDescriptions.getOrElse(vertex.name, vertex.name)
      builder ++= s"  <g><title>${escape(description)}</title>\n"
      builder ++= s"    <circle cx=\"${fmt(point.x)}\" cy=\"${fmt(point.y)}\" r=\"15\" " +
        "fill=\"white\" stroke=\"#1f2937\" stroke-width=\"2.2\"/>\n"
      builder ++= s"    <text x=\"${fmt(point.x)}\" y=\"${fmt(point.y + 4.0)}\" " +
        "text-anchor=\"middle\" font-size=\"11\" font-weight=\"bold\" " +
        s"fill=\"#111827\">${escape(vertex.name)}</text>\n"
      builder ++= "  </g>\n"
    }
    builder ++= "</svg>\n"
    builder.result()
  }

  /** Deterministic force-directed placement on the hypergraph's primal graph. */
  private def layout(vertices: Vector[Vertex], edges: Vector[Hyperedge]): Map[Vertex, Point] = {
    if (vertices.isEmpty) return Map.empty
    if (vertices.size == 1) return Map(vertices.head -> Point(0.0, 0.0))

    val positions = mutable.Map.from(vertices.zipWithIndex.map { case (vertex, index) =>
      val angle = 2.0 * math.Pi * index / vertices.size
      vertex -> Point(250.0 * math.cos(angle), 250.0 * math.sin(angle))
    })
    val adjacency = mutable.Map.empty[(Vertex, Vertex), Double].withDefaultValue(0.0)
    edges.foreach { edge =>
      val members = edge.nodes.toVector.sortBy(_.name)
      val weight = 1.0 / math.max(1, members.size - 1)
      members.indices.foreach { left =>
        (left + 1 until members.size).foreach { right =>
          val key = orderedPair(members(left), members(right))
          adjacency(key) = adjacency(key) + weight
        }
      }
    }

    val area = 500.0 * 500.0
    val idealDistance = math.sqrt(area / vertices.size) * 0.9
    var temperature = 45.0
    (0 until 650).foreach { _ =>
      val displacement = mutable.Map.from(vertices.map(_ -> Point(0.0, 0.0)))
      vertices.indices.foreach { left =>
        (left + 1 until vertices.size).foreach { right =>
          val a = vertices(left)
          val b = vertices(right)
          val delta = positions(a) - positions(b)
          val distance = math.max(0.01, delta.length)
          val force = idealDistance * idealDistance / distance
          val vector = delta * (force / distance)
          displacement(a) = displacement(a) + vector
          displacement(b) = displacement(b) - vector
        }
      }
      adjacency.foreach { case ((a, b), weight) =>
        val delta = positions(a) - positions(b)
        val distance = math.max(0.01, delta.length)
        val force = distance * distance / idealDistance * weight
        val vector = delta * (force / distance)
        displacement(a) = displacement(a) - vector
        displacement(b) = displacement(b) + vector
      }
      vertices.foreach { vertex =>
        val delta = displacement(vertex)
        val distance = math.max(0.01, delta.length)
        positions(vertex) = positions(vertex) + delta * (math.min(distance, temperature) / distance)
      }
      temperature *= 0.992
    }
    positions.toMap
  }

  private def fitToCanvas(
      positions: Map[Vertex, Point],
      width: Double,
      height: Double,
      margin: Double
  ): Map[Vertex, Point] = {
    if (positions.isEmpty) return positions
    val xs = positions.values.map(_.x)
    val ys = positions.values.map(_.y)
    val minX = xs.min
    val maxX = xs.max
    val minY = ys.min
    val maxY = ys.max
    val scaleX = (width - 2.0 * margin) / math.max(1.0, maxX - minX)
    val scaleY = (height - 2.0 * margin) / math.max(1.0, maxY - minY)
    val scale = math.min(scaleX, scaleY)
    positions.view.mapValues(point => Point(
      margin + (point.x - minX) * scale,
      margin + (point.y - minY) * scale
    )).toMap
  }

  private def orientLandscape(positions: Map[Vertex, Point]): Map[Vertex, Point] = {
    if (positions.isEmpty) return positions
    val xSpan = positions.values.map(_.x).max - positions.values.map(_.x).min
    val ySpan = positions.values.map(_.y).max - positions.values.map(_.y).min
    if (ySpan > xSpan) positions.view.mapValues(point => Point(point.y, point.x)).toMap
    else positions
  }

  /** Convex hull of equally sized discs: an approximate rounded bubble. */
  private def bubbleContour(points: Vector[Point], padding: Double): String = {
    if (points.isEmpty) return ""
    val samples = points.flatMap { point =>
      (0 until 16).map { index =>
        val angle = 2.0 * math.Pi * index / 16.0
        Point(point.x + padding * math.cos(angle), point.y + padding * math.sin(angle))
      }
    }
    val hull = convexHull(samples)
    if (hull.isEmpty) ""
    else hull.tail.foldLeft(s"M ${fmt(hull.head.x)} ${fmt(hull.head.y)}") { (path, point) =>
      path + s" L ${fmt(point.x)} ${fmt(point.y)}"
    } + " Z"
  }

  private def convexHull(points: Vector[Point]): Vector[Point] = {
    val sorted = points.distinct.sortBy(point => (point.x, point.y))
    if (sorted.size <= 2) return sorted
    def cross(origin: Point, a: Point, b: Point): Double =
      (a.x - origin.x) * (b.y - origin.y) - (a.y - origin.y) * (b.x - origin.x)
    def half(sequence: Vector[Point]): Vector[Point] = {
      val result = mutable.ArrayBuffer.empty[Point]
      sequence.foreach { point =>
        while (result.size >= 2 && cross(result(result.size - 2), result.last, point) <= 0.0)
          result.remove(result.size - 1)
        result += point
      }
      result.toVector
    }
    half(sorted).dropRight(1) ++ half(sorted.reverse).dropRight(1)
  }

  private def orderedPair(left: Vertex, right: Vertex): (Vertex, Vertex) =
    if (left.name <= right.name) (left, right) else (right, left)

  private def fmt(value: Double): String = f"$value%.2f"

  private def escape(value: String): String = value
    .replace("&", "&amp;")
    .replace("<", "&lt;")
    .replace(">", "&gt;")
    .replace("\"", "&quot;")
    .replace("'", "&apos;")
}
