package utils.validation

import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import uk.gov.hmrc.play.test.WithFakeApplication
import utils.validation.TextValidator._
import play.api.data.FormError

class TextValidatorSpec extends PlaySpec with MockitoSugar  with OneServerPerSuite {
  private val maxLength = 35
  "mandatoryText" should {
    "return valid string if correct" in {
      mandatoryText("blank message", "invalid length", maxLength).bind(Map("" -> "barry")) mustBe Right("barry")
      mandatoryText("blank message", "invalid length", maxLength)
        .bind(Map("" -> "1234myname1234")) mustBe Right("1234myname1234")
    }

    "report an invalid error" in {
      mandatoryText("blank message", "invalid length", maxLength).bind(Map("" -> "")) mustBe
        Left(List(FormError("", "blank message")))
      mandatoryText("blank message", "invalid length", maxLength).bind(Map("" -> "a" * 250)) mustBe
        Left(List(FormError("", "invalid length")))
    }

  }

}