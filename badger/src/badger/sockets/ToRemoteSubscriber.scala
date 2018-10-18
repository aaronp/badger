package badger.sockets
import java.util.concurrent.atomic.AtomicBoolean

import com.typesafe.scalalogging.StrictLogging
import io.vertx.core.Handler
import io.vertx.core.buffer.Buffer
import io.vertx.scala.core.http.{ServerWebSocket, WebSocketFrame}
import org.reactivestreams.{Subscriber, Subscription}

import scala.util.control.NonFatal

object ToRemoteSubscriber {
  def apply(name: String, socket: ServerWebSocket): ToRemoteSubscriber = {
    new ToRemoteSubscriber(name, socket)
  }
}

class ToRemoteSubscriber private (name: String, socket: ServerWebSocket)
    extends Subscriber[WebFrame]
    with StrictLogging {
  private var subscription: Subscription = null
  private val completed = new AtomicBoolean(false)

  override def onSubscribe(s: Subscription): Unit = {
    require(subscription == null)
    require(s != null)
    subscription = s
    subscription.request(Long.MaxValue)

    socket.frameHandler(new Handler[WebSocketFrame] {
      override def handle(event: WebSocketFrame): Unit = {
        if (event.isClose()) {
          logger.debug(s"$name handling close frame")
          //markComplete()
        } else {
          val frame = WebFrame(event)
          logger.debug(s"$name handling frame ${frame}")
          onNext(frame)

          // TODO - we should apply back-pressure, but also not block the event loop.
          // need to apply some thought here if this can work in the general case,
          // of if this should be made more explicit
          //Await.result(fut, timeout)
        }
      }
    })

    socket.exceptionHandler(new Handler[Throwable] {
      override def handle(event: Throwable): Unit = {
        logger.warn(s"$name got exception $event")
        socket.close()
        onError(event)
      }
    })
    socket.endHandler(new Handler[Unit] {
      override def handle(event: Unit): Unit = {
        logger.debug(s"$name ending")
        onComplete()
      }
    })

    socket.accept()
  }
  override def onNext(elem: WebFrame) = {
    elem match {
      case TextFrame(text) =>
        logger.debug(s"$name writing  to socket: $text")
        socket.writeTextMessage(text)
      case BinaryFrame(data) =>
        logger.debug(s"$name writing bytes to socket")
        val buff: Buffer = io.vertx.core.buffer.Buffer.buffer(data.array)
        socket.writeBinaryMessage(buff)
      case FinalTextFrame(text) =>
        logger.debug(s"$name writing final text to socket: $text")
        socket.writeFinalTextFrame(text)
      case FinalBinaryFrame(data) =>
        logger.debug(s"$name writing final binary frame")
        val buff: Buffer = io.vertx.core.buffer.Buffer.buffer(data.array)
        socket.writeFinalBinaryFrame(buff)
      case CloseFrame(statusCode, reason) =>
        logger.debug(s"$name writing close frame to socket w/ status $statusCode, reason $reason")
        socket.close(statusCode, reason)
    }
    subscription.request(1)
  }

  override def onError(ex: Throwable): Unit = {
    val ok = completed.compareAndSet(false, true)

    logger.debug(s"\n\t!!!! $name onError trying to close the socket will  ${if (ok) "succeed" else "fail"}")

    try {
      if (ok) {
        logger.debug(s"$name onError($ex) closing the socket")
        socket.close(500, Option(s"Error: $ex"))
      } else {
        logger.warn(s"onError($ex) has not effect on the closed socket")
      }
    } catch {
      case NonFatal(e) =>
        logger.error(s"Error ending socket connected to ${socket.remoteAddress()} after error $ex", e)
    }
  }

  override def onComplete(): Unit = {

    val ok = completed.compareAndSet(false, true)
    logger.debug(s"\n\t!!!! $name onComplete trying to close the socket will  ${if (ok) "succeed" else "fail"}")

    try {
      if (ok) {
        socket.end()
      } else {
        logger.warn(s"$name onComplete has no effect as the socket it already closed")
      }
    } catch {
      case NonFatal(e) =>
        logger.error(s"$name Error ending socket connected to ${socket.remoteAddress()}", e)
    }
  }

}
