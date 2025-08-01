package experiments

import experiments.Config.benchmarksPath

import java.io.File
import scala.io.Source

def readInOneLine(file: File): String = {
	val source = Source.fromFile(file)
	val result = source.getLines().mkString(" ")
	source.close()
	result
}
