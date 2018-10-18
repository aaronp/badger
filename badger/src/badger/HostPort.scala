package badger

case class HostPort(port : Int, host : String = "0.0.0.0", hostNameOrigin : String = null) {
  def origin: Option[String] = Option(hostNameOrigin)
  def withOrigin(name : String) = copy(hostNameOrigin = name)
}
