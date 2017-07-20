/*
 * Copyright 2017 HM Revenue & Customs
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

  "The RegistrationDetails model" when {
    "deserialised" must {
      "produce the correct json" when {
        "the data represents an organisation" in {
          val expectedModel = RegistrationDetails(false, Organisation("Test Organisation", true, LLP))

          val json = Json.obj(
            "isAnIndividual" -> false,
            "organisation" -> Json.obj(
              "organisationName" -> "Test Organisation",
              "isAGroup" -> true,
              "organisationType" -> "LLP"
            )
          )

          Json.fromJson[RegistrationDetails](json) mustBe JsSuccess(expectedModel)
        }

        "the data represents an individual" in {
          val expectedModel = RegistrationDetails(true, Individual("Firstname", "Middlename".some, "Lastname", new LocalDate(2002, 5, 10)))
          val json = Json.obj(
            "isAnIndividual" -> true,
            "individual" -> Json.obj(
              "firstName" -> "Firstname",
              "middleName" -> "Middlename",
              "lastName" -> "Lastname",
              "dateOfBirth" -> "2002-05-10"
            ))

          Json.fromJson[RegistrationDetails](json) mustBe JsSuccess(expectedModel)
        }
      }
    }
  }

  "The Organisation model" when {
    "deserialised" must {
      "produce the correct json" in {
        val expectedModel = Organisation("Test Organisation", true, Partnership)

        val json = Json.obj(
          "organisationName" -> "Test Organisation",
          "isAGroup" -> true,
          "organisationType" -> "Partnership"
        )

        Json.fromJson[Organisation](json) mustBe JsSuccess(expectedModel)
      }
    }
  }

  "The organisation type objects" when {
    "deserialised" must {
      "produce the correct values" in {
        Seq(
          (Partnership, "Partnership"),
          (LLP, "LLP"),
          (CorporateBody, "Corporate body"),
          (UnincorporatedBody, "Unincorporated body")
        ) foreach {
          case (t, str) => Json.fromJson[OrganisationType](JsString(str)) mustBe JsSuccess(t)
        }
      }
    }
  }

}
