val myVersion = "0.0.1"

val ScalaLoggingVersion = "3.1.0"
val LogbackVersion = "1.1.6"
val TypesafeConfigVersion = "1.3.0"
val AkkaVersion = "2.5.11"
val AkkaHttpVersion = "10.1.0"
val Json4sVersion = "3.5.3"
val AkkaHttpJson4sVersion = "1.20.0"
val ElasticSearchVersion = "5.6.6"

lazy val commonSettings = Seq(
  organization := "com.foo",
  version := myVersion,
  scalaVersion := "2.11.2",
  resolvers ++=
    Seq("Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/",
      "Spray Repository" at "http://repo.spray.io"
    )
)

lazy val main = (project in file(".")).aggregate(core)

publishArtifact in main := false

lazy val core = (project in file("core")).
  settings(commonSettings:_*).
  settings(
    Seq(
      name := "skinny-core"
    ):_*).
  settings(
    libraryDependencies ++= Seq(
      "com.github.java-json-tools" % "json-schema-validator" % "2.2.10",
      "com.typesafe.scala-logging" %% "scala-logging" % ScalaLoggingVersion,
      "com.typesafe.akka" %% "akka-actor" % AkkaVersion,
      "com.typesafe.akka" %% "akka-slf4j" % AkkaVersion,
      "com.typesafe.akka" %% "akka-testkit" % AkkaVersion % "test",
      "com.typesafe.akka" %% "akka-http" % AkkaHttpVersion,
      "com.typesafe.akka" %% "akka-http-testkit" % AkkaHttpVersion % "test",
      "de.heikoseeberger" %% "akka-http-json4s" % AkkaHttpJson4sVersion,
      "org.json4s" %% "json4s-jackson" % Json4sVersion,
      "ch.qos.logback" % "logback-classic" % LogbackVersion,
      "org.scalatest" %% "scalatest" % "3.0.5" % "test",
      "com.typesafe" % "config" % TypesafeConfigVersion
    )
  )

scalacOptions ++= Seq(
  "-feature",
  "-unchecked",
  "-language:implicitConversions",
  "-language:existentials",
  "-language:postfixOps")

fork in test := true

