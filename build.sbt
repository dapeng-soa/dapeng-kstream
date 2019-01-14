
organization := "com.github.dapeng-soa"
name := "dapeng-kstream"

version := "0.1-SNAPSHOT"

scalaVersion := "2.12.8"

libraryDependencies ++= Seq(
  "javax.ws.rs" % "javax.ws.rs-api" % "2.1.1" artifacts(Artifact("javax.ws.rs-api", "jar", "jar")),
  "org.apache.kafka" %% "kafka" % "2.1.0",
  "org.apache.kafka" % "kafka-streams" % "2.1.0",
  "org.apache.kafka" % "kafka-clients" % "1.1.0",
  "org.apache.kafka" %% "kafka-streams-scala" % "2.1.0",
  "org.slf4j" % "slf4j-api" % "1.7.13",
  "ch.qos.logback" % "logback-classic" % "1.1.3",
  "ch.qos.logback" % "logback-core" % "1.1.3",
  "com.lihaoyi" % "ammonite" % "1.6.0" cross CrossVersion.full,
  "com.google.guava" % "guava" % "16.0.1",
  "org.apache.commons" % "commons-email" % "1.5",
  "apache-httpclient" % "commons-httpclient" % "3.1",
  "com.google.code.gson" % "gson" % "2.8.2",
  "commons-lang" % "commons-lang" % "2.6"
)