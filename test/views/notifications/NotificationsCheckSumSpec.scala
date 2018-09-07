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
        val versionNumbers: Seq[String] = Seq(
            "v1m0/"
        )
        def generateCheckSum(s: String): String =
            String.format("%032x", new BigInteger(1, MessageDigest.getInstance("SHA-256").digest(s.getBytes)))
        val checkSums: Map[String, String] = Map(
            "v1m0/message_details" -> "bdf7127cf68f52ed48a7393bd0f9a99330a4b7c6955f6cd7948bd7281d71b387",
            "v1m0/minded_to_reject" -> "5e638f12882b827548af6f48c76404c936fa821e26533c2f11ccd6157b5912b",
            "v1m0/minded_to_revoke" -> "a09d17105a334528f33a39f162dd679ad417d8ae44ff8fccb6f79e0984fd1dbe",
            "v1m0/no_longer_minded_to_reject" -> "1f7297641906bd1a2401071ba79363dc8f93a0d5228ffe3be86f0083c1ad5dd8",
            "v1m0/no_longer_minded_to_revoke" -> "e04806e5034c46802a94ddcb002df9eb1c039ec695c56595b5eac0e6470ba1fa",
            "v1m0/rejection_reasons" -> "799ff1fbc55fa853404e1e1138bd1a0d29d7584b1e0ad3000803c14837bc9a92",
            "v1m0/revocation_reasons" -> "d7971a4154d343ed5c9a4821a1e2de464b091aaab7ad1ec0a1fd4510491b6dc6"
        )
    }

    "Checksums must be equal" in new TemplateRouteFixture {
        versionNumbers.foreach(versionNumber => {
            templateNames.foreach(templateName => {
                val source = Source.fromFile(s"${ templateRoute }${ versionNumber }${ templateName }${ templateSuffix }")
                val lines: String = try source.mkString finally source.close()
                val checkSum: String = generateCheckSum(lines)
                checkSum mustEqual checkSums(s"${ versionNumber }${ templateName }")
            })
        })
    }
}
