package badger.tls
import badger.tls.KeyTool.thisCountry

import scala.util.Properties

case class DistinguishedName(organisationalUnit: String,
                             organisationName: String = null,
                             country: String = thisCountry,
                             commonName: String = Properties.userName) {
  override def toString: String = {
    val organisation = Option(organisationName).getOrElse(organisationalUnit)

    s"cn=${commonName}, ou=$organisationalUnit, o=${organisation}, c=${country}"
  }
}