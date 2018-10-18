package badger
import badger.tls.KeyTool
import com.typesafe.scalalogging.StrictLogging
import io.vertx.scala.ext.web.Route

import scala.concurrent.{ExecutionContext, Future}

object BadgerMain extends StrictLogging {

  def main(a: Array[String]) : Unit = {
    import ExecutionContext.Implicits.global
    run()
  }

  def run(hostPort: HostPort = HostPort(8080).withOrigin(BaseVerticle.host.hostname()))(
      implicit ec: ExecutionContext): (KeyTool, Future[BaseVerticle]) = {
    BaseVerticle.startOnEnsuringKeystore(hostPort)(handle)
  }

  def handle(vert: BaseVerticle): Route = {
    val router = vert.router

    BaseVerticle.static.defaultHandlerForWeb.foreach { staticHandler => //
      router.route("/*").handler(staticHandler)
    }

    router.get("/rest/hello").handler(_.response().end("world"))
    router.delete("/rest/shutdown").handler { req =>
      req.response().end("bye!")
      vert.close()
    }
  }

}
