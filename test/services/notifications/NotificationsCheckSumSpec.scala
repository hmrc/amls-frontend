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

package services.notifications

import java.math.BigInteger
import java.security.MessageDigest

import org.scalatest.MustMatchers
import utils.{AmlsSpec, AuthorisedFixture}

import scala.io.Source

class NotificationsCheckSumSpec extends AmlsSpec with MustMatchers {

    trait TemplateRouteFixture extends AuthorisedFixture {
        val templateRoute: String = "./app/services/notifications/"
        val templateSuffix: String = ".scala"
        val templateNames: Seq[String] = Seq(
            "MessageDetails"
        )
        val versionNumbers: Seq[String] = Seq(
            "v1m0/"
        )
        def generateCheckSum(s: String): String =
            String.format("%032x", new BigInteger(1, MessageDigest.getInstance("SHA-256").digest(s.getBytes)))
        val checkSums: Map[String, String] = Map(
            "v1m0/MessageDetails" -> "49e4bc6831d3a03264d00294745b726e42ef9bc9b44da7b234f12e8c2dedeb42"
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
