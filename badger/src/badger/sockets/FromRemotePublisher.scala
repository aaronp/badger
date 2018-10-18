package badger.sockets
import com.typesafe.scalalogging.{LazyLogging, StrictLogging}
import io.vertx.core.Handler
import io.vertx.scala.core.http.{ServerWebSocket, WebSocketFrame}
import org.reactivestreams.{Publisher, Subscriber, Subscription}

object FromRemotePublisher extends StrictLogging {
  def apply(name: String, socket: ServerWebSocket): FromRemotePublisher = {
    new FromRemotePublisher(name, socket)
  }
}

class FromRemotePublisher private (name: String, socket: ServerWebSocket) extends Publisher[WebFrame] with LazyLogging {
  override def subscribe(wrapped: Subscriber[_ >: WebFrame]): Unit = {

    wrapped.onSubscribe(new Subscription {
      override def request(n: Long): Unit = {
        // TODO - plug in real publisher here
      }
      override def cancel(): Unit = {
        socket.close(1000, Option("subscription cancelled"))
      }
    })
    socket.frameHandler(new Handler[WebSocketFrame] {
      override def handle(event: WebSocketFrame): Unit = {
        if (event.isClose()) {
          logger.debug(s"$name handling close frame")
          wrapped.onComplete()
        } else {
          val frame = WebFrame(event)
          logger.debug(s"$name handling frame ${frame}")

          // TODO - we should apply back-pressure, but also not block the event loop.
          // need to apply some thought here if this can work in the general case,
          // of if this should be made more explicit
          //Await.result(fut, timeout)
          wrapped.onNext(frame)
        }
      }
    })

    socket.exceptionHandler(new Handler[Throwable] {
      override def handle(event: Throwable): Unit = {
        logger.warn(s"$name got exception $event")
        wrapped.onError(event)
        socket.close()
      }
    })
    socket.endHandler(new Handler[Unit] {
      override def handle(event: Unit): Unit = {
        logger.debug(s"$name ending")
        wrapped.onComplete()
      }
    })

    socket.accept()

  }
}
