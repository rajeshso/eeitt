name := "delta-automation"

version := "1.0"

scalaVersion := "2.12.1"


libraryDependencies ++= Seq(
  "org.apache.poi" % "poi"              % "3.15-beta2",
  "org.apache.poi" % "poi-ooxml"        % "3.15-beta2",
  "org.apache.poi" % "poi-ooxml-schemas"% "3.15-beta2",
  "org.scalactic" %% "scalactic" % "3.0.1",
  "org.scalatest" %% "scalatest" % "3.0.1" % "test",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.5.0",
  "ch.qos.logback" % "logback-classic" % "1.1.7"
)