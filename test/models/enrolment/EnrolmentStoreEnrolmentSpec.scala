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

package models.enrolment

import generators.{AmlsReferenceNumberGenerator, BaseGenerator}
import org.scalatest.MustMatchers
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json

class EnrolmentStoreEnrolmentSpec extends PlaySpec with MustMatchers with BaseGenerator with AmlsReferenceNumberGenerator {

  trait Fixture {
    //noinspection ScalaStyle
    val userId = numSequence(10).sample.get
    val postCode = postcodeGen.sample.get
  }

  "The model" must {
    "serialize to the correct Json" in new Fixture {
      val model = EnrolmentStoreEnrolment(userId, postCode)

      val expectedJson = Json.obj(
        "userId" -> userId,
        "friendlyName" -> "AMLS Enrolment",
        "type" -> "principal",
        "verifiers" -> Seq(
          EnrolmentIdentifier("Postcode", postCode)
        )
      )

      Json.toJson(model) mustBe expectedJson
    }
  }

}
