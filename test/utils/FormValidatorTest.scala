package utils

import org.scalatest.mock.MockitoSugar
import uk.gov.hmrc.play.test.UnitSpec
import play.api.data.FormError
import FormValidator._

class FormValidatorTest extends UnitSpec with MockitoSugar with amls.FakeAmlsApp {
  "mandatoryNino" should {
    "respond appropriately if the nino format is correct" in {
      val mapping = FormValidator.mandatoryNino("blank message", "invalid length", "invalid value")
      mapping.bind(Map("" -> "AB123456C")) shouldBe Right("AB123456C")
    }

    "respond appropriately if the nino format is correct with spaces" in {
      val mapping = FormValidator.mandatoryNino("blank message", "invalid length", "invalid value")
      mapping.bind(Map("" -> "AB 12 34 56 C")) shouldBe Right("AB 12 34 56 C")
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

  "mandatoryEmail" should {
    "return the email if the email format is correct" in {
      mandatoryEmail("blank message", "invalid length", "invalid value")
        .bind(Map("" -> "aaaa@aaa.com")) shouldBe Right("aaaa@aaa.com")
    }

    "return correct form error if the email format is incorrect" in {
      mandatoryEmail("blank message", "invalid length", "invalid value").bind(Map("" -> ""))
        .left.getOrElse(Nil).contains(FormError("", "blank message")) shouldBe true

      mandatoryEmail("blank message", "invalid length", "invalid value").bind(Map("" -> "a" * 250))
        .left.getOrElse(Nil).contains(FormError("", "invalid length")) shouldBe true

      mandatoryEmail("blank message", "invalid length", "invalid value").bind(Map("" -> "@aaa.com.uk@467"))
        .left.getOrElse(Nil).contains(FormError("", "invalid value")) shouldBe true
    }
  }

  "address" should {
    "respond suitably to first two lines being blank" in {
      val first2Blank = Map(
        "addr1key"->"",
        "addr2key"->"",
        "postcodekey"->"CA3 9SD",
        "countrycodekey"->"GB"
      )
      address("addr2key","addr3key","addr4key","postcodekey", "countrycodekey",
        "first-two-blank","invalid-line","blank-postcode","invalid-postcode").bind(first2Blank)
            .left.getOrElse(Nil).contains(FormError("", "first-two-blank")) shouldBe true
    }

    "respond suitably to invalid lines" in {
      val invalidLine2 = Map(
        "addr1key"->"addr1",
        "addr2key"->"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
        "addr3key"->"addr3",
        "addr4key"->"addr4",
        "postcodekey"->"pcode",
        "countrycodekey"->"GB"
      )
      address("addr2key","addr3key","addr4key","postcodekey", "countrycodekey",
        "first-two-blank","invalid-line","blank-postcode","invalid-postcode").bind(invalidLine2)
        .left.getOrElse(Nil).contains(FormError("addr2key", "invalid-line")) shouldBe true
    }

    "respond suitably to blank postcode" in {
      val blankPostcode = Map(
        "addr1key"->"addr1",
        "addr2key"->"addr2",
        "addr3key"->"addr3",
        "addr4key"->"addr4",
        "postcodekey"->"",
        "countrycodekey"->"GB"
      )
      address("addr2key","addr3key","addr4key","postcodekey", "countrycodekey",
        "first-two-blank","invalid-line","blank-postcode","invalid-postcode").bind(blankPostcode)
        .left.getOrElse(Nil).contains(FormError("postcodekey", "blank-postcode")) shouldBe true
    }

    "respond suitably to invalid postcode" in {
      val invalidPostcode = Map(
        "addr1key"->"addr1",
        "addr2key"->"addr2",
        "addr3key"->"addr3",
        "addr4key"->"addr4",
        "postcodekey"->"CC!",
        "countrycodekey"->"GB"
      )
      address("addr2key","addr3key","addr4key","postcodekey", "countrycodekey",
        "first-two-blank","invalid-line","blank-postcode","invalid-postcode").bind(invalidPostcode)
        .left.getOrElse(Nil).contains(FormError("postcodekey", "invalid-postcode")) shouldBe true
    }
  }

  "currency" should {
    "return valid integer based values" in {

      optionalCurrency("error.currency").bind(Map("" -> "0")) shouldBe Right(Some(0))
      optionalCurrency("error.currency").bind(Map("" -> "   0   ")) shouldBe Right(Some(0))
      optionalCurrency("error.currency").bind(Map("" -> "1234")) shouldBe Right(Some(1234))
      optionalCurrency("error.currency").bind(Map("" -> "1,234")) shouldBe Right(Some(1234))
      optionalCurrency("error.currency").bind(Map("" -> "1,23,4")) shouldBe Right(Some(1234))
      optionalCurrency("error.currency").bind(Map("" -> "99999999999")) shouldBe Right(Some(BigDecimal("99999999999")))
    }

    "report an invalid money error" in {
      optionalCurrency("error.currency").bind(Map("" -> "£")) shouldBe Left(List(FormError("", "error.currency")))
      optionalCurrency("error.currency").bind(Map("" -> "a")) shouldBe Left(List(FormError("", "error.currency")))
      optionalCurrency("error.currency").bind(Map("" -> "1234.001")) shouldBe Left(List(FormError("", "error.currency")))
      optionalCurrency("error.currency").bind(Map("" -> "1.234.001")) shouldBe Left(List(FormError("", "error.currency")))
      optionalCurrency("error.currency").bind(Map("" -> "99999999999999999")) shouldBe Left(List(FormError("", "error.currency")))
      optionalCurrency("error.currency").bind(Map("" -> "0.00")) shouldBe Left(List(FormError("", "error.currency")))
      optionalCurrency("error.currency").bind(Map("" -> "1234.01")) shouldBe Left(List(FormError("", "error.currency")))
      optionalCurrency("error.currency").bind(Map("" -> "1234.1")) shouldBe Left(List(FormError("", "error.currency")))
      optionalCurrency("error.currency").bind(Map("" -> "-1234")) shouldBe Left(List(FormError("", "error.currency")))
    }

    "report correctly for blank value" in {
      optionalCurrency("error.currency").bind(Map("" -> "")) shouldBe Right(None)
    }
  }

  "mandatoryText" should {
    "return valid string if correct" in {
      mandatoryText("blank message", "invalid length", "validationMaxLengthFirstName").bind(Map("" -> "barry")) shouldBe Right("barry")
      mandatoryText("blank message", "invalid length", "validationMaxLengthFirstName").bind(Map("" -> "1234myname1234")) shouldBe Right("1234myname1234")
    }

    "report an invalid money error" in {
      mandatoryText("blank message", "invalid length", "validationMaxLengthFirstName").bind(Map("" -> "")) shouldBe
        Left(List(FormError("", "blank message")))
      mandatoryText("blank message", "invalid length", "validationMaxLengthFirstName").bind(Map("" -> "a" * 250)) shouldBe
        Left(List(FormError("", "invalid length")))
    }
  }

}
