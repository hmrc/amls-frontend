/*
 * Copyright 2024 HM Revenue & Customs
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

package models.responsiblepeople

import models.Country
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsError, JsPath, JsSuccess, Json}

class NationalitySpec extends PlaySpec with MockitoSugar {

  "JSON" must {

    "Read json and write the option British successfully" in {

      Nationality.jsonReads.reads(Nationality.jsonWrites.writes(British)) must be(JsSuccess(British, JsPath))
    }

    "Read read and write the option `other country` successfully" in {
      val json = Nationality.jsonWrites.writes(OtherCountry(Country("United Kingdom", "GB")))
      Nationality.jsonReads.reads(json) must be(
        JsSuccess(OtherCountry(Country("United Kingdom", "GB")), JsPath \ "otherCountry")
      )
    }

    "fail to validate given an invalid value supplied that is not matching to any nationality" in {

      Nationality.jsonReads.reads(Json.obj("nationality" -> "10")) must be(
        JsError(List((JsPath, List(play.api.libs.json.JsonValidationError("error.invalid")))))
      )

    }
  }
}
