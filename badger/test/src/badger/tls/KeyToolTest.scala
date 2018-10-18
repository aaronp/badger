package badger.tls

import java.nio.file.{Files, Paths}
import java.util.UUID

import badger.BaseBadgerSpec

import scala.util.{Properties, Try}

class KeyToolTest extends BaseBadgerSpec {

  "KeyTool" should {
    "generate jks and certificates via genKey and getCert" in {
      val unique = UUID.randomUUID().toString
      val keyFile = Paths.get(s"${Properties.userDir}/out/test/keystore-$unique.jks")
      val certFile = Paths.get(s"${Properties.userDir}/out/test/someCert-$unique.cer")


      // the keytool saves all aliases as lower case
      val testAlias = s"KeyToolTest-$unique".toLowerCase

      Files.exists(keyFile) shouldBe false
      Files.exists(certFile) shouldBe false

      try {
        val gen = KeyTool(DistinguishedName("com.github.aaronp"), testAlias, keyFile)

        val initialAliases = gen.aliases
        initialAliases.foreach(println)

        gen.genKey() shouldBe keyFile

        Files.exists(keyFile) shouldBe true

        Files.exists(certFile) shouldBe false
        gen.export(certFile)
        Files.exists(certFile) shouldBe true

        gen.aliases should contain(testAlias)
        gen.delete(testAlias)
        gen.aliases should not contain (testAlias)
      } finally {
        Try(Files.delete(keyFile))
        Try(Files.delete(certFile))
      }
    }
  }

}
