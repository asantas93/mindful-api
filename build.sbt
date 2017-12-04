name := """mm-api"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

resolvers += Resolver.sonatypeRepo("snapshots")
resolvers += "lightshed-maven" at "http://dl.bintray.com/content/lightshed/maven"

scalaVersion := "2.12.2"

libraryDependencies += guice
libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "3.0.0" % Test
libraryDependencies += "com.h2database" % "h2" % "1.4.194"
libraryDependencies += "org.scalaj" %% "scalaj-http" % "2.3.0"
libraryDependencies += "org.json4s" %% "json4s-jackson" % "3.5.0"
libraryDependencies += "org.json4s" %% "json4s-native" % "3.5.0"
libraryDependencies += "org.apache.commons" % "commons-text" % "1.1"
libraryDependencies += "ch.lightshed" %% "courier" % "0.1.4"
libraryDependencies += "com.google.apis" % "google-api-services-oauth2" % "v2-rev83-1.19.1"
libraryDependencies += "com.google.apis" % "google-api-services-sheets" % "v4-rev484-1.22.0"

javaOptions += "-Dhttps.port=9443"
javaOptions += "-Dhttp.port=disabled"
