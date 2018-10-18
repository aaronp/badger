package badger

import java.net.{InetAddress, URL}
import java.nio.file.{Files, Path, Paths}

import badger.sockets.{ServerWebSocketHandler, WebFrame}
import badger.tls.{DistinguishedName, KeyTool}
import com.typesafe.scalalogging.{LazyLogging, StrictLogging}
import io.vertx.core.Handler
import io.vertx.lang.scala.ScalaVerticle
import io.vertx.scala.core.Vertx
import io.vertx.scala.core.http.{Http2Settings, HttpServer, HttpServerOptions, ServerWebSocket}
import io.vertx.scala.core.net.JksOptions
import io.vertx.scala.ext.web.Router
import io.vertx.scala.ext.web.handler.StaticHandler
import org.reactivestreams.{Publisher, Subscriber, Subscription}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal
import scala.util.{Properties, Try}

/**
  * Just an extension of the example scala verticle w/ some settings
  *
  * @param socketHandler
  * @param staticPath
  * @param hostPort
  */
class BaseVerticle(socketHandler: Handler[ServerWebSocket], options: HttpServerOptions, hostPort: HostPort)(
    init: BaseVerticle => Unit)
    extends ScalaVerticle
    with StrictLogging
    with AutoCloseable {

  override def close() = {
    vertxInstance.close()
  }

  def vertxInstance = vertx

  def router = routerInst

  private var routerInst: Router = null
  override def startFuture(): Future[Unit] = {

    require(routerInst == null)
    routerInst = Router.router(vertx)
    init(this)

    val fut: Future[HttpServer] = vertx
      .createHttpServer(options)
      .requestHandler(routerInst.accept)
      .websocketHandler(socketHandler)
      .listenFuture(hostPort.port, hostPort.host)

    fut.map { server =>
      logger.info(s"Server Listening on ${server.actualPort}")
      ()
    }
  }
}

object BaseVerticle extends LazyLogging {

  /**
    *
    * @param hostPort
    * @param fn
    * @param execCtxt
    * @return
    */
  def startOnEnsuringKeystore(hostPort: HostPort)(fn: BaseVerticle => Unit)(
      implicit execCtxt: ExecutionContext) = {
    val keys = KeyTool(
      DistinguishedName("BaseVerticle"),
      "BaseVerticle",
      Paths.get(Properties.userDir).resolve(".cacerts/certs.jks"),
      subjectAlternativeName = Option(host.hostname())
    )
    val keystorePath = keys.genKeyOrGet()

    val options = serverOptions(hostPort.origin, keystorePath, keys.keypass)

    keys -> startOn(hostPort, options)(fn)
  }

  def startOn(hostPort: HostPort, options: HttpServerOptions)(fn: BaseVerticle => Unit)(
      implicit execCtxt: ExecutionContext): Future[BaseVerticle] = {
    val vertx = Vertx.vertx()

    val socketHandler: ServerWebSocketHandler = ServerWebSocketHandler {
      case (name, publisher) =>
        new Publisher[WebFrame] {
          override def subscribe(wrapped: Subscriber[_ >: WebFrame]): Unit = {
            publisher.subscribe(new Subscriber[WebFrame] {
              override def onSubscribe(s: Subscription): Unit = wrapped.onSubscribe(s)
              override def onNext(t: WebFrame): Unit = {
                val txt = t.asText.getOrElse("binary frame")
                logger.info(s"$name :: $txt")
                wrapped.onNext(WebFrame.text("Echo:: " + txt))
              }
              override def onError(t: Throwable): Unit = wrapped.onError(t)
              override def onComplete(): Unit = wrapped.onComplete()
            })
          }
        }
    }

    val verticle: BaseVerticle = new BaseVerticle(socketHandler, options, hostPort)(fn)
    vertx.deployVerticleFuture(verticle).map { running =>
      logger.info(s"Running ${hostPort}")
      verticle

    }
  }

  def serverOptions(origin: Option[String], keystorePath: Path, keystorePassword: String): HttpServerOptions = {
    val base = HttpServerOptions()
      .setSsl(true)
      .setCompressionSupported(true)
      .setKeyStoreOptions(
        JksOptions().setPath(keystorePath.toAbsolutePath.toString).setPassword(keystorePassword)
      )

    base.setInitialSettings(Http2Settings().setPushEnabled(true))
    //base.setUseAlpn(true)
    //base.addEnabledCipherSuite("TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256")

    origin.fold(base)(base.setHost)
    base
  }

  object static {

    def handler(path: String) = {
      val staticHandler = StaticHandler.create().setCachingEnabled(false)

      logger.info(s"serving static paths under $path")

      staticHandler.setAllowRootFileSystemAccess(true).setDirectoryListing(true).setWebRoot(path)
      staticHandler
    }

    def defaultHandlerForWeb: Option[StaticHandler] = defaultStaticPath.map(handler)

    def defaultStaticPath: Option[String] = {

      try {
        val indexHtml: URL = getClass.getClassLoader.getResource("web/index.html")
        val fullPath = Paths.get(indexHtml.toURI).getParent

        val absPath = if (fullPath.getParent.getFileName.toString == "classes") {
          def parentOf(d: Path): Stream[Path] = {
            if (d == null) {
              Stream.empty[Path]
            } else {
              d #:: parentOf(d.getParent)
            }
          }
          val rootStream = parentOf(fullPath).dropWhile { d => //
            val root = d.resolve("badger/resources/web")
            !Files.isDirectory(root)
          }
          rootStream.headOption match {
            case Some(root) =>
              val dir = root.resolve("badger/resources/web")
              logger.info(s"Project root is $root, using resource dir ${dir}")
              dir.toAbsolutePath.toString
            case None =>
              logger.info("Using default resources/web")
              fullPath.toAbsolutePath.toString
          }
        } else {
          fullPath.toAbsolutePath.toString
        }

        Option(absPath)
      } catch {
        case NonFatal(e) =>
          logger.error(s"Couldn't load web/index.html: $e")
          None
      }

    }
  }

  object host {
    def hostname(): String = hostnameFromSys().getOrElse(inetHostName)

    def hostnameFromSys() = Try(invokeHostname)

    lazy val inetHostName = {
      InetAddress.getLocalHost.getHostName
    }

    private def invokeHostname = {
      val output: String = {
        import sys.process._
        "hostname".!!
      }

      val first: Iterator[String] = output.lines
      first.next
    }
  }

}
