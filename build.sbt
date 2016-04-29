
lazy val commonSettings = Seq (
  organization := "jp.co.sample",
  version := "0.1",
  scalaVersion := "2.11.8"
)

lazy val root =
  (project in file(".")).
    settings(commonSettings: _*).
    settings(
      name := "akka-http",
      scalacOptions += "-deprecation",
      libraryDependencies ++= Seq (
//        "com.typesafe.akka" %% "akka-http-core" % "2.4.3",
        "com.typesafe.akka" %% "akka-stream" % "2.4.3",
        "com.typesafe.akka" %% "akka-http-experimental" % "2.4.3"
      )
    )