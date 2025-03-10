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
import play.api.libs.json.Json

class BankAccountSpec extends PlaySpec with MockitoSugar {

  val ukAccount    = BankAccount(Some(BankAccountIsUk(true)), None, Some(UKAccount("12341234", "000000")))
  val nonUkIban    = BankAccount(
    Some(BankAccountIsUk(false)),
    Some(BankAccountHasIban(true)),
    Some(NonUKIBANNumber("ABCDEFGHIJKLMNOPQRSTUVWXYZABCD"))
  )
  val nonUkAccount = BankAccount(
    Some(BankAccountIsUk(false)),
    Some(BankAccountHasIban(false)),
    Some(NonUKAccountNumber("ABCDEFGHIJKLMNOPQRSTUVWXYZABCD"))
  )

  "BankAccount" must {
    "serialise and deserialise correctly" in {
      Json.toJson(ukAccount).as[BankAccount] mustBe ukAccount
      Json.toJson(nonUkAccount).as[BankAccount] mustBe nonUkAccount
      Json.toJson(nonUkIban).as[BankAccount] mustBe nonUkIban
    }

    "update correctly" when {
      "filling in UK account" in {
        BankAccount(None, None, None)
          .isUk(BankAccountIsUk(true))
          .account(UKAccount("12341234", "000000")) mustBe ukAccount
      }
      "filling in non-UK IBAN account" in {
        BankAccount(None, None, None)
          .isUk(BankAccountIsUk(false))
          .hasIban(BankAccountHasIban(true))
          .account(NonUKIBANNumber("ABCDEFGHIJKLMNOPQRSTUVWXYZABCD")) mustBe nonUkIban
      }
      "filling in non-UK account number" in {
        BankAccount(None, None, None)
          .isUk(BankAccountIsUk(false))
          .hasIban(BankAccountHasIban(false))
          .account(NonUKAccountNumber("ABCDEFGHIJKLMNOPQRSTUVWXYZABCD")) mustBe nonUkAccount
      }

      "is UK account and" when {
        "switching to non UK account" in {
          ukAccount.isUk(BankAccountIsUk(false)) mustBe BankAccount(Some(BankAccountIsUk(false)), None, None)
        }
        "is UK is identical" in {
          ukAccount.isUk(BankAccountIsUk(true)) mustBe ukAccount
        }
      }

      "is non UK IBAN account and" when {
        "has IBAN is changed" in {
          nonUkIban.hasIban(BankAccountHasIban(false)) mustBe BankAccount(
            Some(BankAccountIsUk(false)),
            Some(BankAccountHasIban(false)),
            None
          )
        }
        "has IBAN is identical" in {
          nonUkIban.hasIban(BankAccountHasIban(true)) mustBe nonUkIban
        }
      }
    }
  }
}
