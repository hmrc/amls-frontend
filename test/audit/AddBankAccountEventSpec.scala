package audit

import models.bankdetails._
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import uk.gov.hmrc.play.audit.AuditExtensions._
import uk.gov.hmrc.play.http.HeaderCarrier

class AddBankAccountEventSpec extends PlaySpec with OneAppPerSuite {

  implicit val headerCarrier = HeaderCarrier()

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
          "accountNumber" -> "ASD123",
          "iban" -> ""
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
          "sortCode" -> "",
          "accountNumber" -> "",
          "iban" -> "9ds8ofidf"
        )

        result.detail mustBe expected
      }

      "bank account is a business account" in {
        val account = BankDetails(Some(BelongsToBusiness))
        val result = AddBankAccountEvent(account)

        result.detail("accountType") mustBe "business"
      }

      "bank account belongs to some other business" in {
        val account = BankDetails(Some(BelongsToOtherBusiness))
        val result = AddBankAccountEvent(account)

        result.detail("accountType") mustBe "other business"
      }
    }
  }
}
