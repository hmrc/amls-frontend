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

package models.hvd

import org.joda.time.LocalDate
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import jto.validation.{Invalid, Path, Valid}
import jto.validation.ValidationError
import play.api.libs.json.{JsError, JsPath, JsSuccess, Json}

class CashPaymentSpec extends PlaySpec with MockitoSugar {

  "CashPaymentSpec" should {
    val DefaultCashPaymentYes = CashPaymentYes(new LocalDate(1990, 2, 24))

    "Form Validation" must {

      "successfully validate given an enum value" in {

        CashPayment.formRule.validate(Map("acceptedAnyPayment" -> Seq("false"))) must
          be(Valid(CashPaymentNo))
      }

      "successfully validate given an `Yes` value" in {

        val data = Map(
          "acceptedAnyPayment" -> Seq("true"),
          "paymentDate.day" -> Seq("15"),
          "paymentDate.month" -> Seq("2"),
          "paymentDate.year" -> Seq("1956")
        )

        CashPayment.formRule.validate(data) must
          be(Valid(CashPaymentYes(new LocalDate(1956, 2, 15))))
      }

      "fail to validate when neither 'Yes' nor 'No' is selected" in {
        CashPayment.formRule.validate(Map.empty) must
          be(Invalid(Seq(
            (Path \ "acceptedAnyPayment") -> Seq(ValidationError("error.required.hvd.accepted.cash.payment"))
          )))
      }

      "fail to validate given an invalid date" in {

        val data = Map(
          "acceptedAnyPayment" -> Seq("true"),
          "paymentDate.day" -> Seq("30"),
          "paymentDate.month" -> Seq("2"),
          "paymentDate.year" -> Seq("1956")
        )

        CashPayment.formRule.validate(data) must
          be(Invalid(Seq(Path \ "paymentDate" -> Seq(ValidationError("error.expected.jodadate.format", "yyyy-MM-dd")))))
      }

      "fail to validate given an future date" in {
        val model = CashPayment.formWrites.writes(CashPaymentYes(LocalDate.now.plusMonths(1)))

        CashPayment.formRule.validate(model) must
          be(Invalid(Seq(Path \ "paymentDate" -> Seq(ValidationError("error.future.date")))))
      }

      "fail to validate given missing day" in {

        val data = Map(
          "acceptedAnyPayment" -> Seq("true"),
          "paymentDate.day" -> Seq(""),
          "paymentDate.month" -> Seq("2"),
          "paymentDate.year" -> Seq("1956")
        )

        CashPayment.formRule.validate(data) must
          be(Invalid(Seq(Path \ "paymentDate" -> Seq(ValidationError("error.expected.jodadate.format", "yyyy-MM-dd")))))
      }

      "fail to validate given missing month" in {

        val data = Map(
          "acceptedAnyPayment" -> Seq("true"),
          "paymentDate.day" -> Seq("2"),
          "paymentDate.month" -> Seq(""),
          "paymentDate.year" -> Seq("1956")
        )

        CashPayment.formRule.validate(data) must
          be(Invalid(Seq(Path \ "paymentDate" -> Seq(ValidationError("error.expected.jodadate.format", "yyyy-MM-dd")))))
      }

      "fail to validate given missing year" in {

        val data = Map(
          "acceptedAnyPayment" -> Seq("true"),
          "paymentDate.day" -> Seq("1"),
          "paymentDate.month" -> Seq("2"),
          "paymentDate.year" -> Seq("")
        )

        CashPayment.formRule.validate(data) must
          be(Invalid(Seq(Path \ "paymentDate" -> Seq(ValidationError("error.expected.jodadate.format", "yyyy-MM-dd")))))
      }

      "write correct data from enum value" in {

        CashPayment.formWrites.writes(CashPaymentNo) must
          be(Map("acceptedAnyPayment" -> Seq("false")))

      }

      "write correct data from `Yes` value" in {

        CashPayment.formWrites.writes(DefaultCashPaymentYes) must
          be(Map("acceptedAnyPayment" -> Seq("true"),
            "paymentDate.day" -> List("24"), "paymentDate.month" -> List("2"), "paymentDate.year" -> List("1990")))
      }
    }

    "JSON validation" must {

      "successfully validate given an enum value" in {

        Json.fromJson[CashPayment](Json.obj("acceptedAnyPayment" -> false)) must
          be(JsSuccess(CashPaymentNo, JsPath))
      }

      "successfully validate given an `Yes` value" in {

        val json = Json.obj("acceptedAnyPayment" -> true, "paymentDate" ->"1990-02-24")

        Json.fromJson[CashPayment](json) must
          be(JsSuccess(CashPaymentYes(new LocalDate(1990, 2, 24)), JsPath \"paymentDate"))
      }

      "fail to validate when given an empty `Yes` value" in {

        val json = Json.obj("acceptedAnyPayment" -> true)

        Json.fromJson[CashPayment](json) must
          be(JsError((JsPath \ "paymentDate") -> play.api.data.validation.ValidationError("error.path.missing")))
      }

      "Successfully read and write Json data" in {

        CashPayment.jsonReads.reads(CashPayment.jsonWrites.writes(DefaultCashPaymentYes)) must be(
          JsSuccess(CashPaymentYes(new LocalDate(1990, 2, 24)), JsPath \ "paymentDate"))
      }

      "write the correct value" in {

        Json.toJson(CashPaymentNo) must
          be(Json.obj("acceptedAnyPayment" -> false))

        Json.toJson(DefaultCashPaymentYes) must
          be(Json.obj(
            "acceptedAnyPayment" -> true,
            "paymentDate" -> new LocalDate(1990, 2, 24)
          ))
      }
    }
  }

}
