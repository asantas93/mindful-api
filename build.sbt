lazy val `mindful-api` = (project in file("."))
  .settings(
	  libraryDependencies ++= Seq(
      "com.amazonaws" % "aws-lambda-java-core" % "1.4.0",
      "com.amazonaws" % "aws-java-sdk-ses" % "1.12.797",
      "org.scalatest" %% "scalatest" % "3.2.19" % Test,
      "org.apache.commons" % "commons-text" % "1.15.0",
      "com.dropbox.core" % "dropbox-core-sdk" % "7.0.0",
      "com.squareup" % "connect" % "2.20191120.0",
      "org.apache.poi" % "poi-ooxml" % "5.5.1",
      "org.apache.poi" % "poi" % "5.5.1",
      "com.typesafe" % "config" % "1.4.3",
      "org.json4s" %% "json4s-native" % "4.1.0-M8",
    ),
    resolvers += Resolver.jcenterRepo,
    resolvers += "lightshed-maven" at "https://dl.bintray.com/content/lightshed/maven",
    scalaVersion := "2.12.21",
	  organization := "biz.mindfulmassage",
    assembly / assemblyMergeStrategy := {
      case PathList("META-INF", "services", _*) => MergeStrategy.concat
      case PathList("META-INF", _*) => MergeStrategy.discard
      case "reference.conf" => MergeStrategy.concat
      case x => MergeStrategy.first
    }
  )
evictionErrorLevel := Level.Warn
