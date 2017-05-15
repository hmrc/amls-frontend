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


class NCARegisteredSpec extends PlaySpec {

  "Form Validation" must {

    "successfully validate given an true value" in {
      val data = Map("ncaRegistered" -> Seq("true"))
      val result = NCARegistered.formRule.validate(data)
      result mustBe Valid(NCARegistered(true))
    }

    "fail validation for empty data" in {
      val result = NCARegistered.formRule.validate(Map.empty)
      result mustBe Invalid(Seq((Path \ "ncaRegistered", Seq(ValidationError("error.required.ba.select.nca")))))
    }

    "successfully validate given a false value" in {
      val data = Map("ncaRegistered" -> Seq("false"))
      val result = NCARegistered.formRule.validate(data)
      result mustBe Valid(NCARegistered(false))
    }

    "write correct data from true value" in {
      val result = NCARegistered.formWrites.writes(NCARegistered(true))
      result must be(Map("ncaRegistered" -> Seq("true")))
    }

    "write correct data from false value" in {
      val result = NCARegistered.formWrites.writes(NCARegistered(false))
      result must be(Map("ncaRegistered" -> Seq("false")))
    }

  }

  "JSON validation" must {

    "successfully validate given an `true` value" in {
      val json = Json.obj("ncaRegistered" -> true)
      Json.fromJson[NCARegistered](json) must
        be(JsSuccess(NCARegistered(true), JsPath \ "ncaRegistered"))
    }

    "successfully validate given an `false` value" in {
      val json = Json.obj("ncaRegistered" -> false)
      Json.fromJson[NCARegistered](json) must
        be(JsSuccess(NCARegistered(false), JsPath \ "ncaRegistered"))
    }

    "write the correct value given an NCARegisteredYes" in {
      Json.toJson(NCARegistered(true)) must
        be(Json.obj("ncaRegistered" -> true))
    }

    "write the correct value given an NCARegisteredNo" in {
      Json.toJson(NCARegistered(false)) must
        be(Json.obj("ncaRegistered" -> false))
    }
  }

}
