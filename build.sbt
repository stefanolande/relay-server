ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.7"

lazy val root = (project in file("."))
  .settings(
    name := "RelayServer"
  )

val fs2Version        = "3.2.2"
val catsEffectVersion = "3.3.0"

libraryDependencies ++= Seq(
  "org.typelevel" %% "cats-effect" % catsEffectVersion withSources () withJavadoc (),
  "co.fs2" %% "fs2-core" % fs2Version,
  "co.fs2" %% "fs2-io" % fs2Version,
  "ch.qos.logback" % "logback-classic" % "1.2.7",
  "net.logstash.logback" % "logstash-logback-encoder" % "7.0.1",
  "org.typelevel" %% "log4cats-slf4j" % "2.1.1"
)

scalacOptions ++= Seq(
  "-feature",
  "-deprecation",
  "-unchecked",
  "-language:postfixOps"
)