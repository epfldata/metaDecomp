package sql

import decompositions.{FilterCondition, HyperEdge, OutputItem, QualifiedCol}

import scala.collection.mutable

object SQLParser {
	private val tokens: mutable.Queue[String] = mutable.Queue.empty
	private var currentToken: Option[String] = None

	private case class OutputAttribute(table: String, column: String, aggregation: Option[String], alias: Option[String])
	private case class TableAlias(table: String, alias: String)
	private case class Condition(expression: String, referencedTablesAndColumns: Set[(String, String)])

	def parse(query: String): IR = {
		tokenize(query)
		parseQuery()
	}

	// Splits query into tokens. Delimiters are whitespace, parentheses, commas, equals, and semicolons.
	private def tokenize(query: String): Unit = {
		tokens.clear()
		tokens.enqueueAll("""'[^']*'|"[^"]*"|\s+|<=|>=|!=|=|[(),;]|[^\s=(),;]+""".r.findAllIn(query).map(_.trim).filter(_.nonEmpty))
		nextToken()
	}

	private def nextToken(): Unit = {
		currentToken = if (tokens.nonEmpty) Some(tokens.dequeue()) else None
	}

	private def expect(expected: String*): Unit = {
		if (expected.contains(currentToken.get)) nextToken()
		else throw new IllegalArgumentException(s"Expected '$expected' but found '$currentToken'")
	}

	private def accept(expected: String*): Boolean = {
		expected.contains(currentToken.get)
	}

	private def lookahead(i: Int): Option[String] = {
		tokens.lift(i-1)
	}

	private def parseQuery() = {
		expect("SELECT")
		val outputAttributes = parseOutputAttributes()
		expect("FROM")
		val tableAliases = parseTableAliases()
		val (joinConditions, filterConditions) = if (currentToken.contains("WHERE")) {
			nextToken()
			parseConditions()
		} else (List.empty[Condition], List.empty[Condition])

		val tableAliasNameMap = tableAliases.map(alias => alias.alias -> alias.table).toMap

		// Group the joined columns
		val columnGroups: Set[Set[(String, String)]] = {
			val parent = mutable.Map[(String, String), (String, String)]()
			outputAttributes.foreach(attr => parent((attr.table, attr.column)) = (attr.table, attr.column))

			def find(x: (String, String)): (String, String) = {
				if (!parent.contains(x)) parent(x) = x
				if (parent(x) != x) parent(x) = find(parent(x))
				parent(x)
			}

			def union(x: (String, String), y: (String, String)): Unit = {
				val px = find(x)
				val py = find(y)
				if (px != py) parent(px) = py
			}

			joinConditions.foreach { cond =>
				val cols = cond.referencedTablesAndColumns.toList
				if (cols.size == 2) union(cols(0), cols(1))
			}
			parent.keys.foreach(find)
			parent.keys.groupBy(find).values.map(_.toSet).toSet
		}

		// Create hypergraph vertices for each joined group or output column
		val columnToVertexIdMap = mutable.Map[(String, String), Int]()
		columnGroups.zipWithIndex.foreach((group, i) => group.foreach(col => columnToVertexIdMap(col) = i))

		// Create hyperedges with the merged join columns + output attributes
		val aliasHyperedgeMap =
			tableAliasNameMap
				.map((alias, name) => alias -> Relation(
					name, alias,
					columnToVertexIdMap
						.filter { case ((columnTableAlias, columnName), vertexId) => columnTableAlias == alias }
						.map((_, vertexId) => Attribute(vertexId.toString)).toSet)
				).toMap

		// Create the names map, aliases map, filter conditions map, real column names map
		val hyperedges: Set[Relation] = aliasHyperedgeMap.values.toSet // Set of hyperedges
		val hyperedgeRealTableNameMap: Map[HyperEdge[Attribute], String] = aliasHyperedgeMap.map((alias, hyperedge) => hyperedge -> tableAliasNameMap(alias)) // Hyperedge -> table name
		val hyperedgeAliasMap: Map[Relation, String] = aliasHyperedgeMap.map((alias, hyperedge) => hyperedge -> alias) // Hyperedge -> alias

		val subsetFilterConditionsMap: Map[Set[Relation], Set[FilterCondition]] =
			filterConditions.toSet
				.map { case condition @ Condition(expression, columns) => columns.map((tableName, columnName) => aliasHyperedgeMap(tableName)) -> condition } // Set of (hyperedges, condition)
				.groupBy((hyperedges, condition) => hyperedges.toSet) // Hyperedges -> Set of (hyperedges, condition)
				.view.mapValues(_.map((hyperedges, condition) =>
					FilterCondition(condition.expression, condition.referencedTablesAndColumns.map((table, column) => QualifiedCol(aliasHyperedgeMap(table), column)))
				)).toMap // Subset of hyperedges -> filter conditions

		val vertexIdRealColumnNamesMap: Map[String, Map[Relation, String]] =
			columnToVertexIdMap.toSet
				.map { case ((table, column), vertexId) => (vertexId -> (aliasHyperedgeMap(table) -> column)) } // Set of (vertexId, (hyperedge, columnName))
				.groupBy((vertexId, _) => vertexId.toString) // Vertex ID -> Set of (vertexId, (hyperedge, columnName))
				.view.mapValues(_.map { case (vertexId, (hyperedge, columnName)) => hyperedge -> columnName }.toMap).toMap // Vertex ID -> (hyperedge -> column name)

		val outputItems = outputAttributes.toSet.map(outputAttribute =>
			OutputItem(QualifiedCol(hyperedgeAliasMap.keySet.find(hyperedgeAliasMap(_) == outputAttribute.table).get, outputAttribute.column), outputAttribute.aggregation)
		)

		IR(hyperedges, subsetFilterConditionsMap, vertexIdRealColumnNamesMap, outputItems)
	}

	private def parseOutputAttributes(): List[OutputAttribute] = {
		val attributes = mutable.ListBuffer[OutputAttribute]()
		while (!accept("FROM")) {
			val agg = if (lookahead(1).nonEmpty && lookahead(1).get == "(") {
				val func = currentToken.get
				nextToken()
				expect("(")
				Some(func)
			} else None
			val tableColumn = currentToken.get.split("\\.")
			nextToken()
			if (currentToken.nonEmpty && currentToken.get == ")" && agg.isDefined) {
				nextToken()
			}
			val alias = if (currentToken.contains("AS")) {
				nextToken()
				val aliasName = currentToken.get
				nextToken()
				Some(aliasName)
			} else None
			attributes += OutputAttribute(tableColumn(0), tableColumn(1), agg, alias)
			if (currentToken.contains(",")) nextToken()
		}
		attributes.toList
	}

	private def parseTableAliases(): List[TableAlias] = {
		val aliases = mutable.ListBuffer[TableAlias]()
		while (currentToken.isDefined && !currentToken.contains("WHERE")) {
			val table = currentToken.get
			nextToken()
			val alias = if (currentToken.contains("AS")) {
				nextToken()
				val aliasName = currentToken.get
				nextToken()
				aliasName
			} else table
			aliases += TableAlias(table, alias)
			if (currentToken.contains(",")) nextToken()
		}
		aliases.toList
	}

	private def parseConditions(): (List[Condition], List[Condition]) = {
		val joinConditions = mutable.ListBuffer[Condition]()
		val filterConditions = mutable.ListBuffer[Condition]()
		while (currentToken.isDefined && !currentToken.contains(";") && !currentToken.contains("GROUP")) {
			val condition = parseCondition()
			val tables = condition.referencedTablesAndColumns.map((table, column) => table)
			// Here if we see a condition in the form "table1.column1 = table2.column2" with exactly two tables, we treat it as a join condition.
			if ("""^[a-zA-Z]\w*\.[a-zA-Z]\w* = [a-zA-Z]\w*\.[a-zA-Z]\w*""".r.matches(condition.expression) && tables.size == 2) {
				joinConditions += condition // Condition(condition.expression.replaceAll("""\.""", "_"), condition.referencedTablesAndColumns)
			} else {
				filterConditions += condition // (if condition.referencedTablesAndColumns.size == 1 then condition else Condition(condition.expression.replaceAll("""\.""", "_"), condition.referencedTablesAndColumns))
			}
			if (currentToken.contains("AND")) nextToken()
		}
		(joinConditions.toList, filterConditions.toList)
	}

	private def parseCondition(): Condition = {
		val expression = new StringBuilder
		val referencedColumns = mutable.Set[(String, String)]()
		var depth = 0
		var hasBetween = false
		while (currentToken.isDefined && currentToken.get != ";" && currentToken.get != "GROUP" && (currentToken.get != "AND" || depth > 0 || hasBetween)) {
			val token = currentToken.get
			if (token == "(") {
				depth += 1
			} else if (token == ")") {
				depth -= 1
				if (depth < 0) throw new IllegalArgumentException("Unmatched closing parenthesis")
			} else if (token == "BETWEEN") {
				hasBetween = true
			} else if (hasBetween && token == "AND") {
				hasBetween = false
			}
			expression.append(token)
			if ((token == "<" || token == ">" || token == "!") && lookahead(1).isDefined && lookahead(1).get == "=") {
				// Don't append the whitespace
			} else {
				expression.append(" ")
			}
			if (token.matches("""^[a-zA-Z]\w*\.[a-zA-Z]\w*""")) {
				val parts = token.split("\\.")
				referencedColumns += ((parts(0), parts(1)))
			}
			nextToken()
		}
		Condition(expression.toString.trim, referencedColumns.toSet)
	}
}
