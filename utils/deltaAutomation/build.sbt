name := "delta-automation"

version := "1.0"

scalaVersion := "2.12.1"
enablePlugins(JavaAppPackaging)
libraryDependencies ++= Seq(
  "org.apache.poi" % "poi"              % "3.15-beta2",
  "org.apache.poi" % "poi-ooxml"        % "3.15-beta2",
  "org.apache.poi" % "poi-ooxml-schemas"% "3.15-beta2",
  "org.scalactic" %% "scalactic" % "3.0.1" % Test,
  "org.scalatest" %% "scalatest" % "3.0.1" % "test",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.5.0",
  "ch.qos.logback" % "logback-classic" % "1.1.7",
  "com.google.api-client" % "google-api-client-appengine" % "1.22.0",
  "com.google.gdata" % "core" % "1.47.1",
  "com.google.apis" % "google-api-services-gmail" % "v1-rev62-1.22.0",
  "com.typesafe" % "config" % "1.3.1",
  "com.google.apis" % "google-api-services-oauth2" % "v2-rev124-1.22.0",
  "com.typesafe.play" %% "play-json" % "2.6.0-M7",
  "com.google.apis" % "google-api-services-drive" % "v3-rev59-1.22.0",
  "org.scalaj" %% "scalaj-http" % "2.3.0"
  )
