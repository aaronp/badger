import mill._, scalalib._, publish._, scalafmt._, scalajslib._


// http://www.lihaoyi.com/mill/index.html

object const {
  def ScalaEleven = "2.11.11"
  def ScalaTwelve = "2.12.7"
  def ScalaVersion = ScalaTwelve
}

object badger extends ScalaModule with ScalafmtModule with PublishModule {
  def scalaVersion = const.ScalaVersion

  //def forkArgs = Seq("-Xmx1g")
  override def scalacOptions = Seq("-deprecation", "-feature")
  
  def mainClass = Some("badger.Main")


//  val monix = List("monix", "monix-execution",  "monix-eval", "monix-reactive", "monix-tail").map { art =>
//    ivy"io.monix::$art:3.0.0-RC1"
//  }
  val monix = Nil

  def ivyDeps = Agg(
    ivy"io.vertx::vertx-lang-scala:3.5.2",
    ivy"io.vertx::vertx-web-scala:3.5.2",
    ivy"org.reactivestreams:reactive-streams:1.0.2",
    ivy"com.typesafe.scala-logging::scala-logging:3.7.2",
    ivy"ch.qos.logback:logback-classic:1.1.11"
  ) ++ monix

  def publishVersion = "0.0.1"
  def pomSettings = PomSettings(
    description = "Badger",
    organization = "com.github.aaronp",
    url = "https://github.com/aaronp/badger",
    licenses = Seq(License.MIT),
    versionControl = VersionControl.github("aaronp", "badger"),
    developers = Seq(
      Developer("aaronp", "Aaron Pritzlaff","https://github.com/aaronp")
    )
  )


  object test extends Tests {
    def ivyDeps = Agg(ivy"org.scalatest::scalatest:3.0.4")
    def testFrameworks = Seq("org.scalatest.tools.Framework")
  }

}