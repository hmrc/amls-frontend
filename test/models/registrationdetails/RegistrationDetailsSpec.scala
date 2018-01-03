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

package models.registrationdetails

import org.scalatest.MustMatchers
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsString, JsSuccess, Json}
import cats.implicits._
import org.joda.time.LocalDate

class RegistrationDetailsSpec extends PlaySpec with MustMatchers {

  "The RegistrationDetails model" must {
    "deserialize from json" in {
      val expectedModel = RegistrationDetails("Test Company", isIndividual = false)

      val json = Json.obj(
        "companyName" -> "Test Company",
        "isIndividual" -> false
      )

      Json.fromJson[RegistrationDetails](json) mustBe JsSuccess(expectedModel)
    }
  }

}
