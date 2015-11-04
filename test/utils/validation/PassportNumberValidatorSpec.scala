package utils.validation

import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.data.FormError
import utils.validation.PassportNumberValidator._

class PassportNumberValidatorSpec extends PlaySpec with MockitoSugar  with OneServerPerSuite{

  "mandatory UK Passport Number" should {
    "return the passport number if uk format is correct" in {
      mandatoryPassportNumber("uk passport", "blank message", "invalid length", "invalid value")
        .bind(Map("uk passport" -> "true", "" -> "12F456789")) mustBe Right("12F456789")
      mandatoryPassportNumber("uk passport", "blank message", "invalid length", "invalid value")
        .bind(Map("uk passport" -> "true", "" -> "123456A")) mustBe Right("123456A")
      mandatoryPassportNumber("uk passport", "blank message", "invalid length", "invalid value")
        .bind(Map("uk passport" -> "true", "" -> "A123456")) mustBe Right("A123456")
    }

    "return the passport number if non UK format is correct" in {
      mandatoryPassportNumber("non uk passport", "blank message", "invalid length", "invalid value")
        .bind(Map("" -> "1" * 40)) mustBe Right("1" * 40)
    }

    "return correct form error if a uk passport number format is incorrect" in {
      mandatoryPassportNumber("uk passport", "blank message", "invalid length", "invalid value")
        .bind(Map("uk passport" -> "true", "" -> ""))
        .left.getOrElse(Nil).contains(FormError("", "blank message")) mustBe true

      mandatoryPassportNumber("uk passport", "blank message", "invalid length", "invalid value")
        .bind(Map("uk passport" -> "true", "" -> "12165"))
        .left.getOrElse(Nil).contains(FormError("", "invalid length")) mustBe true

      mandatoryPassportNumber("uk passport", "blank message", "invalid length", "invalid value")
        .bind(Map("uk passport" -> "true", "" -> "1212166565"))
        .left.getOrElse(Nil).contains(FormError("", "invalid length")) mustBe true

      mandatoryPassportNumber("uk passport", "blank message", "invalid length", "invalid value")
        .bind(Map("uk passport" -> "true", "" -> "12121@656"))
        .left.getOrElse(Nil).contains(FormError("", "invalid value")) mustBe true

      mandatoryPassportNumber("uk passport", "blank message", "invalid length", "invalid value")
        .bind(Map("uk passport" -> "true", "" -> "123A456"))
        .left.getOrElse(Nil).contains(FormError("", "invalid value")) mustBe true
    }

    "return correct form error if a non uk passport number format is incorrect" in {
      mandatoryPassportNumber("uk passport", "blank message", "invalid length", "invalid value")
        .bind(Map("uk passport" -> "false", "" -> ""))
        .left.getOrElse(Nil).contains(FormError("", "blank message")) mustBe true

      mandatoryPassportNumber("uk passport", "blank message", "invalid length", "invalid value")
        .bind(Map("uk passport" -> "false", "" -> "1" * 41))
        .left.getOrElse(Nil).contains(FormError("", "invalid length")) mustBe true

      mandatoryPassportNumber("uk passport", "blank message", "invalid length", "invalid value")
        .bind(Map("uk passport" -> "false", "" -> "121£21@656"))
        .left.getOrElse(Nil).contains(FormError("", "invalid value")) mustBe true
    }
  }

}