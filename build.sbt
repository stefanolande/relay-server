ThisBuild / version := "0.7.0"

ThisBuild / scalaVersion := "3.2.0"

lazy val root = (project in file("."))
  .settings(
    name := "RelayServer",
    assembly / assemblyJarName := "radioware-relay-server.jar"
  )

val logbackVersion    = "1.4.4"
val logstashVersion   = "7.2"
val log4catsVersion   = "2.5.0"
val fs2Version        = "3.3.0"
val catsEffectVersion = "3.3.14"
val tapirVersion      = "1.1.4"
val http4sVersion     = "0.23.16"
val blazeVersion      = "0.23.12"
val circeVersion      = "0.14.3"
val pureconfigVersion = "0.17.1"
val scalaTestVersion  = "3.2.14"

libraryDependencies ++= Seq(
  "org.typelevel" %% "cats-effect" % catsEffectVersion withSources () withJavadoc (),
  "co.fs2" %% "fs2-core" % fs2Version,
  "co.fs2" %% "fs2-io" % fs2Version,
  "org.typelevel" %% "log4cats-slf4j" % log4catsVersion,
  "ch.qos.logback" % "logback-classic" % logbackVersion,
  "net.logstash.logback" % "logstash-logback-encoder" % logstashVersion,
  "com.softwaremill.sttp.tapir" %% "tapir-core" % tapirVersion,
  "com.softwaremill.sttp.tapir" %% "tapir-cats" % tapirVersion,
  "com.softwaremill.sttp.tapir" %% "tapir-http4s-server" % tapirVersion,
  "io.circe" %% "circe-core" % circeVersion,
  "io.circe" %% "circe-generic" % circeVersion,
  "io.circe" %% "circe-parser" % circeVersion,
  "com.github.pureconfig" %% "pureconfig-core" % pureconfigVersion,
  "org.scalatest" %% "scalatest" % scalaTestVersion % "test",
  "org.http4s" %% "http4s-dsl" % http4sVersion,
  "org.http4s" %% "http4s-blaze-server" % blazeVersion
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
