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

package models.declaration

import org.scalatestplus.play.PlaySpec
import jto.validation.{Invalid, Path, Valid}
import jto.validation.ValidationError
import play.api.libs.json.{JsPath, JsSuccess, Json}


class WhoIsRegisteringSpec extends PlaySpec {

  "Form Validation" must {

    "successfully validate" when {
      "successfully validate given a valid person name" in {
        val data = Map("person" -> Seq("PersonName"))
        val result = WhoIsRegistering.formRule.validate(data)
        result mustBe Valid(WhoIsRegistering("PersonName"))
      }
    }

    "fail validation" when {
      "fail validation for missing data represented by an empty Map" in {
        val result = WhoIsRegistering.formRule.validate(Map.empty)
        result mustBe Invalid(Seq((Path \ "person", Seq(ValidationError("error.required.declaration.who.is.registering")))))
      }
    }

    "write correct data from true value" in {
      val result = WhoIsRegistering.formWrites.writes(WhoIsRegistering("PersonName"))
      result must be(Map("person" -> Seq("PersonName")))
    }
  }

  "JSON validation" must {

    "successfully validate given an model value" in {
      val json = Json.obj("person" -> "PersonName")
      Json.fromJson[WhoIsRegistering](json) must
        be(JsSuccess(WhoIsRegistering("PersonName"), JsPath \ "person"))
    }

    "successfully validate json read write" in {
      Json.toJson(WhoIsRegistering("PersonName")) must
        be(Json.obj("person" -> "PersonName"))
    }
  }

}
