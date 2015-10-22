package utils.validation

import org.scalatest.mock.MockitoSugar
import uk.gov.hmrc.play.test.UnitSpec
import utils.validation.BankDetailsValidator._
import play.api.data.FormError

class BankDetailsValidatorTest extends UnitSpec with MockitoSugar with amls.FakeAmlsApp {

  "mandatoryAccountNumber" should {
    "return valid string if correct" in {
      mandatoryAccountNumber("blank message", "invalid message").bind(Map("" -> "12345678")) shouldBe Right("12345678")
      mandatoryAccountNumber("blank message", "invalid message").bind(Map("" -> "87654321")) shouldBe Right("87654321")
    }

    "report an invalid error" in {
      mandatoryAccountNumber("blank message", "invalid message").bind(Map("" -> "")) shouldBe Left(List(FormError("", "blank message")))
      mandatoryAccountNumber("blank message", "invalid message").bind(Map("" -> "abc")) shouldBe Left(List(FormError("", "invalid message")))
    }
  }

  "mandatorySortCode" should {
    "return valid string if correct" in {
      mandatorySortCode("blank message", "invalid message").bind(Map("" -> "11-11-11")) shouldBe Right("11-11-11")
      mandatorySortCode("blank message", "invalid message").bind(Map("" -> "99-99-99")) shouldBe Right("99-99-99")
    }

    "report an invalid error" in {
      mandatorySortCode("blank message", "invalid message").bind(Map("" -> "")) shouldBe Left(List(FormError("", "blank message")))
      mandatorySortCode("blank message", "invalid message").bind(Map("" -> "12345678")) shouldBe Left(List(FormError("", "invalid message")))
      mandatorySortCode("blank message", "invalid message").bind(Map("" -> "11--11-11")) shouldBe Left(List(FormError("", "invalid message")))
      mandatorySortCode("blank message", "invalid message").bind(Map("" -> "ab-cd-ef")) shouldBe Left(List(FormError("", "invalid message")))
      mandatorySortCode("blank message", "invalid message").bind(Map("" -> "sdad3 e32 rb")) shouldBe Left(List(FormError("", "invalid message")))
      mandatorySortCode("blank message", "invalid message").bind(Map("" -> "a" * 250)) shouldBe Left(List(FormError("", "invalid message")))
    }
  }

  "mandatoryIban" should {
    "return valid string if correct" in {
      mandatoryIban("blank message", "invalid message").bind(Map("" -> "AL47 2121 1009 0000 0002 3569 8741")) shouldBe
        Right("AL47212110090000000235698741")
      mandatoryIban("blank message", "invalid message").bind(Map("" -> "DK50 0040 0440 1162 43")) shouldBe
        Right("DK5000400440116243")
      mandatoryIban("blank message", "invalid message").bind(Map("" -> "MT84 MALT 0110 0001 2345 MTLC AST0 01S")) shouldBe
        Right("MT84MALT011000012345MTLCAST001S")
      mandatoryIban("blank message", "invalid message").bind(Map("" -> "SI56 1910 0000 0123 438")) shouldBe
        Right("SI56191000000123438")
      mandatoryIban("blank message", "invalid message").bind(Map("" -> "MT84 MALT 0110 0001 2345 MTLC AST0 01SS SS")) shouldBe
        Right("MT84MALT011000012345MTLCAST001SSSS")
    }

    "return valid string if correct and no spaces" in {
      mandatoryIban("blank message", "invalid message").bind(Map("" -> "AL47212110090000000235698741")) shouldBe
        Right("AL47212110090000000235698741")
      mandatoryIban("blank message", "invalid message").bind(Map("" -> "DK5000400440116243")) shouldBe
        Right("DK5000400440116243")
    }

    "report an invalid error" in {
      mandatoryIban("blank message", "invalid message").bind(Map("" -> "")) shouldBe Left(List(FormError("", "blank message")))
      mandatoryIban("blank message", "invalid message").bind(Map("" -> "12345678")) shouldBe Left(List(FormError("", "invalid message")))
      mandatoryIban("blank message", "invalid message").bind(Map("" -> "11--11-11")) shouldBe Left(List(FormError("", "invalid message")))
      mandatoryIban("blank message", "invalid message").bind(Map("" -> "ab-cd-ef")) shouldBe Left(List(FormError("", "invalid message")))
      mandatoryIban("blank message", "invalid message").bind(Map("" -> "sdad3 e32 rb")) shouldBe Left(List(FormError("", "invalid message")))
      mandatoryIban("blank message", "invalid message").bind(Map("" -> "a" * 250)) shouldBe Left(List(FormError("", "invalid message")))
      mandatoryIban("blank message", "invalid message").bind(Map("" -> ("MT84 MALT 0110 0001 2345 MTLC AST0 01S" + ("S" * 4)))) shouldBe
        Left(List(FormError("", "invalid message")))
    }
  }

}