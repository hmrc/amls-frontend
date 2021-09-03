/*
 * Copyright 2021 HM Revenue & Customs
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
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsError, JsPath, JsSuccess, Json}

class PercentageOfCashPaymentOver15000Spec extends PlaySpec {

  "PercentageOfCashPaymentOver15000" should {
    "Form Validation" must {

      "successfully validate given an enum value" in {

        PercentageOfCashPaymentOver15000.formRule.validate(Map("percentage" -> Seq("01"))) must
          be(Valid(PercentageOfCashPaymentOver15000.First))

        PercentageOfCashPaymentOver15000.formRule.validate(Map("percentage" -> Seq("02"))) must
          be(Valid(PercentageOfCashPaymentOver15000.Second))

        PercentageOfCashPaymentOver15000.formRule.validate(Map("percentage" -> Seq("03"))) must
          be(Valid(PercentageOfCashPaymentOver15000.Third))

        PercentageOfCashPaymentOver15000.formRule.validate(Map("percentage" -> Seq("04"))) must
          be(Valid(PercentageOfCashPaymentOver15000.Fourth))

        PercentageOfCashPaymentOver15000.formRule.validate(Map("percentage" -> Seq("05"))) must
          be(Valid(PercentageOfCashPaymentOver15000.Fifth))

      }

      "write correct data from enum value" in {

        PercentageOfCashPaymentOver15000.formWrites.writes(PercentageOfCashPaymentOver15000.First) must
          be(Map("percentage" -> Seq("01")))

        PercentageOfCashPaymentOver15000.formWrites.writes(PercentageOfCashPaymentOver15000.Second) must
          be(Map("percentage" -> Seq("02")))

        PercentageOfCashPaymentOver15000.formWrites.writes(PercentageOfCashPaymentOver15000.Third) must
          be(Map("percentage" -> Seq("03")))

        PercentageOfCashPaymentOver15000.formWrites.writes(PercentageOfCashPaymentOver15000.Fourth) must
          be(Map("percentage" -> Seq("04")))

        PercentageOfCashPaymentOver15000.formWrites.writes(PercentageOfCashPaymentOver15000.Fifth) must
          be(Map("percentage" -> Seq("05")))

      }


      "throw error on invalid data" in {
        PercentageOfCashPaymentOver15000.formRule.validate(Map("percentage" -> Seq("20"))) must
          be(Invalid(Seq((Path \ "percentage", Seq(ValidationError("error.invalid"))))))
      }

      "throw error on empty data" in {
        PercentageOfCashPaymentOver15000.formRule.validate(Map.empty) must
          be(Invalid(Seq((Path \ "percentage", Seq(ValidationError("error.required.renewal.hvd.percentage"))))))
      }
    }

    "JSON validation" must {

      "successfully validate given an enum value" in {

        Json.fromJson[PercentageOfCashPaymentOver15000](Json.obj("percentage" -> "01")) must
          be(JsSuccess(PercentageOfCashPaymentOver15000.First, JsPath ))

        Json.fromJson[PercentageOfCashPaymentOver15000](Json.obj("percentage" -> "02")) must
          be(JsSuccess(PercentageOfCashPaymentOver15000.Second, JsPath ))

        Json.fromJson[PercentageOfCashPaymentOver15000](Json.obj("percentage" -> "03")) must
          be(JsSuccess(PercentageOfCashPaymentOver15000.Third, JsPath ))

        Json.fromJson[PercentageOfCashPaymentOver15000](Json.obj("percentage" -> "04")) must
          be(JsSuccess(PercentageOfCashPaymentOver15000.Fourth, JsPath ))

        Json.fromJson[PercentageOfCashPaymentOver15000](Json.obj("percentage" -> "05")) must
          be(JsSuccess(PercentageOfCashPaymentOver15000.Fifth, JsPath ))

      }

      "write the correct value" in {

        Json.toJson(PercentageOfCashPaymentOver15000.First.asInstanceOf[PercentageOfCashPaymentOver15000]) must
          be(Json.obj("percentage" -> "01"))

        Json.toJson(PercentageOfCashPaymentOver15000.Second.asInstanceOf[PercentageOfCashPaymentOver15000]) must
          be(Json.obj("percentage" -> "02"))

        Json.toJson(PercentageOfCashPaymentOver15000.Third.asInstanceOf[PercentageOfCashPaymentOver15000]) must
          be(Json.obj("percentage" -> "03"))

        Json.toJson(PercentageOfCashPaymentOver15000.Fourth.asInstanceOf[PercentageOfCashPaymentOver15000]) must
          be(Json.obj("percentage" -> "04"))

        Json.toJson(PercentageOfCashPaymentOver15000.Fifth.asInstanceOf[PercentageOfCashPaymentOver15000]) must
          be(Json.obj("percentage" -> "05"))

      }

      "throw error for invalid data" in {
        Json.fromJson[PercentageOfCashPaymentOver15000](Json.obj("percentage" -> "20")) must
          be(JsError(JsPath , play.api.libs.json.JsonValidationError("error.invalid")))
      }
    }
   
  }
}
