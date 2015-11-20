package utils.validation

import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.data.FormError
import utils.validation.BooleanWithTextValidator._


class BooleanWithTextValidatorSpec extends PlaySpec with MockitoSugar  with OneServerPerSuite{
  val MAX_LENGTH = 100
  "mandatoryBoolean" should {

    "respond appropriately if Yes is chosen and a value is entered into the text field" in {
      val mapping = mandatoryBooleanWithText("website","true", "No radio button selected", "blank value", "value not allowed", MAX_LENGTH)
      mapping.bind(Map("" -> "true", "website" -> "wwww.google.com")) mustBe Right(true)
    }

    "respond appropriately if user has not choosen any" in {
      val mapping = mandatoryBooleanWithText("website","true", "No radio button selected", "blank value", "value not allowed", MAX_LENGTH)
      mapping.bind(Map("website" -> "wwww.google.com"))
        .left.getOrElse(Nil).contains(FormError("", "No radio button selected")) mustBe true
    }

    "respond appropriately if Yes is chosen and no value entered into the text field" in {
      val mapping = mandatoryBooleanWithText("website","true", "No radio button selected", "blank value", "value not allowed", MAX_LENGTH)
      mapping.bind(Map("website" -> "", "" -> "true"))
        .left.getOrElse(Nil).contains(FormError("website", "blank value")) mustBe true
    }

    "respond appropriately if No is chosen and no value entered into the text field" in {
      val mapping = mandatoryBooleanWithText("website","true", "No radio button selected", "blank value", "value not allowed", MAX_LENGTH)
      mapping.bind(Map("website" -> "", "" -> "false")) mustBe Right(false)
    }

    "respond appropriately if No is chosen and a value entered into the text field" in {
      val mapping = mandatoryBooleanWithText("website","true", "No radio button selected", "blank value", "value not allowed", MAX_LENGTH)
      val gg = mapping.bind(Map("website" -> "bla", "" -> "false"))
        .left.getOrElse(Nil)
        gg.contains(FormError("website", "value not allowed")) mustBe true
    }

  }
}
