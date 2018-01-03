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

package models.asp

import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import jto.validation.{Invalid, Path, Valid}
import jto.validation.ValidationError
import play.api.libs.json.{JsError, JsPath, JsSuccess, Json}

class OtherBusinessTaxMattersSpec extends PlaySpec with MockitoSugar {

  "Form Validation" must {

    "successfully validate given enum value" in {
      OtherBusinessTaxMatters.formRule.validate(Map("otherBusinessTaxMatters" -> Seq("false"))) must
        be(Valid(OtherBusinessTaxMattersNo))
    }

    "successfully validate given an `Yes` value" in {
      OtherBusinessTaxMatters.formRule.validate(Map("otherBusinessTaxMatters" -> Seq("true"))) must
        be(Valid(OtherBusinessTaxMattersYes))
    }

    "Fail validation when an option is not selected" when {
      "represented by and empty string" in  {
        OtherBusinessTaxMatters.formRule.validate(Map("otherBusinessTaxMatters" -> Seq(""))) must
          be(Invalid(Seq((Path \ "otherBusinessTaxMatters") -> Seq(ValidationError("error.required.asp.other.business.tax.matters")))))
      }

      "represented by a missing field" in {
        OtherBusinessTaxMatters.formRule.validate(Map.empty[String, Seq[String]]) must
          be(Invalid(Seq((Path \ "otherBusinessTaxMatters") -> Seq(ValidationError("error.required.asp.other.business.tax.matters")))))
      }
    }

    "Fail validation when an unexpected value is provided" in  {
      OtherBusinessTaxMatters.formRule.validate(Map("otherBusinessTaxMatters" -> Seq("Wheeeeee!!!!"))) must
        be(Invalid(Seq((Path \ "otherBusinessTaxMatters") -> Seq(ValidationError("error.required.asp.other.business.tax.matters")))))
    }

    "write correct data from enum value" in {

      OtherBusinessTaxMatters.formWrites.writes(OtherBusinessTaxMattersNo) must
        be(Map("otherBusinessTaxMatters" -> Seq("false")))

    }

    "write correct data from `yes` value" in {

      OtherBusinessTaxMatters.formWrites.writes(OtherBusinessTaxMattersYes) must
        be(Map("otherBusinessTaxMatters" -> Seq("true")))

    }

  }

  "Json validation" must {

    "successfully validate given an enum value" in {

      Json.fromJson[OtherBusinessTaxMatters](Json.obj("otherBusinessTaxMatters" -> false)) must
        be(JsSuccess(OtherBusinessTaxMattersNo, JsPath))
    }

    "successfully validate given an `Yes` value" in {

      Json.fromJson[OtherBusinessTaxMatters](Json.obj("otherBusinessTaxMatters" -> true)) must
        be(JsSuccess(OtherBusinessTaxMattersYes, JsPath))
    }

    "write the correct value" in {

      Json.toJson(OtherBusinessTaxMattersNo) must
        be(Json.obj("otherBusinessTaxMatters" -> false))

      Json.toJson(OtherBusinessTaxMattersYes)must
        be(Json.obj("otherBusinessTaxMatters" -> true))
    }


    }
    val model: Asp = Asp(otherBusinessTaxMatters = Some(OtherBusinessTaxMattersYes))
    val test: Option[String] = model.otherBusinessTaxMatters.map {
      case OtherBusinessTaxMattersNo => "lbl.no"
      case OtherBusinessTaxMattersYes => "lbl.yes"
  }

}
