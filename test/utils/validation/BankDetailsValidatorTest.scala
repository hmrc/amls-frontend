package utils.validation

import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import uk.gov.hmrc.play.test.WithFakeApplication
import utils.validation.BankDetailsValidator._
import play.api.data.FormError

class BankDetailsValidatorTest extends PlaySpec with MockitoSugar  with WithFakeApplication {

  "mandatoryAccountNumber" should {
    "return valid string if correct" in {
      mandatoryAccountNumber("blank message", "invalid message").bind(Map("" -> "12345678")) mustBe Right("12345678")
    }

    "report an invalid error" in {
      mandatoryAccountNumber("blank message", "invalid message").bind(Map("" -> "")) mustBe Left(List(FormError("", "blank message")))
      mandatoryAccountNumber("blank message", "invalid message").bind(Map("" -> "abc")) mustBe Left(List(FormError("", "invalid message")))
    }
  }

  "mandatorySortCode" should {
    "return valid string if correct" in {
      mandatorySortCode("blank message", "invalid message").bind(Map("" -> "11-11-11")) mustBe Right("11-11-11")
      mandatorySortCode("blank message", "invalid message").bind(Map("" -> "99-99-99")) mustBe Right("99-99-99")
    }

    "report an invalid error" in {
      mandatorySortCode("blank message", "invalid message").bind(Map("" -> "")) mustBe Left(List(FormError("", "blank message")))
      mandatorySortCode("blank message", "invalid message").bind(Map("" -> "12345678")) mustBe Left(List(FormError("", "invalid message")))
      mandatorySortCode("blank message", "invalid message").bind(Map("" -> "11--11-11")) mustBe Left(List(FormError("", "invalid message")))
      mandatorySortCode("blank message", "invalid message").bind(Map("" -> "ab-cd-ef")) mustBe Left(List(FormError("", "invalid message")))
      mandatorySortCode("blank message", "invalid message").bind(Map("" -> "sdad3 e32 rb")) mustBe Left(List(FormError("", "invalid message")))
      mandatorySortCode("blank message", "invalid message").bind(Map("" -> "a" * 250)) mustBe Left(List(FormError("", "invalid message")))
    }
  }

  "mandatoryIban" should {
    "return valid string if correct" in {
      mandatoryIban("blank message", "invalid message").bind(Map("" -> "AL47 2121 1009 0000 0002 3569 8741")) mustBe
        Right("AL47212110090000000235698741")
      mandatoryIban("blank message", "invalid message").bind(Map("" -> "DK50 0040 0440 1162 43")) mustBe
        Right("DK5000400440116243")
      mandatoryIban("blank message", "invalid message").bind(Map("" -> "MT84 MALT 0110 0001 2345 MTLC AST0 01S")) mustBe
        Right("MT84MALT011000012345MTLCAST001S")
      mandatoryIban("blank message", "invalid message").bind(Map("" -> "SI56 1910 0000 0123 438")) mustBe
        Right("SI56191000000123438")
      mandatoryIban("blank message", "invalid message").bind(Map("" -> "MT84 MALT 0110 0001 2345 MTLC AST0 01SS SS")) mustBe
        Right("MT84MALT011000012345MTLCAST001SSSS")
    }

    "return valid string if correct and no spaces" in {
      mandatoryIban("blank message", "invalid message").bind(Map("" -> "AL47212110090000000235698741")) mustBe
        Right("AL47212110090000000235698741")
      mandatoryIban("blank message", "invalid message").bind(Map("" -> "DK5000400440116243")) mustBe
        Right("DK5000400440116243")
    }

    "report an invalid error" in {
      mandatoryIban("blank message", "invalid message").bind(Map("" -> "")) mustBe Left(List(FormError("", "blank message")))
      mandatoryIban("blank message", "invalid message").bind(Map("" -> "12345678")) mustBe Left(List(FormError("", "invalid message")))
      mandatoryIban("blank message", "invalid message").bind(Map("" -> "11--11-11")) mustBe Left(List(FormError("", "invalid message")))
      mandatoryIban("blank message", "invalid message").bind(Map("" -> "ab-cd-ef")) mustBe Left(List(FormError("", "invalid message")))
      mandatoryIban("blank message", "invalid message").bind(Map("" -> "sdad3 e32 rb")) mustBe Left(List(FormError("", "invalid message")))
      mandatoryIban("blank message", "invalid message").bind(Map("" -> "a" * 250)) mustBe Left(List(FormError("", "invalid message")))
      mandatoryIban("blank message", "invalid message").bind(Map("" -> ("MT84 MALT 0110 0001 2345 MTLC AST0 01S" + ("S" * 4)))) mustBe
        Left(List(FormError("", "invalid message")))
    }
  }

}