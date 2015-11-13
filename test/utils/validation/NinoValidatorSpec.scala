package utils.validation

import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import uk.gov.hmrc.play.test.WithFakeApplication
import utils.validation.NinoValidator._
import play.api.data.FormError

class NinoValidatorSpec extends PlaySpec with MockitoSugar  with OneServerPerSuite {

  "mandatoryNino" should {
    "respond appropriately if the nino format is correct" in {
      val mapping = mandatoryNino("blank message", "invalid length", "invalid value")
      mapping.bind(Map("" -> "AB123456C")) mustBe Right("AB123456C")
    }

    "respond appropriately if the nino format is correct with spaces" in {
      val mapping = mandatoryNino("blank message", "invalid length", "invalid value")
      mapping.bind(Map("" -> "AB 12 34 56 C")) mustBe Right("AB123456C")
    }

    "respond appropriately if the nino format is incorrect" in {
      mandatoryNino("blank", "length", "invalid").bind(Map("" -> ""))
        .left.getOrElse(Nil).contains(FormError("", "blank")) mustBe true

      mandatoryNino("blank", "length", "invalid").bind(Map("" -> "AB123456C45234"))
        .left.getOrElse(Nil).contains(FormError("", "length")) mustBe true

      mandatoryNino("blank", "length", "invalid").bind(Map("" -> "@&%a"))
        .left.getOrElse(Nil).contains(FormError("", "invalid")) mustBe true
    }

    "respond appropriately if unbound" in {
      val mapping = mandatoryNino("blank message", "invalid length", "invalid value")
      mapping.binder.unbind("", "AB123456C") mustBe Map("" -> "AB123456C")
    }

  }

}