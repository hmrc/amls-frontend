package utils.validation

import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.data.FormError
import utils.validation.RadioGroupWithTextValidator._

class RadioGroupWithTextValidatorSpec extends PlaySpec with MockitoSugar  with OneServerPerSuite {

  "RadioGroupWithTextValidator" must {
    "when mandatory fields filled correctly respond with success[true, false]" in {
      val mapping = mandatoryBooleanWithText("mlrNumber", "", "No radio button selected", "blank value", "value not allowed")
      mapping.bind(Map("" -> "01", "mlrNumber" -> "12345078")) mustBe Right(Tuple2(true, false))

    }

    "when mandatory fields filled correctly respond with success[false, true]" in {
      val mapping = mandatoryBooleanWithText("mlrNumber", "", "No radio button selected", "blank value", "value not allowed")
      mapping.bind(Map("" -> "02", "mlrNumber" -> "")) mustBe Right(Tuple2(false, true))

    }

    "when mandatory fields filled correctly respond with success[false, false]" in {
      val mapping = mandatoryBooleanWithText("mlrNumber", "", "No radio button selected", "blank value", "value not allowed")
      mapping.bind(Map("" -> "03" )) mustBe Right(Tuple2(false, false))
    }

    "when mandatory fields not filled correctly respond with failure" in {
      val mapping = mandatoryBooleanWithText("mlrNumber", "", "No radio button selected", "blank value", "value not allowed")
      mapping.bind(Map("" -> "03", "mlrNumber"->"12345678", "prevMlrNum"->"12436143651436152346"))
        .left.getOrElse(Nil).contains(FormError("", "value not allowed")) mustBe true
    }

  }


}
