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

package models.hvd

import jto.validation.{Invalid, Path, Valid, ValidationError}
import org.joda.time.LocalDate
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec

class CashPaymentFirstDateSpec extends PlaySpec with MockitoSugar {

  "CashPaymentFirstDateSpec" should {

    "Form Validation" must {

      "successfully validate given a date" in {

        val data = Map(
          "acceptedAnyPayment" -> Seq("true"),
          "paymentDate.day" -> Seq("15"),
          "paymentDate.month" -> Seq("2"),
          "paymentDate.year" -> Seq("1956")
        )

        CashPaymentFirstDate.formRule.validate(data) must
          be(Valid(CashPaymentFirstDate(new LocalDate(1956, 2, 15))))
      }

      "fail validation" when {
        "given a day in future beyond end of 2099" in {
          val model = CashPaymentFirstDate.formWrites.writes(CashPaymentFirstDate(new LocalDate(2100, 1, 1)))

          CashPaymentFirstDate.formRule.validate(model) must be(Invalid(Seq(
            Path \ "paymentDate" -> Seq(ValidationError("error.future.date"))
          )))
        }
      }

      "fail validation" when {
        "given a day in the past before start of 1900" in {
          val model = CashPaymentFirstDate.formWrites.writes(CashPaymentFirstDate(new LocalDate(1089, 12, 31)))

          CashPaymentFirstDate.formRule.validate(model) must
            be(Invalid(Seq(Path \ "paymentDate" -> Seq(ValidationError("error.allowed.start.date")))))
        }
      }

      "fail to validate given an invalid date" in {

        val data = Map(
          "acceptedAnyPayment" -> Seq("true"),
          "paymentDate.day" -> Seq("30"),
          "paymentDate.month" -> Seq("2"),
          "paymentDate.year" -> Seq("1956")
        )

        CashPaymentFirstDate.formRule.validate(data) must
          be(Invalid(Seq(Path \ "paymentDate" -> Seq(ValidationError("error.expected.jodadate.format", "yyyy-MM-dd")))))
      }

      "fail to validate given an future date" in {
        val model = CashPaymentFirstDate.formWrites.writes(CashPaymentFirstDate(LocalDate.now.plusMonths(1)))

        CashPaymentFirstDate.formRule.validate(model) must
          be(Invalid(Seq(Path \ "paymentDate" -> Seq(ValidationError("error.future.date")))))
      }

      "fail to validate given missing day" in {

        val data = Map(
          "acceptedAnyPayment" -> Seq("true"),
          "paymentDate.day" -> Seq(""),
          "paymentDate.month" -> Seq("2"),
          "paymentDate.year" -> Seq("1956")
        )

        CashPaymentFirstDate.formRule.validate(data) must
          be(Invalid(Seq(Path \ "paymentDate" -> Seq(ValidationError("error.expected.jodadate.format", "yyyy-MM-dd")))))
      }

      "fail to validate given missing month" in {

        val data = Map(
          "acceptedAnyPayment" -> Seq("true"),
          "paymentDate.day" -> Seq("2"),
          "paymentDate.month" -> Seq(""),
          "paymentDate.year" -> Seq("1956")
        )

        CashPaymentFirstDate.formRule.validate(data) must
          be(Invalid(Seq(Path \ "paymentDate" -> Seq(ValidationError("error.expected.jodadate.format", "yyyy-MM-dd")))))
      }

      "fail to validate given missing year" in {

        val data = Map(
          "acceptedAnyPayment" -> Seq("true"),
          "paymentDate.day" -> Seq("1"),
          "paymentDate.month" -> Seq("2"),
          "paymentDate.year" -> Seq("")
        )

        CashPaymentFirstDate.formRule.validate(data) must
          be(Invalid(Seq(Path \ "paymentDate" -> Seq(ValidationError("error.expected.jodadate.format", "yyyy-MM-dd")))))
      }

      "write correct data from `Yes` value" in {

        val cashPaymentFirstDate = CashPaymentFirstDate(new LocalDate(1990, 2, 24))

        CashPaymentFirstDate.formWrites.writes(cashPaymentFirstDate) must
          be(Map(
            "paymentDate.day" -> List("24"), "paymentDate.month" -> List("2"), "paymentDate.year" -> List("1990")))
      }
    }
  }
}
