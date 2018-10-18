package badger
import com.typesafe.scalalogging.StrictLogging
import io.vertx.core.Handler
import io.vertx.scala.core.http.{ServerWebSocket, WebSocketBase, WebSocketFrame}

case class ServerWebSocketHandler(onConnect: WebSocketObserver => Unit) extends Handler[ServerWebSocket] with StrictLogging {
  override def handle(socket: ServerWebSocket): Unit = {
    val path = socket.uri()

    val addr = {
      val a = socket.remoteAddress()
      val url = s"${a.host}:${a.port}/${a.path}"
      s"$path (socket connected to $url)"
    }
    logger.info(s"$addr Accepting connection")

    val sub = WebSocketObserver(addr, socket)
    onConnect(sub)
  }

}
