/*
 * Copyright 2019 HM Revenue & Customs
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

package models.renewal

import jto.validation.{Invalid, Path, Valid, ValidationError}
import models.CharacterSets
import play.api.libs.json.{JsError, JsPath, JsSuccess, Json}
import utils.AmlsSpec

class UsesForeignCurrenciesSpec extends AmlsSpec with CharacterSets {

  "UsesForeignCurrencies renewal" must {
    "pass validation" when {
      "yes is selected" in {
        val model = Map("usesForeignCurrencies" -> Seq("true"))

        UsesForeignCurrencies.formRule.validate(model) must be(Valid(UsesForeignCurrenciesYes))
      }

      "no is selected" in {
        val model = Map("usesForeignCurrencies" -> Seq("false"))

        UsesForeignCurrencies.formRule.validate(model) must be(Valid(UsesForeignCurrenciesNo))
      }
    }

    "fail validation" when {
      "usesForeignCurrencies field is missing" in {
        val model = Map[String, Seq[String]]()

        UsesForeignCurrencies.formRule.validate(model) must
          be(Invalid(List((Path \ "usesForeignCurrencies", Seq(ValidationError("error.required.renewal.wc.foreign.currencies"))))))
      }

      "given no data represented by an empty Map" in {
        UsesForeignCurrencies.formRule.validate(Map.empty) must
          be(Invalid(Seq((Path \ "usesForeignCurrencies") -> Seq(ValidationError("error.required.renewal.wc.foreign.currencies")))))
      }
    }

    "validate form write for option No" in {
      val map = Map("usesForeignCurrencies" -> Seq("false"))

      UsesForeignCurrencies.formWrites.writes(UsesForeignCurrenciesNo) must be (map)
    }

    "validate form write for option Yes" in {
      val map = Map("usesForeignCurrencies" -> Seq("true"))

      UsesForeignCurrencies.formWrites.writes(UsesForeignCurrenciesYes) must be (map)
    }

    "form write test" in {
      val map = Map("usesForeignCurrencies" -> Seq("false"))

      UsesForeignCurrencies.formWrites.writes(UsesForeignCurrenciesNo) must be(map)
    }

    "JSON validation" must {
      "successfully validate given values" in {
        val json =  Json.obj("foreignCurrencies" -> true)

        Json.fromJson[UsesForeignCurrencies](json) must be(JsSuccess(UsesForeignCurrenciesYes, JsPath))
      }

      "successfully validate given values with option No" in {
        val json =  Json.obj("foreignCurrencies" -> false)

        Json.fromJson[UsesForeignCurrencies](json) must be(JsSuccess(UsesForeignCurrenciesNo, JsPath))
      }

      "fail when on path is missing" in {
        Json.fromJson[UsesForeignCurrencies](Json.obj()) must
          be(JsError((JsPath \"foreignCurrencies") -> play.api.data.validation.ValidationError("error.path.missing")))
      }

      "fail when on invalid data" in {
        Json.fromJson[UsesForeignCurrencies](Json.obj("foreignCurrencies" -> "")) must
          be(JsError((JsPath \ "foreignCurrencies") -> play.api.data.validation.ValidationError("error.expected.jsboolean")))
      }

      "write valid data in using json write" in {
        Json.toJson[UsesForeignCurrencies](UsesForeignCurrenciesYes) must be (Json.obj("foreignCurrencies" -> true))
      }

      "write valid data in using json write with Option No" in {
        Json.toJson[UsesForeignCurrencies](UsesForeignCurrenciesNo) must be (Json.obj("foreignCurrencies" -> false))
      }
    }
  }
}
