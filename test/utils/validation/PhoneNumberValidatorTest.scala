package utils.validation

import org.scalatest.mock.MockitoSugar
import uk.gov.hmrc.play.test.UnitSpec
import utils.validation.PhoneNumberValidator._
import play.api.data.FormError

class PhoneNumberValidatorTest extends UnitSpec with MockitoSugar with amls.FakeAmlsApp {

  "mandatoryPhoneNumber" should {
    "respond appropriately for valid phone numbers " in {
      mandatoryPhoneNumber("blank","length","invalid").bind(Map("" -> "+44 0191 6678 899")) shouldBe Right("0044 0191 6678 899")
      mandatoryPhoneNumber("blank","length","invalid").bind(Map("" -> "(0191) 6678 899")) shouldBe Right("(0191) 6678 899")
      mandatoryPhoneNumber("blank","length","invalid").bind(Map("" -> "(0191) 6678 899#4456")) shouldBe Right("(0191) 6678 899#4456")
      mandatoryPhoneNumber("blank","length","invalid").bind(Map("" -> "(0191) 6678 899*6")) shouldBe Right("(0191) 6678 899*6")
      mandatoryPhoneNumber("blank","length","invalid").bind(Map("" -> "(0191) 6678-899")) shouldBe Right("(0191) 6678-899")
      mandatoryPhoneNumber("blank","length","invalid").bind(Map("" -> "01912224455")) shouldBe Right("01912224455")
      mandatoryPhoneNumber("blank","length","invalid").bind(Map("" -> "01912224455 ext 5544")) shouldBe Right("01912224455 EXT 5544")
    }

    "respond appropriately for invalid phone numbers " in {
      mandatoryPhoneNumber("blank","length","invalid").bind(Map("" -> ""))
        .left.getOrElse(Nil).contains(FormError("", "blank")) shouldBe true
      mandatoryPhoneNumber("blank","length","invalid").bind(Map("" -> "1111111111111111111111111111"))
        .left.getOrElse(Nil).contains(FormError("", "length")) shouldBe true
      mandatoryPhoneNumber("blank","length","invalid").bind(Map("" -> "$5gggF"))
        .left.getOrElse(Nil).contains(FormError("", "invalid")) shouldBe true
      mandatoryPhoneNumber("blank","length","invalid").bind(Map("" -> "019122244+55 ext 5544"))
        .left.getOrElse(Nil).contains(FormError("", "invalid")) shouldBe true
    }
  }

}