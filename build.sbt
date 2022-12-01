ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.10"

lazy val root = (project in file("."))
  .settings(
    name := "rivulet"
  )
val AkkaVersion = "2.7.0"
val testcontainersVersion = "1.17.5"
val scalatestVersion = "3.1.4"
libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-stream-kafka" % "4.0.0",
  "com.typesafe.akka" %% "akka-stream" % AkkaVersion,
  "com.typesafe.akka" %% "akka-http" % "10.4.0",
  "org.typelevel" %% "cats-core" % "2.9.0",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.5",
  "ch.qos.logback" % "logback-classic" % "1.4.5",
  "ch.qos.logback" % "logback-classic" % "1.4.5" % Test,
  "com.typesafe.akka" %% "akka-http-spray-json" % "10.4.0",
  "com.typesafe.akka" %% "akka-stream-testkit" % AkkaVersion % Test,
  "com.typesafe.akka" %% "akka-stream-kafka-testkit" % "2.1.1" % Test,
  "org.testcontainers" % "kafka" % testcontainersVersion % Test,
  "org.scalatest" %% "scalatest" % scalatestVersion % Test
)
addCommandAlias("run_client", "runMain org.ab.Main")