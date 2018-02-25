name := """mm-api"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

resolvers += Resolver.sonatypeRepo("snapshots")
resolvers += "lightshed-maven" at "http://dl.bintray.com/content/lightshed/maven"

scalaVersion := "2.12.2"

libraryDependencies ++= Seq(
  guice,
  "org.scalatestplus.play" %% "scalatestplus-play" % "3.0.0" % Test,
  "com.h2database" % "h2" % "1.4.194",
  "org.scalaj" %% "scalaj-http" % "2.3.0",
  "org.json4s" %% "json4s-jackson" % "3.5.0",
  "org.json4s" %% "json4s-native" % "3.5.0",
  "org.apache.commons" % "commons-text" % "1.1",
  "ch.lightshed" %% "courier" % "0.1.4",
  "com.google.apis" % "google-api-services-oauth2" % "v2-rev83-1.19.1",
  "com.google.apis" % "google-api-services-sheets" % "v4-rev484-1.22.0",
  "com.dropbox.core" % "dropbox-core-sdk" % "3.0.6",
  "org.apache.poi" % "poi-ooxml" % "3.17",
  "org.apache.poi" % "poi" % "3.17"
)


javaOptions += "-Dhttps.port=9443"
javaOptions += "-Dhttp.port=disabled"
