package utils.validation

import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.data.FormError
import utils.validation.VATNumberValidator._

class VATNumberValidatorSpec extends PlaySpec with MockitoSugar with OneServerPerSuite {

  "VATNumberValidator" should {
    "return the VAT number if vat number entered is valid" in {
      vatNumber("invalid length", "invalid value", 9)
        .bind(Map("" -> "123456789")) mustBe Right("123456789")
    }

    "return correct form error if vat number entered is incorrect" in {
      vatNumber("invalid length", "invalid value", 9).bind(Map("" -> "21" * 12))
        .left.getOrElse(Nil).contains(FormError("", "invalid length")) mustBe true

      vatNumber("invalid length", "invalid value", 9).bind(Map("" -> "121331"))
        .left.getOrElse(Nil).contains(FormError("", "invalid value")) mustBe true
    }
  }

}