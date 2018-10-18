package badger.tls
import java.net.InetAddress

case class Nic(displayName: String, name: String, addresses: List[InetAddress]) {
  def addressNames = {
    addresses.map(_.getHostName)
  }
}

object Nic {

  def list() = {
    import scala.collection.JavaConverters._
    java.net.NetworkInterface.getNetworkInterfaces.asScala.filterNot(_ == null).map { ni =>
      val addresses = ni.getInterfaceAddresses.asScala.filterNot(_ == null).map(_.getAddress)
      Nic(ni.getDisplayName, ni.getName, addresses.toList)
    }
  }
}
