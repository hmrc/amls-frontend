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

package models.declaration

import jto.validation.{Invalid, Path, Valid, ValidationError}
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsPath, JsSuccess, Json}

class BusinessNominatedOfficerSpec extends PlaySpec {

  "Form Validation" must {

    "successfully validate" when {
      "successfully validate given a valid person name" in {
        val data = Map("value" -> Seq("PersonName"))
        val result = BusinessNominatedOfficer.formRule.validate(data)
        result mustBe Valid(BusinessNominatedOfficer("PersonName"))
      }
    }

    "fail validation" when {
      "fail validation for missing data represented by an empty Map" in {
        val result = BusinessNominatedOfficer.formRule.validate(Map.empty)
        result mustBe Invalid(Seq((Path \ "value", Seq(ValidationError("error.required.declaration.nominated.officer")))))
      }
    }

    "write correct data from true value" in {
      val result = BusinessNominatedOfficer.formWrites.writes(BusinessNominatedOfficer("PersonName"))
      result must be(Map("value" -> Seq("PersonName")))
    }
  }

  "JSON validation" must {

    "successfully validate given an model value" in {
      val json = Json.obj("value" -> "PersonName")
      Json.fromJson[BusinessNominatedOfficer](json) must
        be(JsSuccess(BusinessNominatedOfficer("PersonName"), JsPath \ "value"))
    }

    "successfully validate json read write" in {
      Json.toJson(BusinessNominatedOfficer("PersonName")) must
        be(Json.obj("value" -> "PersonName"))
    }
  }

}
