ThisBuild / version := "0.6.0"

ThisBuild / scalaVersion := "2.13.7"

lazy val root = (project in file("."))
  .settings(
    name := "RelayServer",
    assembly / assemblyJarName := "radioware-relay-server.jar",
  )

val slf4jVersion      = "1.7.36"
val logbackVersion    = "1.2.11"
val logstashVersion   = "7.2"
val log4catsVersion   = "2.3.0"
val fs2Version        = "3.2.7"
val catsEffectVersion = "3.3.12"
val tapirVersion      = "0.20.2"
val http4Version      = "0.23.7"
val circeVersion      = "0.14.2"
val pureconfigVersion = "0.17.1"

libraryDependencies ++= Seq(
  "org.slf4j" % "slf4j-api" % slf4jVersion,
  "org.slf4j" % "jcl-over-slf4j" % slf4jVersion,
  "org.slf4j" % "jul-to-slf4j" % slf4jVersion,
  "org.slf4j" % "log4j-over-slf4j" % slf4jVersion,
  "ch.qos.logback" % "logback-core" % logbackVersion,
  "ch.qos.logback" % "logback-classic" % logbackVersion,
  "org.typelevel" %% "cats-effect" % catsEffectVersion withSources () withJavadoc (),
  "co.fs2" %% "fs2-core" % fs2Version,
  "co.fs2" %% "fs2-io" % fs2Version,
  "net.logstash.logback" % "logstash-logback-encoder" % logstashVersion,
  "org.typelevel" %% "log4cats-slf4j" % log4catsVersion,
  "com.softwaremill.sttp.tapir" %% "tapir-core" % tapirVersion,
  "com.softwaremill.sttp.tapir" %% "tapir-cats" % tapirVersion,
  "com.softwaremill.sttp.tapir" %% "tapir-http4s-server" % tapirVersion,
  "io.circe" %% "circe-core" % circeVersion,
  "io.circe" %% "circe-generic" % circeVersion,
  "io.circe" %% "circe-parser" % circeVersion,
  "com.github.pureconfig" %% "pureconfig" % pureconfigVersion,
  "org.scalatest" %% "scalatest" % "3.2.12" % "test"
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
