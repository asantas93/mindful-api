lazy val `mindful-api` = (project in file("."))
  .settings(
	  libraryDependencies ++= Seq(
      "com.github.dnvriend" %% "sam-annotations" % "1.0.30",
      "com.github.dnvriend" %% "sam-lambda" % "1.0.30",
      "com.amazonaws" % "aws-lambda-java-core" % "1.2.0",
      "com.amazonaws" % "aws-java-sdk-ses" % "1.11.656",
      "org.scalatest" %% "scalatest" % "3.0.4" % Test,
      "org.json4s" %% "json4s-jackson" % "3.5.0",
      "org.apache.commons" % "commons-text" % "1.1",
      "com.dropbox.core" % "dropbox-core-sdk" % "3.0.6",
      "com.squareup" % "connect" % "2.5.3",
      "org.apache.poi" % "poi-ooxml" % "3.17",
      "org.apache.poi" % "poi" % "3.17",
    ),
    resolvers += Resolver.bintrayRepo("dnvriend", "maven"),
    resolvers += Resolver.jcenterRepo,
    resolvers += Resolver.sonatypeRepo("snapshots"),
    resolvers += "lightshed-maven" at "http://dl.bintray.com/content/lightshed/maven",
    scalaVersion := "2.12.4",
	  samStage := "prod",
	  organization := "biz.mindfulmassage",
  )
