package badger.sockets
import com.typesafe.scalalogging.StrictLogging
import io.vertx.core.Handler
import io.vertx.scala.core.http.ServerWebSocket
import org.reactivestreams.Publisher

/**
  * A handler whose 'onConnect' callback takes a publisher of events from the remote socket and returns a publisher of events TO the remote socket
  *
  * @param onConnect
  */
case class ServerWebSocketHandler(onConnect: (String, Publisher[WebFrame]) => Publisher[WebFrame])
    extends Handler[ServerWebSocket]
    with StrictLogging {
  override def handle(socket: ServerWebSocket): Unit = {
    val path = socket.uri()

    val addr: String = {
      val a = socket.remoteAddress()
      val url = s"${a.host}:${a.port}/${a.path}"
      s"$path (socket connected to $url)"
    }
    logger.info(s"$addr Accepting connection")


    val fromRemote: Publisher[WebFrame] = FromRemotePublisher(addr, socket)
    val toRemote = onConnect(addr, fromRemote)
    toRemote.subscribe(ToRemoteSubscriber(addr, socket))
  }

}
