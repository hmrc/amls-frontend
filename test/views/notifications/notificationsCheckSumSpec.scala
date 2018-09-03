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

class notificationsCheckSumSpec extends AmlsSpec with MustMatchers {

    trait TemplateRouteFixture extends AuthorisedFixture {
        val templateRoute: String = "./app/views/notifications/"
        val templateSuffix: String = ".scala.html"
    }

    trait V1Fixture extends TemplateRouteFixture {
        val templateRouteVersion = s"${ templateRoute }v1/"
    }

    val templateNames: Seq[String] = Seq(
        "message_details"
    )

    def checkSum(s: String): String = {
        String.format("%032x", new BigInteger(1, MessageDigest.getInstance("SHA-256").digest(s.getBytes)))
    }

    "V1 checksums must be equal" in new V1Fixture {
        templateNames.foreach(templateName => {
            val source = Source.fromFile(s"${ templateRouteVersion }${ templateName }${ templateSuffix }")
            val lines: String = try source.mkString finally source.close()
            val cs: String = checkSum(lines)
            cs mustEqual "61129094be6311b0704d785723e198948a96c4424d9d1c6e2ff9351be804fc6c"
        })
    }
}
