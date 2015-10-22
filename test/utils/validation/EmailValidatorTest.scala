package utils.validation

import org.scalatest.mock.MockitoSugar
import uk.gov.hmrc.play.test.UnitSpec
import utils.validation.EmailValidator._
import play.api.data.FormError

class EmailValidatorTest extends UnitSpec with MockitoSugar with amls.FakeAmlsApp {

  "mandatoryEmail" should {
    "return the email if the email format is correct" in {
      mandatoryEmail("blank message", "invalid length", "invalid value")
        .bind(Map("" -> "aaaa@aaa.com")) shouldBe Right("aaaa@aaa.com")
    }

    "return correct form error if the email format is incorrect" in {
      mandatoryEmail("blank message", "invalid length", "invalid value").bind(Map("" -> ""))
        .left.getOrElse(Nil).contains(FormError("", "blank message")) shouldBe true

      mandatoryEmail("blank message", "invalid length", "invalid value").bind(Map("" -> "a" * 250))
        .left.getOrElse(Nil).contains(FormError("", "invalid length")) shouldBe true

      mandatoryEmail("blank message", "invalid length", "invalid value").bind(Map("" -> "@aaa.com.uk@467"))
        .left.getOrElse(Nil).contains(FormError("", "invalid value")) shouldBe true
    }
  }

}