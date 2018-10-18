package badger.tls

import java.nio.file.{Files, Paths}
import java.util.UUID

import org.scalatest.{Matchers, WordSpec}

import scala.util.Try

class KeyToolTest extends WordSpec with Matchers {
  "KeyTool" should {
    "generate jks and certificates via genKey and getCert" in {
      val unique = UUID.randomUUID().toString
      val keyFile = Paths.get(s"keystore-$unique.jks")
      val certFile = Paths.get(s"someCert-$unique.cer")

      Files.exists(keyFile) shouldBe false
      Files.exists(certFile) shouldBe false
      try {
        val gen = KeyTool(DistinguishedName("com.github.aaronp"), keyFile)
        gen.genKey() shouldBe keyFile
        gen.genCert(certFile) shouldBe certFile

        Files.exists(keyFile) shouldBe true
        Files.exists(certFile) shouldBe true
      } finally {
        Try(Files.delete(keyFile))
        Try(Files.delete(certFile))
      }
    }
  }

}
