enablePlugins(AutoMkcolPlugin)

name := """pincette-common"""
organization := "net.pincette"
version := "1.2.1"

scalaVersion := "2.12.4"

libraryDependencies ++= Seq(
  "javax.json" % "javax.json-api" % "1.1.2"
)

publishTo := Some("Pincette" at "https://re.pincette.net/repo")
credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")
isSnapshot := true // Allow overwrites.
publishMavenStyle := true
crossPaths := false
