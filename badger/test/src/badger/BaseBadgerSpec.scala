package badger
import org.scalatest.{Matchers, WordSpec}
import org.scalatest.concurrent.ScalaFutures
import scala.concurrent.duration._
import org.scalatest.time.{Millis, Seconds, Span}

import scala.concurrent.duration.FiniteDuration

class BaseBadgerSpec extends WordSpec with Matchers with ScalaFutures {

  /**
    * All the timeouts!
    */
  implicit def testTimeout: FiniteDuration = 2.seconds

  /**
    * @return the timeout for something NOT to happen
    */
  def testNegativeTimeout: FiniteDuration = 300.millis

  implicit override def patienceConfig =
    PatienceConfig(timeout = scaled(Span(testTimeout.toSeconds, Seconds)), interval = scaled(Span(150, Millis)))


}
