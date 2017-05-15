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

package models.aboutthebusiness

import cats.data.Validated.{Invalid, Valid}
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import jto.validation.Path
import play.api.data.validation.ValidationError
import play.api.libs.json._

class CorporationTaxRegisteredSpec extends PlaySpec with MockitoSugar {

  "Form Validation" must {
    import jto.validation.ValidationError
    "successfully validate" when {
      "given a 'false' value" in {

        val data = Map("registeredForCorporationTax" -> Seq("false"))

        CorporationTaxRegistered.formRule.validate(data) must
          be(Valid(CorporationTaxRegisteredNo))
      }

      "given a 'true' value and a valid corporation tax reference" in {

        val data = Map(
          "registeredForCorporationTax" -> Seq("true"),
          "corporationTaxReference" -> Seq("1234567890")
        )

        CorporationTaxRegistered.formRule.validate(data) must
          be(Valid(CorporationTaxRegisteredYes("1234567890")))
      }
    }

    "fail validation" when {
      "no option is selected - represented by an empty Map" in {

        CorporationTaxRegistered.formRule.validate(Map.empty) must
          be(Invalid(Seq(
            (Path \ "registeredForCorporationTax") -> Seq(ValidationError("error.required.atb.corporation.tax"))
          )))
      }

      "no option is selected - represented by an empty string" in {

        val data = Map(
          "registeredForCorporationTax" -> Seq("")
        )

        CorporationTaxRegistered.formRule.validate(data) must
          be(Invalid(Seq(
            (Path \ "registeredForCorporationTax") -> Seq(ValidationError("error.required.atb.corporation.tax"))
          )))
      }

      "given a 'true' value and a corporation tax reference containing too many characters" in {

        val data = Map(
          "registeredForCorporationTax" -> Seq("true"),
          "corporationTaxReference" -> Seq("1" * 15)
        )

        CorporationTaxRegistered.formRule.validate(data) must
          be(Invalid(Seq(
            (Path \ "corporationTaxReference") -> Seq(ValidationError("error.invalid.atb.corporation.tax.number"))
          )))
      }
      "given a 'true' value and a corporation tax reference containing too few characters" in {

        val data = Map(
          "registeredForCorporationTax" -> Seq("true"),
          "corporationTaxReference" -> Seq("1" * 3)
        )

        CorporationTaxRegistered.formRule.validate(data) must
          be(Invalid(Seq(
            (Path \ "corporationTaxReference") -> Seq(ValidationError("error.invalid.atb.corporation.tax.number"))
          )))
      }
      "given a 'true' value and a corporation tax reference containing non-numeric characters" in {

        val data = Map(
          "registeredForCorporationTax" -> Seq("true"),
          "corporationTaxReference" -> Seq("12abcdefg")
        )

        CorporationTaxRegistered.formRule.validate(data) must
          be(Invalid(Seq(
            (Path \ "corporationTaxReference") -> Seq(ValidationError("error.invalid.atb.corporation.tax.number"))
          )))
      }
      "given a 'true' value and a missing corporation tax reference represented by a missing field" in {

        val data = Map(
          "registeredForCorporationTax" -> Seq("true")
        )

        CorporationTaxRegistered.formRule.validate(data) must
          be(Invalid(Seq(
            (Path \ "corporationTaxReference") -> Seq(ValidationError("error.required"))
          )))
      }
      "given a 'true' value and a missing corporation tax reference represented by an empty string" in {

        val data = Map(
          "registeredForCorporationTax" -> Seq("true"),
          "corporationTaxReference" -> Seq("")
        )

        CorporationTaxRegistered.formRule.validate(data) must
          be(Invalid(Seq(
            (Path \ "corporationTaxReference") -> Seq(ValidationError("error.required.atb.corporation.tax.number"))
          )))
      }
      "given a 'true' value and a missing corporation tax reference represented by a sequence of whitespace" in {

        val data = Map(
          "registeredForCorporationTax" -> Seq("true"),
          "corporationTaxReference" -> Seq("      ")
        )

        CorporationTaxRegistered.formRule.validate(data) must
          be(Invalid(Seq(
            (Path \ "corporationTaxReference") -> Seq(ValidationError("error.invalid.atb.corporation.tax.number"))
          )))
      }
    }

    "write correct data from enum value" in {

      CorporationTaxRegistered.formWrites.writes(CorporationTaxRegisteredNo) must
        be(Map("registeredForCorporationTax" -> Seq("false")))

    }

    "write correct data from `Yes` value" in {

      CorporationTaxRegistered.formWrites.writes(CorporationTaxRegisteredYes("1234567890")) must
        be(Map("registeredForCorporationTax" -> Seq("true"), "corporationTaxReference" -> Seq("1234567890")))
    }
  }

  "JSON validation" must {

    "successfully validate given an false value" in {
      Json.fromJson[CorporationTaxRegistered](Json.obj("registeredForCorporationTax" -> false)) must
        be(JsSuccess(CorporationTaxRegisteredNo, JsPath))
    }

    "successfully validate given an `Yes` value" in {

      val json = Json.obj("registeredForCorporationTax" -> true, "corporationTaxReference" -> "1234567890")

      Json.fromJson[CorporationTaxRegistered](json) must
        be(JsSuccess(CorporationTaxRegisteredYes("1234567890"), JsPath \ "corporationTaxReference"))
    }

    "fail to validate when given an empty `Yes` value" in {

      val json = Json.obj("registeredForCorporationTax" -> true)

      Json.fromJson[CorporationTaxRegistered](json) must
        be(JsError((JsPath \ "corporationTaxReference") -> ValidationError("error.path.missing")))
    }

    "write the correct value" in {

      Json.toJson(CorporationTaxRegisteredNo) must
        be(Json.obj("registeredForCorporationTax" -> false))

      Json.toJson(CorporationTaxRegisteredYes("1234567890")) must
        be(Json.obj(
          "registeredForCorporationTax" -> true,
          "corporationTaxReference" -> "1234567890"
        ))
    }
  }


}
