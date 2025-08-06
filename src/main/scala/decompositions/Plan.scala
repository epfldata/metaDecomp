package decompositions

import decompositions.CostModel.getCumulativeCost
import sql.{Attribute, Relation}

case class QualifiedCol(hyperedge: Relation, column: String)
case class OutputItem(qualifiedCol: QualifiedCol, aggregation: Option[String] = None) {
	def withoutAggregation: OutputItem = OutputItem(qualifiedCol)
}
case class FilterCondition(conditionText: String, referencedCols: Set[QualifiedCol])

trait PlanNode()(implicit sqlIR: sql.IR) {
	var projectTo: Set[OutputItem] = Set.empty
	val allJoinedRelations: Set[Relation]
	val cumulativeCost: Long
	val cardinality: Long
	val coutCost: Long

	var joinTree: TreeNode[Attribute, Relation] = null

	override def toString: String = toString(0)
	def toString(depth: Int): String

	def generateSqlWithViews()(implicit sqlIR: sql.IR): (String, String, String) // views, final, group by

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
}

case class JoinNode(lhs: PlanNode, rhs: PlanNode)(implicit sqlIR: sql.IR) extends PlanNode {

	val allJoinedRelations: Set[Relation] = lhs.allJoinedRelations ++ rhs.allJoinedRelations
	val cardinality: Long = if sqlIR.cardinalities.isEmpty then 0 else sqlIR.cardinalities(allJoinedRelations)
	val cumulativeCost: Long = getCumulativeCost(lhs, rhs)
	val coutCost: Long = lhs.coutCost + rhs.coutCost + this.cardinality

	override def generateSqlWithViews()(implicit sqlIR: sql.IR): (String, String, String) = {
		val joinConditionColumnPairs =
			lhs.allJoinedRelations.flatMap(_.nodes).map(_.name)
				.intersect(rhs.allJoinedRelations.flatMap(_.nodes).map(_.name)) // The intersecting hypergraph nodes
				.flatMap(n => {
					val nodeRealColNames = sqlIR.vertexIdToColumnName(n) // Map hyperedge -> real column name in that table
					val lhsAttributesInJoin = lhs.allJoinedRelations.intersect(nodeRealColNames.keySet).map(h => OutputItem(QualifiedCol(h, nodeRealColNames(h))))
					val rhsAttributesInJoin = rhs.allJoinedRelations.intersect(nodeRealColNames.keySet).map(h => OutputItem(QualifiedCol(h, nodeRealColNames(h))))
					for lAttr <- lhsAttributesInJoin; rAttr <- rhsAttributesInJoin yield (lAttr, rAttr)
				})

		// Output columns that have already appeared + columns used in the join conditions
		lhs.projectTo = this.projectTo.filter(outputCol => lhs.allJoinedRelations.contains(outputCol.qualifiedCol.hyperedge)).map(_.withoutAggregation) ++ joinConditionColumnPairs.map(_._1)
		rhs.projectTo = this.projectTo.filter(outputCol => rhs.allJoinedRelations.contains(outputCol.qualifiedCol.hyperedge)).map(_.withoutAggregation) ++ joinConditionColumnPairs.map(_._2)

		val joinConditions =
			joinConditionColumnPairs
				.map((lhsAttr, rhsAttr) => s"${lhsAttr.qualifiedCol.hyperedge.alias}_${lhsAttr.qualifiedCol.column} = ${rhsAttr.qualifiedCol.hyperedge.alias}_${rhsAttr.qualifiedCol.column}")
				.mkString(" AND ")

		// Filter conditions that can be applied but not yet covered by the lhs or rhs
		val filterConditions = sqlIR.filterConditions.keySet
			.filter(hyperedges => hyperedges.subsetOf(this.allJoinedRelations) && !hyperedges.subsetOf(lhs.allJoinedRelations) && !hyperedges.subsetOf(rhs.allJoinedRelations))
			.flatMap(sqlIR.filterConditions(_))

		// Add the attributes that will be used in some filter condition later
		this.projectTo ++= columnsInFutureFilters

		val (leftViewSqls, leftFinalSql, lhsGroupBy) = lhs.generateSqlWithViews()
		val (rightViewSqls, rightFinalSql, rhsGroupBy) = rhs.generateSqlWithViews()
		val lhsViewName = lhs.allJoinedRelations.map(_.alias).mkString("", "_", "_view")
		val rhsViewName = rhs.allJoinedRelations.map(_.alias).mkString("", "_", "_view")

		val viewSqls =
			leftViewSqls +
				rightViewSqls +
				"CREATE OR REPLACE TEMP VIEW " + lhsViewName + " AS \n" +
				leftFinalSql +
				(if lhs.projectTo.size == 1 && lhs.allJoinedRelations.size != 1 then "\n" + lhsGroupBy else "") +
				";\n" +
				"CREATE OR REPLACE TEMP VIEW " + rhsViewName + " AS \n" +
				rightFinalSql +
				(if rhs.projectTo.size == 1 && lhs.allJoinedRelations.size != 1 then "\n" + rhsGroupBy else "") +
				";\n"

		val finalSql =
			"SELECT "
				+ projectTo.map(outputColumn => outputColumn.aggregation match {
				case Some(aggregation) => s"${outputColumn.qualifiedCol.hyperedge.alias}_${outputColumn.qualifiedCol.column}"
				case None => s"${outputColumn.qualifiedCol.hyperedge.alias}_${outputColumn.qualifiedCol.column}"
			}).mkString(", ")
				+ "\nFROM " + lhsViewName + " JOIN " + rhsViewName + " ON " + joinConditions
				+ (if filterConditions.isEmpty then "" else "\nWHERE " + filterConditions.map(_.conditionText).mkString(" AND "))

		val groupBy = "GROUP BY " + projectTo.map(outputColumn => s"${outputColumn.qualifiedCol.hyperedge.alias}_${outputColumn.qualifiedCol.column}").mkString(", ")

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
}

case class ScanNode(hyperedge: Relation)(implicit sqlIR: sql.IR) extends PlanNode {

	val allJoinedRelations: Set[Relation] = Set(hyperedge)
	val cardinality: Long = if sqlIR.cardinalities.isEmpty then 0 else sqlIR.cardinalities(allJoinedRelations)
	val cumulativeCost: Long = 0
	val coutCost: Long = 0

	override def generateSqlWithViews()(implicit sqlIR: sql.IR): (String, String, String) = {
		val filterConditions = sqlIR.filterConditions.keySet.filter(_ == Set(hyperedge)).flatMap(sqlIR.filterConditions)

		this.projectTo ++= columnsInFutureFilters

		(
			"",
			"SELECT "
				+ projectTo.map(outputColumn => outputColumn.aggregation match {
				case Some(aggregation) => s"${outputColumn.qualifiedCol.hyperedge.alias}.${outputColumn.qualifiedCol.column}"
				case None => s"${outputColumn.qualifiedCol.hyperedge.alias}.${outputColumn.qualifiedCol.column} AS ${outputColumn.qualifiedCol.hyperedge.alias}_${outputColumn.qualifiedCol.column}"
			}).mkString(", ")
				+ "\n"
				+ "FROM " + hyperedge.name + " AS " + hyperedge.alias
				+ (if filterConditions.isEmpty then "" else "\n" + "WHERE " + filterConditions.map(_.conditionText).mkString(" AND ")),
			""
		)
	}

	override def toString(depth: Int): String = f"Scan ${hyperedge.nodes.mkString(", ")}"
}
