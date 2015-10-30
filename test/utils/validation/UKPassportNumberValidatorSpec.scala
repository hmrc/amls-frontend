package utils.validation

import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.data.FormError
import utils.validation.PassportNumberValidator._

class UKPassportNumberValidatorSpec extends PlaySpec with MockitoSugar  with OneServerPerSuite{

  "mandatory UK Passport Number" should {
    "return the passport number if format is correct" in {
      mandatoryPassportNumber("blank message", "invalid length", "invalid value")
        .bind(Map("" -> "1234563")) mustBe Right("1234563")
    }

    "return correct form error if the email format is incorrect" in {
      mandatoryPassportNumber("blank message", "invalid length", "invalid value").bind(Map("" -> ""))
        .left.getOrElse(Nil).contains(FormError("", "blank message")) mustBe true

        mandatoryPassportNumber("blank message", "invalid length", "invalid value").bind(Map("" -> "1212166565"))
        .left.getOrElse(Nil).contains(FormError("", "invalid length")) mustBe true
    }
  }

}