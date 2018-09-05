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
            "revocation_reasons",
            "your_messages"
        )
        def generateCheckSum(s: String): String =
            String.format("%032x", new BigInteger(1, MessageDigest.getInstance("SHA-256").digest(s.getBytes)))
    }

    trait V1Fixture extends TemplateRouteFixture {
        val templateRouteVersion = s"${ templateRoute }v1m0/"
        val checkSums: Map[String, String] = Map(
            "message_details" -> "61129094be6311b0704d785723e198948a96c4424d9d1c6e2ff9351be804fc6c",
            "minded_to_reject" -> "928eb5662a97231f1b1c6c17761f5dd3bfc68d4c1bf6f19804744f4d8d799c90",
            "minded_to_revoke" -> "a65db89e3b61f3e33b7aa86877e5b3ecf604974fba47b791db4cb65a601c726",
            "no_longer_minded_to_reject" -> "8d90003634cde11db6114b49a6a50a2be34eb040cbcbf6fc1046759376c5092f",
            "no_longer_minded_to_revoke" -> "4ed45f4f0f813c544a4cc9a75748b5ea30a6ccb68a04035bf317398487461942",
            "rejection_reasons" -> "1f6199b002d0f0652c386126fd0830c42578eb70f87fc7a683a310d68ea6ff3d",
            "revocation_reasons" -> "e24be18a9e459fcf9febc16b344d9c67c1dc347a4f256821cd169c10ae1c206c",
            "your_messages" -> "d3c6185f71d52f84e814554d4e72c93cb4adf9fc728f2b352c09ae880dcdaa2"
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
