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

package models.bankdetails

import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsPath, JsSuccess, Json}

class AccountSpec extends PlaySpec with MockitoSugar {

  "For the Account" must {

    "displaySortCode" must {
      "return the sort code formatted for display" in {
        val account = UKAccount("12341234", "000000")
        account.displaySortCode must be("00-00-00")
      }
    }

    "JSON Read is successful for UKAccount" in {
      val jsObject = Json.obj(
        "accountNumber" -> "12345678",
        "sortCode"      -> "000000"
      )

      Account.ukJsonReads.reads(jsObject) must be(JsSuccess(UKAccount("12345678", "000000"), JsPath))
    }

    "JSON Write is successful for UKAccount" in {

      val ukAccount = UKAccount("12345678", "000000")

      val jsObject = Json.obj(
        "accountNumber" -> "12345678",
        "sortCode"      -> "000000"
      )

      Account.ukJsonWrites.writes(ukAccount) must be(jsObject)
    }

    "JSON Read is successful for Non UKAccount with IBAN" in {
      val jsObject = Json.obj(
        "IBANNumber" -> "IB12345678"
      )

      Account.nonUkIbanJsonReads.reads(jsObject) must be(
        JsSuccess(NonUKIBANNumber("IB12345678"), JsPath \ "IBANNumber")
      )
    }

    "JSON Read is successful for Non UKAccount with Account Number" in {
      val jsObject = Json.obj(
        "nonUKAccountNumber" -> "12345"
      )

      Account.nonUkAccountJsonReads.reads(jsObject) must be(
        JsSuccess(NonUKAccountNumber("12345"), JsPath \ "nonUKAccountNumber")
      )
    }

    "JSON Write is successful for Non UK Account Number" in {

      val nonUKAccountNumber = NonUKAccountNumber("12345678")

      val jsObject = Json.obj(
        "nonUKAccountNumber" -> "12345678"
      )

      Account.nonUkAccountJsonWrites.writes(nonUKAccountNumber) must be(jsObject)
    }

    "JSON Write is successful for Non UK IBAN Number" in {

      val nonUKIBANNumber = NonUKIBANNumber("12345678")

      val jsObject = Json.obj(
        "IBANNumber" -> "12345678"
      )

      Account.nonUkIbanJsonWrites.writes(nonUKIBANNumber) must be(jsObject)
    }
  }
}
