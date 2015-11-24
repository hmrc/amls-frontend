package utils.validation

import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import uk.gov.hmrc.play.test.WithFakeApplication
import utils.validation.PhoneNumberValidator._
import play.api.data.FormError

class PhoneNumberValidatorSpec extends PlaySpec with MockitoSugar  with OneServerPerSuite {

  "mandatoryPhoneNumber" should {
    "respond appropriately for valid phone numbers " in {
      mandatoryPhoneNumber("blank","length","invalid").bind(Map("" -> "+44 0191 6678 899")) mustBe Right("0044 0191 6678 899")
      mandatoryPhoneNumber("blank","length","invalid").bind(Map("" -> "(0191) 6678 899")) mustBe Right("(0191) 6678 899")
      mandatoryPhoneNumber("blank","length","invalid").bind(Map("" -> "(0191) 6678 899#4456")) mustBe Right("(0191) 6678 899#4456")
      mandatoryPhoneNumber("blank","length","invalid").bind(Map("" -> "(0191) 6678 899*6")) mustBe Right("(0191) 6678 899*6")
      mandatoryPhoneNumber("blank","length","invalid").bind(Map("" -> "(0191) 6678-899")) mustBe Right("(0191) 6678-899")
      mandatoryPhoneNumber("blank","length","invalid").bind(Map("" -> "01912224455")) mustBe Right("01912224455")
      mandatoryPhoneNumber("blank","length","invalid").bind(Map("" -> "01912224455 ext 5544")) mustBe Right("01912224455 EXT 5544")
    }

    "respond appropriately for invalid phone numbers " in {
      mandatoryPhoneNumber("blank","length","invalid").bind(Map("" -> ""))
        .left.getOrElse(Nil).contains(FormError("", "blank")) mustBe true
      mandatoryPhoneNumber("blank","length","invalid").bind(Map("" -> "1111111111111111111111111111"))
        .left.getOrElse(Nil).contains(FormError("", "length")) mustBe true
      mandatoryPhoneNumber("blank","length","invalid").bind(Map("" -> "$5gggF"))
        .left.getOrElse(Nil).contains(FormError("", "invalid")) mustBe true
      mandatoryPhoneNumber("blank","length","invalid").bind(Map("" -> "019122244+55 ext 5544"))
        .left.getOrElse(Nil).contains(FormError("", "invalid")) mustBe true
      mandatoryPhoneNumber("blank","length","invalid").bind(Map())
        .left.getOrElse(Nil).contains(FormError("", "Nothing to validate")) mustBe true
    }

    "respond appropriately if unbound" in {
      mandatoryPhoneNumber("blank", "length", "invalid").binder.unbind("", "01912224455") mustBe Map("" -> "01912224455")
    }
  }

}