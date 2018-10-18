package badger
import io.vertx.scala.core.http.WebSocketFrame

object WebSocketFrameAsWebFrame {

  def apply(vertxFrame: WebSocketFrame): WebFrame = {
    if (vertxFrame.isText()) {
      if (vertxFrame.isFinal()) {
        WebFrame.finalText(vertxFrame.textData())
      } else {
        WebFrame.text(vertxFrame.textData())
      }
    } else {
      if (vertxFrame.isFinal()) {
        WebFrame.finalBinary(vertxFrame.binaryData().getBytes)
      } else {
        WebFrame.binary(vertxFrame.binaryData().getBytes)
      }
    }
  }
}
