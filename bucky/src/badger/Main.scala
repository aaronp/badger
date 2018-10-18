package badger
import java.net.URL
import java.nio.file.{Files, Paths}

import badger.tls.{DistinguishedName, KeyTool}
import com.typesafe.scalalogging.StrictLogging
import io.vertx.scala.core.Vertx

import scala.util.control.NonFatal

object Main extends StrictLogging {

  def main(a: Array[String]) = {
    val hostPort = HostPort(8080)
    startOn(hostPort)
  }

  def ensureKeystore() = {
    val pathToKeystore = Paths.get("target/keystore.jks")
    val keys = KeyTool(DistinguishedName("com.github.aaronp"), pathToKeystore)
    if (!Files.exists(pathToKeystore)) {
      Files.createDirectories(pathToKeystore.getParent)
      val keyFile = keys.genKey()
      logger.info(s"Generated ${keyFile}")
      val certFile = keys.genCert()
      logger.info(s"Generated ${certFile}")
    } else {
      logger.info(s"Using keystore at ${pathToKeystore}")
    }
    pathToKeystore -> keys.keypass
  }

  def defaultStaticPath: Option[String] = {

    try {
      val indexHtml: URL = getClass.getClassLoader.getResource("web/index.html")
      val fullPath = Paths.get(indexHtml.toURI).getParent.toAbsolutePath.toString
      Option(fullPath)
    } catch {
      case NonFatal(e) =>
        logger.error(s"Couldn't load web/index.html: $e")
        None
    }

  }

  def startOn(hostPort: HostPort) = {
    val staticPath = defaultStaticPath
    val vertx = Vertx.vertx()

    val socketHandler: ServerWebSocketHandler = ServerWebSocketHandler { frame => //
      logger.info("Handling " + frame)
    }

    val (keystore, pw) = ensureKeystore()

    vertx.deployVerticle(new BaseVerticle(socketHandler, keystore, pw, hostPort, staticPath))

    logger.info(s"Running ${hostPort}")
  }

}
