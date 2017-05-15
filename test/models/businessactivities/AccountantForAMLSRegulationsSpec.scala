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

package models.businessactivities

import org.scalatestplus.play.PlaySpec
import jto.validation.{Path, Invalid, Valid}
import jto.validation.ValidationError
import play.api.libs.json.{JsPath, JsSuccess, Json}


class AccountantForAMLSRegulationsSpec extends PlaySpec {

  "Form Validation" must {

    "successfully validate given an true value" in {
      val data = Map("accountantForAMLSRegulations" -> Seq("true"))
      val result = AccountantForAMLSRegulations.formRule.validate(data)
      result mustBe Valid(AccountantForAMLSRegulations(true))
    }

    "successfully validate given a false value" in {
      val data = Map("accountantForAMLSRegulations" -> Seq("false"))
      val result = AccountantForAMLSRegulations.formRule.validate(data)
      result mustBe Valid(AccountantForAMLSRegulations(false))
    }

    "fail validation when mandatory field is missing" in {
      val result = AccountantForAMLSRegulations.formRule.validate(Map.empty)
      result mustBe Invalid(Seq(
        (Path \ "accountantForAMLSRegulations") -> Seq(ValidationError("error.required.ba.business.use.accountant"))
      ))
    }

    "write correct data from true value" in {
      val result = AccountantForAMLSRegulations.formWrites.writes(AccountantForAMLSRegulations(true))
      result must be(Map("accountantForAMLSRegulations" -> Seq("true")))
    }

    "write correct data from false value" in {
      val result = AccountantForAMLSRegulations.formWrites.writes(AccountantForAMLSRegulations(false))
      result must be(Map("accountantForAMLSRegulations" -> Seq("false")))
    }

  }

  "JSON validation" must {

    "successfully validate given an `true` value" in {
      val json = Json.obj("accountantForAMLSRegulations" -> true)
      Json.fromJson[AccountantForAMLSRegulations](json) must
        be(JsSuccess(AccountantForAMLSRegulations(true), JsPath \ "accountantForAMLSRegulations"))
    }

    "successfully validate given an `false` value" in {
      val json = Json.obj("accountantForAMLSRegulations" -> false)
      Json.fromJson[AccountantForAMLSRegulations](json) must
        be(JsSuccess(AccountantForAMLSRegulations(false), JsPath \ "accountantForAMLSRegulations"))
    }

    "write the correct value given an NCARegisteredYes" in {
      Json.toJson(AccountantForAMLSRegulations(true)) must
        be(Json.obj("accountantForAMLSRegulations" -> true))
    }

    "write the correct value given an NCARegisteredNo" in {
      Json.toJson(AccountantForAMLSRegulations(false)) must
        be(Json.obj("accountantForAMLSRegulations" -> false))
    }
  }

}
