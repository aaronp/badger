package badger.tls
import java.io.FileInputStream
import java.nio.file.attribute.{PosixFilePermission, PosixFilePermissions}
import java.nio.file.{Files, Path, Paths}
import java.security.cert.Certificate
import java.security.{KeyStore, SecureRandom}
import java.util
import java.util.Locale

import com.typesafe.scalalogging.LazyLogging
import javax.net.ssl.{SSLContext, SSLSocketFactory, TrustManager, TrustManagerFactory}

import scala.sys.process.ProcessLogger
import scala.util.Properties
import scala.util.control.NonFatal

/**
  * See e.g.
  * https://www.digitalocean.com/community/tutorials/openssl-essentials-working-with-ssl-certificates-private-keys-and-csrs/
  * https://www.sslshopper.com/article-how-to-create-a-self-signed-certificate-using-java-keytool.html
  *
  */
object KeyTool extends LazyLogging {
  def thisCountry(): String = {
    Locale.getDefault().getCountry
  }

  private def exec(cmd: List[String]) = {
    object errLogger extends ProcessLogger {
      private val outBuffer = new StringBuffer()
      private val errBuffer = new StringBuffer()
      override def out(s: => String): Unit = {
        outBuffer.append(s"OUT:$s\n")
      }
      override def err(s: => String): Unit = {
        errBuffer.append(s"ERR:$s\n")
      }
      override def buffer[T](f: => T): T = f
      def stdOut = outBuffer.toString
      def stdErr = errBuffer.toString
      override def toString =
        s"""STDOUT:$stdOut
           |STDERR:$stdErr
         """.stripMargin
    }

    val retVal = try {
      import scala.sys.process._
      val process = cmd.run(errLogger)
      process.exitValue()

    } catch {
      case NonFatal(err) =>
        val msg = cmd.mkString("Error running $>", " ", s": $err")
        logger.error(s"$msg\nstdErr:\n${errLogger.stdErr}")
        throw new Exception(msg, err)
    }
    require(retVal == 0, cmd.mkString("Error running $>", " ", s"; retVal was $retVal: $errLogger"))

    logger.info(cmd.mkString("", " ", " --> " + errLogger))
    errLogger.stdOut
  }

  def defaultCert: Path = Paths.get(s"${Properties.javaHome}/lib/security/cacerts")
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
                   alias: String,
                   initialPathToKeystore: Path = KeyTool.defaultCert,
                   algorithm: String = "RSA",
                   keypass: String = "changeit",
                   storePassword: String = null,
                   keySize: Int = 2048,
                   validity: Int = 360,
                   subjectAlternativeName: Option[String] = None)
    extends LazyLogging {

  lazy val pathToKeystore = {
    if (!Files.exists(initialPathToKeystore) || !Files.isRegularFile(initialPathToKeystore)) {
      if (initialPathToKeystore.getParent != null) {
        val perms: util.Set[PosixFilePermission] = PosixFilePermissions.fromString("rwxr-x---")
        Files.createDirectories(initialPathToKeystore.getParent, PosixFilePermissions.asFileAttribute(perms))
      } else {
        sys.error(s"Can't create ${initialPathToKeystore}")
      }
    }

    logger.info(s"Using ${initialPathToKeystore.toAbsolutePath}")
    initialPathToKeystore
  }
  private lazy val storepass = Option(storePassword).getOrElse(keypass)
  private def genKeyCmd: List[String] = {
    val base = List(
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
      keypass
    )


    val sanOptions: List[String] = subjectAlternativeName.fold(List[String]()) { name =>
      // -ext san=dns:www.example.com
      val san = if (name.toLowerCase.startsWith("dns:")) {
        name
      } else {
        s"san=dns:$name"
      }
      List("-ext", san)
    }

    val dnameList = {
      List("-dname",
      s"${dname.toString}")
    }

    base ::: sanOptions ::: dnameList
  }

  def socketFactory: SSLSocketFactory = {
    val trustCerts: Array[TrustManager] = {
      val pkix = TrustManagerFactory.getDefaultAlgorithm
      val factory = TrustManagerFactory.getInstance(pkix)
      factory.init(keystore)
      factory.getTrustManagers
    }

    val sc = SSLContext.getInstance("TLSv1")
    sc.init(null, trustCerts, new SecureRandom)
    sc.getSocketFactory
  }

  private case class LoadedStore(inst: KeyStore = KeyStore.getInstance(KeyStore.getDefaultType())) {
    private val is = new FileInputStream(pathToKeystore.toAbsolutePath.toString)
    try {
      inst.load(is, keypass.toCharArray)
    } finally {
      is.close()
    }
  }

  def keystore = store.inst
  private lazy val store = LoadedStore()

  private def withStore[T](default: => T)(fn: KeyStore => T): T = {
    if (Files.exists(pathToKeystore)) {
      fn(store.inst)
    } else {
      default
    }
  }
  def aliases: List[String] = {
    import scala.collection.JavaConverters._
    withStore(List[String]())(_.aliases().asScala.toList)
  }
  def delete(name: String) = {
    withStore(())(_.deleteEntry(name: String))
  }

  def certificateFor(name: String): Option[Certificate] = {
    withStore(Option.empty[Certificate]) { ks => Option(ks.getCertificate(name))
    }
  }

  def genKeyOrGet(): Path = {
    val ok = Files.exists(pathToKeystore)
    if (ok) {
      pathToKeystore
    } else {
      genKey()
    }
  }

  def genKey(): Path = {
    KeyTool.exec(genKeyCmd)
    pathToKeystore
  }

  def export(pathToCertificate: Path): Path = {
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

    KeyTool.exec(cmd)
    pathToCertificate
  }
}
