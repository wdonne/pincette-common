import com.github.tototoshi.sbt.automkcol.Plugin._

name := """pincette-common"""
organization := "net.pincette"
version := "1.0.3"

scalaVersion := "2.11.11"

libraryDependencies ++= Seq(
  "javax.json" % "javax.json-api" % "1.0"
)

publishTo := Some("Pincette" at "https://re.pincette.net/repo")
credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")

AutoMkcol.globalSettings

crossPaths := false

fork in run := true
