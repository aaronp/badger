import mill._, scalalib._, publish._, scalafmt._, scalajslib._


// http://www.lihaoyi.com/mill/index.html

object const {
  def ScalaEleven = "2.11.11"
  def ScalaTwelve = "2.12.7"
}

object bucky extends ScalaModule with ScalafmtModule with PublishModule {
  def scalaVersion = const.ScalaTwelve

  def publishVersion = "0.0.1"
  def forkArgs = Seq("-Xmx1g")
  
  def ivyDeps = Agg(
    ivy"io.vertx::vertx-lang-scala:3.5.2",
    ivy"io.vertx::vertx-web-scala:3.5.2"
  )

  def pomSettings = PomSettings(
    description = "Bucky",
    organization = "com.github.aaronp",
    url = "https://github.com/aaronp/bucky",
    licenses = Seq(License.MIT),
    versionControl = VersionControl.github("aaronp", "bucky"),
    developers = Seq(
      Developer("aaronp", "Aaron Pritzlaff","https://github.com/aaronp")
    )
  )


  object test extends Tests {
    def ivyDeps = Agg(ivy"org.scalatest::scalatest:3.0.4")
    def testFrameworks = Seq("org.scalatest.tools.Framework")
  }

}