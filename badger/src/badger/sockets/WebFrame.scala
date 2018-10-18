package badger.sockets

import java.nio.ByteBuffer

import io.vertx.scala.core.http.WebSocketFrame

sealed trait WebFrame {
  def asText: Option[String]
  def asBinary: Option[ByteBuffer]
}

object WebFrame {
  def text(text: String) = TextFrame(text)

  def finalText(text: String) = FinalTextFrame(text)

  def finalBinary(data: Array[Byte]): FinalBinaryFrame = finalBinary(ByteBuffer.wrap(data))

  def finalBinary(data: ByteBuffer): FinalBinaryFrame = FinalBinaryFrame(data)

  def binary(data: Array[Byte]): BinaryFrame = binary(ByteBuffer.wrap(data))

  def binary(data: ByteBuffer): BinaryFrame = BinaryFrame(data)

  def close(statusCode: Short, reason: Option[String] = None) = CloseFrame(statusCode, reason)



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

final case class TextFrame(text: String) extends WebFrame {
  override def asText   = Option(text)
  override def asBinary = None
}

final case class BinaryFrame(data: ByteBuffer) extends WebFrame {
  override def asText   = None
  override def asBinary = Option(data)
}

final case class FinalTextFrame(text: String) extends WebFrame {
  override def asText   = Option(text)
  override def asBinary = None
}

final case class FinalBinaryFrame(data: ByteBuffer) extends WebFrame {
  override def asText   = None
  override def asBinary = Option(data)
}

final case class CloseFrame(statusCode: Short, reason: Option[String]) extends WebFrame {
  override def asText   = None
  override def asBinary = None
}
