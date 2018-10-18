package badger.tls
import java.nio.file.Path
import java.util.Locale

import com.typesafe.scalalogging.LazyLogging

/**
  * See e.g.
  * https://www.digitalocean.com/community/tutorials/openssl-essentials-working-with-ssl-certificates-private-keys-and-csrs/
  * https://www.sslshopper.com/article-how-to-create-a-self-signed-certificate-using-java-keytool.html
  *
  */
object KeyTool {
  def thisCountry(): String = {
    Locale.getDefault().getCountry
  }
}

/**
  * Provides an easy means to knock out keys and self-signed certificates
  *
  * @param dname
  * @param pathToKeystore
  * @param alias
  * @param algorithm
  * @param keypass
  * @param storepass
  * @param keySize
  * @param validity
  */
case class KeyTool(dname: DistinguishedName,
                   pathToKeystore: Path,
                   alias: String = "server-alias",
                   algorithm: String = "RSA",
                   keypass: String = "changeit",
                   storePassword: String = null,
                   keySize: Int = 2048,
                   validity: Int = 360)
    extends LazyLogging {
  private lazy val storepass = Option(storePassword).getOrElse(keypass)
  def genKeyCmd: List[String] = {
    List(
      "keytool",
      "-genkey",
      "-keyalg",
      algorithm,
      "-alias",
      alias,
      "-keystore",
      pathToKeystore.toAbsolutePath.toString,
      "-storepass",
      storepass,
      "-validity",
      validity.toString,
      "-keysize",
      keySize.toString,
      "-keypass",
      keypass,
      "-dname",
      dname.toString
    )
  }

  private def defaultCert: Path = {
    val ExtensionR = """(.*)\..+""".r
    val fileName = pathToKeystore.getFileName.toString match {
      case ExtensionR(fileName) => fileName + ".cer"
      case name => name + ".cer"
    }
    pathToKeystore.getParent.resolve(fileName)
  }

  def genCert(pathToCertificate: Path = defaultCert): Path = {
    val cmd = List(
      "keytool",
      "-export",
      "-alias",
      alias,
      "-keystore",
      pathToKeystore.toAbsolutePath.toString,
      "-rfc",
      "-file",
      pathToCertificate.toAbsolutePath.toString,
      "-storepass",
      storepass
    )

    import scala.sys.process._
    logger.info(cmd.lineStream.mkString("\n"))
    pathToCertificate
  }

  def genKey(): Path = {
    import scala.sys.process._
    logger.info(genKeyCmd.lineStream.mkString("\n"))
    pathToKeystore
  }
}
