/*
 * Copyright 2018 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package views.notifications

import java.math.BigInteger
import java.security.MessageDigest

import org.scalatest.MustMatchers
import utils.{AmlsSpec, AuthorisedFixture}

import scala.io.Source

class NotificationsCheckSumSpec extends AmlsSpec with MustMatchers {

    trait TemplateRouteFixture extends AuthorisedFixture {
        val templateRoute: String = "./app/views/notifications/"
        val templateSuffix: String = ".scala.html"
        val templateNames: Seq[String] = Seq(
            "message_details",
            "minded_to_reject",
            "minded_to_revoke",
            "no_longer_minded_to_reject",
            "no_longer_minded_to_revoke",
            "rejection_reasons",
            "revocation_reasons"
        )
        def generateCheckSum(s: String): String =
            String.format("%032x", new BigInteger(1, MessageDigest.getInstance("SHA-256").digest(s.getBytes)))
    }

    trait V1Fixture extends TemplateRouteFixture {
        val templateRouteVersion = s"${ templateRoute }v1m0/"
        val checkSums: Map[String, String] = Map(
            "message_details" -> "bdf7127cf68f52ed48a7393bd0f9a99330a4b7c6955f6cd7948bd7281d71b387",
            "minded_to_reject" -> "a23e584f9b233421701e44c3367f5f1f7040b98387b3976cf928f456d0e26ace",
            "minded_to_revoke" -> "7d0fa72dcfbf78daa376ef66ce244f293c1322beec1969bcb005311ea213c5ff",
            "no_longer_minded_to_reject" -> "1f7297641906bd1a2401071ba79363dc8f93a0d5228ffe3be86f0083c1ad5dd8",
            "no_longer_minded_to_revoke" -> "e04806e5034c46802a94ddcb002df9eb1c039ec695c56595b5eac0e6470ba1fa",
            "rejection_reasons" -> "466c1b5b3885d96e93edd100dbf747c45221daf5144565d9d67b7c2d7c8c3c07",
            "revocation_reasons" -> "efe0f113254c9f778e5aae4eb2a682f871605c5cf2fcd3b006856f9bccb734f3"
        )
    }

    "V1 checksums must be equal" in new V1Fixture {
        templateNames.foreach(templateName => {
            val source = Source.fromFile(s"${ templateRouteVersion }${ templateName }${ templateSuffix }")
            val lines: String = try source.mkString finally source.close()
            val checkSum: String = generateCheckSum(lines)
            checkSum mustEqual checkSums(templateName)
        })
    }
}
