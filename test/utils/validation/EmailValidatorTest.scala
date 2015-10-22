package utils.validation

import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import uk.gov.hmrc.play.test.{WithFakeApplication}
import utils.validation.EmailValidator._
import play.api.data.FormError

class EmailValidatorTest extends PlaySpec with MockitoSugar  with WithFakeApplication{

  "mandatoryEmail" should {
    "return the email if the email format is correct" in {
      mandatoryEmail("blank message", "invalid length", "invalid value")
        .bind(Map("" -> "aaaa@aaa.com")) mustBe Right("aaaa@aaa.com")
    }

    "return correct form error if the email format is incorrect" in {
      mandatoryEmail("blank message", "invalid length", "invalid value").bind(Map("" -> ""))
        .left.getOrElse(Nil).contains(FormError("", "blank message")) mustBe true

      mandatoryEmail("blank message", "invalid length", "invalid value").bind(Map("" -> "a" * 250))
        .left.getOrElse(Nil).contains(FormError("", "invalid length")) mustBe true

      mandatoryEmail("blank message", "invalid length", "invalid value").bind(Map("" -> "@aaa.com.uk@467"))
        .left.getOrElse(Nil).contains(FormError("", "invalid value")) mustBe true
    }
  }

}