ThisBuild / version := "0.6.0"

ThisBuild / scalaVersion := "2.13.7"

lazy val root = (project in file("."))
  .settings(
    name := "RelayServer",
    assembly / assemblyJarName := "radioware-relay-server.jar",
  )

val VERSIONS = Map(
  "slf4j"      -> "1.7.32",
  "logback"    -> "1.2.8",
  "logstash"   -> "7.0.1",
  "log4cats"   -> "2.1.1",
  "fs2"        -> "3.2.2",
  "catsEffect" -> "3.3.0",
  "tapir"      -> "0.19.1",
  "http4"      -> "0.23.7",
  "circe"      -> "0.14.1",
  "pureconfig" -> "0.14.1"
)

libraryDependencies ++= Seq(
  "org.slf4j" % "slf4j-api" % VERSIONS("slf4j"),
  "org.slf4j" % "jcl-over-slf4j" % VERSIONS("slf4j"),
  "org.slf4j" % "jul-to-slf4j" % VERSIONS("slf4j"),
  "org.slf4j" % "log4j-over-slf4j" % VERSIONS("slf4j"),
  "ch.qos.logback" % "logback-core" % VERSIONS("logback"),
  "ch.qos.logback" % "logback-classic" % VERSIONS("logback"),
  "org.typelevel" %% "cats-effect" % VERSIONS("catsEffect") withSources () withJavadoc (),
  "co.fs2" %% "fs2-core" % VERSIONS("fs2"),
  "co.fs2" %% "fs2-io" % VERSIONS("fs2"),
  "net.logstash.logback" % "logstash-logback-encoder" % VERSIONS("logstash"),
  "org.typelevel" %% "log4cats-slf4j" % VERSIONS("log4cats"),
  "com.softwaremill.sttp.tapir" %% "tapir-core" % VERSIONS("tapir"),
  "com.softwaremill.sttp.tapir" %% "tapir-cats" % VERSIONS("tapir"),
  "com.softwaremill.sttp.tapir" %% "tapir-http4s-server" % VERSIONS("tapir"),
  "io.circe" %% "circe-core" % VERSIONS("circe"),
  "io.circe" %% "circe-generic" % VERSIONS("circe"),
  "io.circe" %% "circe-parser" % VERSIONS("circe"),
  "com.github.pureconfig" %% "pureconfig" % VERSIONS("pureconfig"),
  "org.scalatest" %% "scalatest" % "3.2.11" % "test"
)

scalacOptions ++= Seq(
  "-feature",
  "-deprecation",
  "-unchecked",
  "-language:postfixOps"
)

assembly / assemblyMergeStrategy := {
  case PathList("META-INF", _ @_*) => MergeStrategy.discard
  case _                           => MergeStrategy.first
}
