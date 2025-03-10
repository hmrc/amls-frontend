/*
 * Copyright 2024 HM Revenue & Customs
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

import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsPath, JsSuccess, Json}

import java.time.LocalDate

class CashPaymentSpec extends PlaySpec with MockitoSugar {

  "CashPayment" should {

    val cashPaymentYes     =
      CashPayment(CashPaymentOverTenThousandEuros(true), Some(CashPaymentFirstDate(LocalDate.of(1990, 2, 24))))
    val cashPaymentYesJson = Json.obj("acceptedAnyPayment" -> true, "paymentDate" -> "1990-02-24")

    val cashPaymentNo     = CashPayment(CashPaymentOverTenThousandEuros(false), None)
    val cashPaymentNoJson = Json.obj("acceptedAnyPayment" -> false)

    "have isCashPaymentsComplete function which" must {
      "return true if CashPayments is complete" in {
        val completeCashPayment =
          CashPayment(CashPaymentOverTenThousandEuros(true), Some(CashPaymentFirstDate(LocalDate.of(1999, 1, 1))))

        completeCashPayment.isCashPaymentsComplete mustBe true
      }

      "return true if CashPaymentOverTenThousandEuros is false" in {
        val completeCashPayment = CashPayment(CashPaymentOverTenThousandEuros(false), None)

        completeCashPayment.isCashPaymentsComplete mustBe true
      }

      "return true if CashPaymentOverTenThousandEuros is true and no date" in {
        val inCompleteCashPayment = CashPayment(CashPaymentOverTenThousandEuros(true), None)

        inCompleteCashPayment.isCashPaymentsComplete mustBe false
      }
    }

    "on calling Update" must {

      "return CashPayment with acceptedPayment:false and paymentDate:None when passed acceptedPayment:false" in {

        CashPayment
          .update(cashPaymentYes, CashPaymentOverTenThousandEuros(false))
          .mustBe(CashPayment(CashPaymentOverTenThousandEuros(false), None))

        CashPayment
          .update(cashPaymentNo, CashPaymentOverTenThousandEuros(false))
          .mustBe(CashPayment(CashPaymentOverTenThousandEuros(false), None))
      }

      "return CashPayment with acceptedPayment:true and paymentDate:None when passed acceptedPayment:true" in {
        CashPayment
          .update(cashPaymentNo, CashPaymentOverTenThousandEuros(true))
          .mustBe(CashPayment(CashPaymentOverTenThousandEuros(true), None))
      }

      "return unchanged CashPayment when acceptedPayment:true and passed acceptedPayment:true" in {
        CashPayment
          .update(cashPaymentYes, CashPaymentOverTenThousandEuros(true))
          .mustBe(cashPaymentYes)
      }

      "return CashPayment with acceptedPayment:true and paymentDate:Some when passed paymentDate" in {
        CashPayment
          .update(cashPaymentYes, CashPaymentFirstDate(LocalDate.of(1980, 2, 24)))
          .mustBe(
            CashPayment(CashPaymentOverTenThousandEuros(true), Some(CashPaymentFirstDate(LocalDate.of(1980, 2, 24))))
          )

        CashPayment
          .update(cashPaymentNo, CashPaymentFirstDate(LocalDate.of(1980, 2, 24)))
          .mustBe(
            CashPayment(CashPaymentOverTenThousandEuros(true), Some(CashPaymentFirstDate(LocalDate.of(1980, 2, 24))))
          )
      }
    }

    "JSON validation" must {

      "successfully validate given a `Yes` value" in {

        Json.fromJson[CashPayment](cashPaymentYesJson) must
          be(JsSuccess(cashPaymentYes, JsPath))
      }

      "successfully validate given a `No` value" in {

        Json.fromJson[CashPayment](cashPaymentNoJson) must
          be(JsSuccess(cashPaymentNo, JsPath))
      }

      "Successfully read and write Json data" in {
        CashPayment.jsonReads.reads(CashPayment.jsonWrites.writes(cashPaymentYes)) must be(JsSuccess(cashPaymentYes))
      }

      "write the correct value" in {

        Json.toJson(cashPaymentNo) must
          be(Json.obj("acceptedAnyPayment" -> false))

        Json.toJson(cashPaymentYes) must
          be(
            Json.obj(
              "acceptedAnyPayment" -> true,
              "paymentDate"        -> LocalDate.of(1990, 2, 24)
            )
          )
      }
    }
  }
}
