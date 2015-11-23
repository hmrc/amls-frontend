package utils.validation

import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.data.FormError
import utils.validation.BooleanValidator._

class BooleanValidatorSpec extends PlaySpec with MockitoSugar  with OneServerPerSuite {
  "mandatoryBoolean" should {

    "respond appropriately if Yes is chosen" in {
      val mapping = mandatoryBoolean("blank message")
      mapping.bind(Map("" -> "true")) mustBe Right(true)
    }

    "respond appropriately if No is chosen" in {
      val mapping = mandatoryBoolean("blank message")
      mapping.bind(Map("" -> "false")) mustBe Right(false)
    }

    "respond appropriately if unbound" in {
      val mapping = mandatoryBoolean("blank message")
      mapping.binder.unbind("", true) mustBe Map("" -> "true")
    }
    "respond appropriately if unbound test" in {
      val mapping = mandatoryBoolean("blank message")
      mapping.bind(Map(""->""))
        .left.getOrElse(Nil).contains(FormError("", "blank message")) mustBe true
    }
  }
}
