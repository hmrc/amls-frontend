package utils.validation

import org.scalatest.mock.MockitoSugar
import uk.gov.hmrc.play.test.UnitSpec
import utils.validation.NinoValidator._
import play.api.data.FormError

class NinoValidatorTest extends UnitSpec with MockitoSugar with amls.FakeAmlsApp {

  "mandatoryNino" should {
    "respond appropriately if the nino format is correct" in {
      val mapping = mandatoryNino("blank message", "invalid length", "invalid value")
      mapping.bind(Map("" -> "AB123456C")) shouldBe Right("AB123456C")
    }

    "respond appropriately if the nino format is correct with spaces" in {
      val mapping = mandatoryNino("blank message", "invalid length", "invalid value")
      mapping.bind(Map("" -> "AB 12 34 56 C")) shouldBe Right("AB123456C")
    }

    "respond appropriately if the nino format is incorrect" in {
      mandatoryNino("blank", "length", "invalid").bind(Map("" -> ""))
        .left.getOrElse(Nil).contains(FormError("", "blank")) shouldBe true

      mandatoryNino("blank", "length", "invalid").bind(Map("" -> "AB123456C45234"))
        .left.getOrElse(Nil).contains(FormError("", "length")) shouldBe true

      mandatoryNino("blank", "length", "invalid").bind(Map("" -> "@&%a"))
        .left.getOrElse(Nil).contains(FormError("", "invalid")) shouldBe true
    }
  }

}