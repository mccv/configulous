import sbt._

class ConfigulousProject(info: ProjectInfo) extends DefaultProject(info) {
  override def filterScalaJars = false
  val scalaTools = "org.scala-lang" % "scala-compiler" % "2.7.7" % "compile"
  
  override def runClasspath = super.runClasspath +++ ("target" / "scala_2.7.7"/ "gen-config")
}
