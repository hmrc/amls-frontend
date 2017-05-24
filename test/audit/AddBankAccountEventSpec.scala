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

package audit

import models.bankdetails._
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.test.FakeRequest
import uk.gov.hmrc.play.audit.AuditExtensions._
import uk.gov.hmrc.play.http.HeaderCarrier

class AddBankAccountEventSpec extends PlaySpec with OneAppPerSuite {

  implicit val headerCarrier = HeaderCarrier()
  implicit val request = FakeRequest("GET", "/test-path")

  "The bank account audit event" must {
    "serialize to the correct json" when {
      "bank account is a UK bank account" in {
        val account = BankDetails(Some(PersonalAccount), Some(BankAccount("Test account", UKAccount("ASD123", "1234567"))))
        val result = AddBankAccountEvent(account)

        val expected = headerCarrier.toAuditDetails() ++ Map(
          "accountName" -> "Test account",
          "isUkBankAccount" -> "true",
          "accountType" -> "personal",
          "sortCode" -> "1234567",
          "accountNumber" -> "ASD123"
        )

        result.detail mustBe expected
      }

      "bank account is a non-UK bank account" in {
        val account = BankDetails(Some(PersonalAccount), Some(BankAccount("Test account", NonUKIBANNumber("9ds8ofidf"))))
        val result = AddBankAccountEvent(account)

        val expected = headerCarrier.toAuditDetails() ++ Map(
          "accountName" -> "Test account",
          "isUkBankAccount" -> "false",
          "accountType" -> "personal",
          "iban" -> "9ds8ofidf"
        )

        result.detail mustBe expected
        result.tags("path") mustBe "/test-path"
      }

      "bank account is a business account" in {
        val account = BankDetails(Some(BelongsToBusiness), Some(BankAccount("Test account", UKAccount("7364823", "8377343"))))
        val result = AddBankAccountEvent(account)

        result.detail("accountType") mustBe "business"
      }

      "bank account belongs to some other business" in {
        val account = BankDetails(Some(BelongsToOtherBusiness), Some(BankAccount("Test account", UKAccount("7364823", "8377343"))))
        val result = AddBankAccountEvent(account)

        result.detail("accountType") mustBe "other business"
      }

      "bank account uses a non-UK account number" in {
        val account = BankDetails(Some(PersonalAccount), Some(BankAccount("Test account", NonUKAccountNumber("98374389hjk"))))
        val result = AddBankAccountEvent(account)

        result.detail("accountNumber") mustBe "98374389hjk"
      }

    }
  }
}
