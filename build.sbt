val scala3Version = "3.3.1"

/*
enablePlugins(ScalaNativePlugin)

import scala.scalanative.build._

// defaults set with common options shown
nativeConfig ~= { c =>
  c.withLTO(LTO.none) // thin
//    .withMode(Mode.debug) // releaseFast
    .withMode(Mode.release)
    .withGC(GC.immix) // commix
}
*/


lazy val root = project
  .in(file("."))
  .settings(
    name         := "metaDecomp",
    version      := "0.1.0-SNAPSHOT",
    scalaVersion := scala3Version,

    Test / parallelExecution := false,

    libraryDependencies ++= Seq(
      "org.scalameta" %% "munit" % "0.7.29" % Test,
      "org.scala-lang" %% "scala3-staging" % scalaVersion.value,
//      "org.scala-lang.modules" %% "scala-parser-combinators" % "2.3.0",

      "com.lihaoyi" %% "fastparse" % "3.0.2",    // non-native
//      "com.lihaoyi" %%% "cssparse" % "3.0.2"  // native

      "org.duckdb" % "duckdb_jdbc" % "1.2.2.0"
    )
  )

enablePlugins(JavaAppPackaging)