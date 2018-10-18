package badger

import java.nio.file.Path

import com.typesafe.scalalogging.StrictLogging
import io.vertx.core.Handler
import io.vertx.lang.scala.ScalaVerticle
import io.vertx.scala.core.http.{HttpServer, HttpServerOptions, ServerWebSocket}
import io.vertx.scala.core.net.JksOptions
import io.vertx.scala.ext.web.Router
import io.vertx.scala.ext.web.handler.StaticHandler

import scala.concurrent.Future

/**
  * Just an extension of the example scala verticle w/ some settings
  *
  * @param socketHandler
  * @param staticPath
  * @param hostPort
  */
class BaseVerticle(socketHandler: Handler[ServerWebSocket],
                   keystorePath: Path,
                   keystorePassword: String,
                   hostPort: HostPort,
                   staticPath: Option[String] = None)
    extends ScalaVerticle
    with StrictLogging {

  override def startFuture(): Future[Unit] = {

    val router = Router.router(vertx)

    router.get("/rest/hello").handler(_.response().end("world"))
    staticPath.foreach { path =>
      val staticHandler = StaticHandler.create().setCachingEnabled(false)

      staticHandler.setAllowRootFileSystemAccess(true).setDirectoryListing(true).setWebRoot(path)
      router.route("/*").handler(staticHandler)
    }

    val options = HttpServerOptions()
      .setSsl(true)
      .setKeyStoreOptions(
        JksOptions().setPath(keystorePath.toAbsolutePath.toString).setPassword(keystorePassword)
      )

    val fut: Future[HttpServer] = vertx
      .createHttpServer(options)
      .requestHandler(router.accept)
      .websocketHandler(socketHandler)
      .listenFuture(hostPort.port, hostPort.host)

    fut.map { server =>
      val staticMessage = staticPath.fold("") { path => s", serving static paths under $path"
      }
      logger.info(s"Server Listening on ${server.actualPort}$staticMessage")
      ()
    }
  }
}
