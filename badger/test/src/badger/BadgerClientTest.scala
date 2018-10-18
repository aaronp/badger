package badger
import java.nio.file.Files

import io.vertx.scala.core.http.HttpClientOptions
import io.vertx.scala.core.net.JksOptions
import org.scalatest.BeforeAndAfterAll
import scalaj.http.{Http, HttpOptions, HttpResponse}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Promise

class BadgerClientTest extends BaseBadgerSpec with BeforeAndAfterAll {

  val hostPort = HostPort(7778).withOrigin(BaseVerticle.host.hostname())
  val (keys, startedFuture) = BadgerMain.run(hostPort)
  var started: BaseVerticle = null
  override def beforeAll(): Unit = {
    started = startedFuture.futureValue
  }
  override def afterAll() = {
    started.vertxInstance.closeFuture().futureValue
    Files.delete(keys.pathToKeystore)
  }

  "BadgerClient" should {
    "be abe to connect an insecure client" in {
      val restHelloUrl = s"https://${BaseVerticle.host.hostname()}:${hostPort.port}/rest/hello"
      val world: HttpResponse[String] = Http(restHelloUrl).option(HttpOptions.allowUnsafeSSL).asString
      world.body shouldBe "world"
    }
    "be abe to connect a secure client" in {
      val restHelloUrl = s"https://${BaseVerticle.host.hostname()}:${hostPort.port}/rest/hello"

      val world = Http(restHelloUrl).option(HttpOptions.sslSocketFactory(keys.socketFactory)).asString
      world.body shouldBe "world"
    }
    "be abe to connect a vertx client" ignore {
      val promise = Promise[String]

      val jkOps = JksOptions().setPath(keys.pathToKeystore.toAbsolutePath.toString).setPassword(keys.keypass)
      val h: String = hostPort.origin.get

//      val pemOps = PemKeyCertOptions().setKeyPath(keys.pathToKeystore.toAbsolutePath.toString)
//        .setPemKeyCertOptions(pemOps)
      val options = HttpClientOptions().setSsl(true).setKeyStoreOptions(jkOps).setTrustAll(true).setTryUseCompression(true)

      started.vertxInstance
        .createHttpClient(options)
        .getNow(hostPort.port, h, "/rest/hello", r => {
        r.exceptionHandler(promise.failure)
        r.bodyHandler(b => promise.success(b.toString))
      })

      promise.future.futureValue shouldBe "world"
    }
  }

}
