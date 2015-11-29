package utils.validation

import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import utils.validation.RadioGroupWithTextValidator._

class RadioGroupWithTextValidatorSpec extends PlaySpec with MockitoSugar  with OneServerPerSuite {

  "RadioGroupWithTextValidator" must {
    "throw error message when mandatory fields not filled correctly" in {
      val mapping = mandatoryBooleanWithText("mlrNumber", "No radio button selected", "blank value", "value not allowed")
      mapping.bind(Map("" -> "01", "mlrNumber" -> "12345078")) mustBe Right(true)

    }
  }


}
