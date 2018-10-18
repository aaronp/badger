package badger
import org.scalatest.Matchers
import org.scalatest.concurrent.{Eventually, ScalaFutures}

import scala.concurrent.Promise

class BaseVerticleTest extends VerticleTesting[BaseVerticle] with Matchers with Eventually with ScalaFutures {

  "HttpVerticle" should "bind to 8666 and answer with 'world'" in {
    val promise = Promise[String]

    vertx
      .createHttpClient()
      .getNow(8666, "127.0.0.1", "/hello", r => {
        r.exceptionHandler(promise.failure)
        r.bodyHandler(b => promise.success(b.toString))
      })

    promise.future.futureValue shouldBe "world"
  }

}
