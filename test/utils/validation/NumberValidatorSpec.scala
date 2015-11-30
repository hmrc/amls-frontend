package utils.validation

import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.data.FormError
import utils.validation.NumberValidator._

class NumberValidatorSpec extends PlaySpec with MockitoSugar with OneServerPerSuite {
  val MAX_LENGTH = 9
  "VATNumberValidator" should {
    "return the VAT number if vat number entered is valid" in {
      validateNumber("invalid length", "invalid value", MAX_LENGTH)
        .bind(Map("" -> "123456789")) mustBe Right("123456789")
    }

    "return correct form error if vat number entered is incorrect - too large" in {
      validateNumber("invalid length", "invalid value", MAX_LENGTH).bind(Map("" -> "21" * 12))
        .left.getOrElse(Nil).contains(FormError("", "invalid length")) mustBe true
    }

    "return correct form error if vat number entered is incorrect - too small" in {
      validateNumber("invalid length", "invalid value", MAX_LENGTH).bind(Map("" -> "121331"))
        .left.getOrElse(Nil).contains(FormError("", "invalid length")) mustBe true
    }

    "return correct form error if vat number entered is incorrect - Alphanumeric" in {
      validateNumber("invalid length", "invalid value", MAX_LENGTH).bind(Map("" -> "aa"*2))
        .left.getOrElse(Nil).contains(FormError("", "invalid value")) mustBe true
    }
  }

}