/*
 * Copyright (C) 2016-2017 Lightbend Inc. <http://www.lightbend.com>
 */
package akka.stream.alpakka.s3.auth

import java.time.LocalDate

import org.scalatest.{FlatSpec, Matchers}

class SigningKeySpec extends FlatSpec with Matchers {
  behavior of "A Signing Key"

  val credentials = AWSCredentials("AKIDEXAMPLE", "wJalrXUtnFEMI/K7MDENG+bPxRfiCYEXAMPLEKEY")

  val scope = CredentialScope(LocalDate.of(2015, 8, 30), "us-east-1", "iam")
  val signingKey = SigningKey(credentials, scope)

  it should "produce a signing key" in {
    val expected: Array[Byte] = Array(196, 175, 177, 204, 87, 113, 216, 113, 118, 58, 57, 62, 68, 183, 3, 87, 27, 85,
      204, 40, 66, 77, 26, 94, 134, 218, 110, 211, 193, 84, 164, 185).map(_.toByte)

    signingKey.key.getEncoded should equal(expected)
  }

  it should "sign a message" in {
    val sts =
      "AWS4-HMAC-SHA256\n20150830T123600Z\n20150830/us-east-1/iam/aws4_request\nf536975d06c0309214f805bb90ccff089219ecd68b2577efef23edd43b7e1a59"
    signingKey.hexEncodedSignature(sts.getBytes) should equal(
      "5d672d79c15b13162d9279b0855cfba6789a8edb4c82c400e06b5924a6f2b5d7"
    )
  }
}
