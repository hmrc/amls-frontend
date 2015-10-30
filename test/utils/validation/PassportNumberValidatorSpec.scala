package utils.validation

import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.data.FormError
import utils.validation.PassportNumberValidator._

class PassportNumberValidatorSpec extends PlaySpec with MockitoSugar  with OneServerPerSuite{

  "mandatory UK Passport Number" should {
    "return the passport number if format is correct" in {
      mandatoryPassportNumber("blank message", "invalid length", "invalid value")
        .bind(Map("" -> "123456789")) mustBe Right("123456789")
    }

    "return correct form error if the email format is incorrect" in {
      mandatoryPassportNumber("blank message", "invalid length", "invalid value").bind(Map("" -> ""))
        .left.getOrElse(Nil).contains(FormError("", "blank message")) mustBe true

        mandatoryPassportNumber("blank message", "invalid length", "invalid value").bind(Map("" -> "1212166565"))
        .left.getOrElse(Nil).contains(FormError("", "invalid length")) mustBe true
    }
  }

}