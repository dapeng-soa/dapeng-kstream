import java.io.{FileInputStream, FileOutputStream}

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

mainClass in assembly := Some("com.dapeng.kstream.ThreadMain")

lazy val dist = taskKey[File]("make a dist scompose file")

dist := {
  val assemblyJar = assembly.value

  val distJar = new java.io.File(target.value, "dapengKStream")
  val out = new FileOutputStream(distJar)

  out.write(
    """#!/usr/bin/env sh
      |exec java -jar -XX:+UseG1GC "$0" "$@"
      |""".stripMargin.getBytes)

  val inStream = new FileInputStream(assemblyJar)
  val buffer = new Array[Byte](1024)

  while( inStream.available() > 0) {
    val length = inStream.read(buffer)
    out.write(buffer, 0, length)
  }

  out.close

  distJar.setExecutable(true, false)
  println(s"build dapengKStream at ${distJar.getAbsolutePath}" )

  distJar
}