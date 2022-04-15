package org.thehellnet.service

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.util.{Failure, Success}

class CryptoServiceTest extends AnyWordSpec with Matchers {

  "decrypt" should {
    "decrypt correctly message" in {
      val secret = "43g5szJ8"
      val salt   = "2GHh5T3v"

      val message = Array[Byte](-76, -76, -11, 87, -114, -93, -114, -40, 114, -15, -40, 127, 113, -120, 91, -45, -45, -40, -114, 5, -66, 41, -48, 85,
        -125, -69, -64, 82, 39, 11, 8, 74, -71, 80, 74, -97, -108, 110, 114, -21, -86, 20, -37, 118, 28, -120, -44, 99, -66, 21, 21, -37, 103, -110,
        -78, -1, -114, 110, 22, 8, 98, -13, -83, -54)

      val cryptoUtils = new CryptoService(secret, salt)
      cryptoUtils.decrypt(message) shouldBe Success("test|51ba91bf-6b44-470f-9639-afb6687e34a5|1650029970")
    }

    "return failure for a bad message" in {
      val secret = "43g5szJ8"
      val salt   = "2GHh5T3v"

      val message = Array[Byte](-86, -76, -11, 87, -114, -93, -114, -40, 114, -15, -40, 127, 113, -120, 91, -45, -45, -40, -114, 5, -66, 41, -48, 85,
        -125, -69, -64, 82, 39, 11, 8, 74, -71, 80, 74, -97, -108, 110, 114, -21, -86, 20, -37, 118, 28, -120, -44, 99, -66, 21, 21, -37, 103, -110,
        -78, -1, -14, 110, 22, 8, 98, -13, -83, -54)

      val cryptoUtils = new CryptoService(secret, salt)
      cryptoUtils.decrypt(message) shouldBe an[Failure[Throwable]]
    }
  }
}
