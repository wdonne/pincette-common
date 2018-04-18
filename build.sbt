name := """pincette-common"""
organization := "net.pincette"
version := "1.2.4"

scalaVersion := "2.12.4"

libraryDependencies ++= Seq(
  "javax.json" % "javax.json-api" % "1.1.2"
)

publishTo := Some(Resolver.file("file",  new File(Path.userHome.absolutePath + "/.m2/repository")))
crossPaths := false
