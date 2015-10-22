package utils.validation

import org.scalatest.mock.MockitoSugar
import uk.gov.hmrc.play.test.UnitSpec
import utils.validation.TextValidator._
import play.api.data.FormError

class TextValidatorTest extends UnitSpec with MockitoSugar with amls.FakeAmlsApp {

  "mandatoryText" should {
    "return valid string if correct" in {
      mandatoryText("blank message", "invalid length", "validationMaxLengthFirstName").bind(Map("" -> "barry")) shouldBe Right("barry")
      mandatoryText("blank message", "invalid length", "validationMaxLengthFirstName").bind(Map("" -> "1234myname1234")) shouldBe Right("1234myname1234")
    }

    "report an invalid error" in {
      mandatoryText("blank message", "invalid length", "validationMaxLengthFirstName").bind(Map("" -> "")) shouldBe
        Left(List(FormError("", "blank message")))
      mandatoryText("blank message", "invalid length", "validationMaxLengthFirstName").bind(Map("" -> "a" * 250)) shouldBe
        Left(List(FormError("", "invalid length")))
    }
  }

}