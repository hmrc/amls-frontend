package utils

import org.scalatest.mock.MockitoSugar
import uk.gov.hmrc.play.test.UnitSpec
import play.api.data.FormError
import utils.TestHelper._
import FormValidator._
import org.joda.time.LocalDate


class FormValidatorTest extends UnitSpec with MockitoSugar with amls.FakeAmlsApp {
  "isNotFutureDate" must {
    "return false if the date is later than current date" in {
      FormValidator.isNotFutureDate(LocalDate.now().plusDays(1)) shouldBe false
    }

    "return true if the date is equal to or earlier than the current date" in {
      FormValidator.isNotFutureDate(LocalDate.now()) shouldBe true
      FormValidator.isNotFutureDate(LocalDate.now().minusDays(1)) shouldBe true
    }
  }

  "validateNinoFormat" must {

    "return true if the nino format is correct" in {
      val formatter = FormValidator.mandatoryNino("blank message", "invalid length", "invalid value")

      formatter.bind(Map("" -> "AB123456C")) shouldBe Right("AB123456C")
    }
    "return true if the nino format is correct with spaces" in {
      val formatter = FormValidator.mandatoryNino("blank message", "invalid length", "invalid value")

      formatter.bind(Map("" -> "AB 12 34 56 C")) shouldBe Right("AB 12 34 56 C")
    }
    "return false if the nino format is correct" in {
      val formatter = FormValidator.mandatoryNino("blank message", "invalid length", "invalid value")

      isErrorMessageKeyEqual(formatter.bind(Map("" -> "")), "blank message") shouldBe true
      isErrorMessageKeyEqual(formatter.bind(Map("" -> "AB123456C45234")), "invalid length") shouldBe true
      isErrorMessageKeyEqual(formatter.bind(Map("" -> "@&%a")), "invalid value") shouldBe true
    }
  }

  "mandatoryPhoneNumberFormatter" should {
    "Return expected mapping validation for valid phone numbers" in {

      val formatter = mandatoryPhoneNumberFormatter("blank message", "invalid length", "invalid value")

      formatter.bind("a", Map("a" -> "+44 0191 6678 899"))  shouldBe Right("0044 0191 6678 899")
      formatter.bind("a", Map("a" -> "(0191) 6678 899")) shouldBe Right("(0191) 6678 899")

      formatter.bind("a", Map("a" -> "(0191) 6678 899#4456")) shouldBe Right("(0191) 6678 899#4456")
      formatter.bind("a", Map("a" -> "(0191) 6678 899*6")) shouldBe Right("(0191) 6678 899*6")
      formatter.bind("a", Map("a" -> "(0191) 6678-899")) shouldBe Right("(0191) 6678-899")
      formatter.bind("a", Map("a" -> "01912224455")) shouldBe Right("01912224455")
      formatter.bind("a", Map("a" -> "01912224455 ext 5544")) shouldBe Right("01912224455 EXT 5544")
    }
    "Return expected mapping validation for invalid phone numbers" in {

      val formatter = mandatoryPhoneNumberFormatter("blank message", "invalid length", "invalid value")

      formatter.bind("a", Map("a" -> ""))  shouldBe Left(Seq(FormError("a", "blank message")))
      formatter.bind("a", Map("a" -> "1111111111111111111111111111"))  shouldBe Left(Seq(FormError("a", "invalid length")))
      formatter.bind("a", Map("a" -> "$5gggF"))  shouldBe Left(Seq(FormError("a", "invalid value")))
      formatter.bind("a", Map("a" -> "019122244+55 ext 5544")) shouldBe Left(Seq(FormError("a", "invalid value")))
    }
  }

  "amlsMandatoryEmailWithDomain" must {
    "return the email if the email format is correct" in {
      val formatter = FormValidator.mandatoryEmailWithDomain(
        "blank message", "invalid length", "invalid value")
      formatter.bind(Map("" -> "aaaa@aaa.com")) shouldBe Right("aaaa@aaa.com")
    }

    "return correct form error if the email format is incorrect" in {
      val formatter = FormValidator.mandatoryEmailWithDomain(
        "blank message", "invalid length", "invalid value")

      isErrorMessageKeyEqual(formatter.bind(Map("" -> "")), "blank message") shouldBe true
      isErrorMessageKeyEqual(formatter.bind(Map("" -> "a" * 250)), "invalid length") shouldBe true
      isErrorMessageKeyEqual(formatter.bind(Map("" -> "@aaa.com.uk@467")), "invalid value") shouldBe true
    }
  }

  "address" should {
    val allBlank = Map(
      "addr1key"->"",
      "addr2key"->"",
      "addr3key"->"",
      "addr4key"->"",
      "postcodekey"->"",
      "countrycodekey"->""
    )

    val first2Blank = Map(
      "addr1key"->"",
      "addr2key"->"",
      "postcodekey"->"CA3 9SD",
      "countrycodekey"->"GB"
    )

    val invalidLine2 = Map(
      "addr1key"->"addr1",
      "addr2key"->"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
      "addr3key"->"addr3",
      "addr4key"->"addr4",
      "postcodekey"->"pcode",
      "countrycodekey"->"GB"
    )

    val blankPostcode = Map(
      "addr1key"->"addr1",
      "addr2key"->"addr2",
      "addr3key"->"addr3",
      "addr4key"->"addr4",
      "postcodekey"->"",
      "countrycodekey"->"GB"
    )

    val invalidPostcode = Map(
      "addr1key"->"addr1",
      "addr2key"->"addr2",
      "addr3key"->"addr3",
      "addr4key"->"addr4",
      "postcodekey"->"CC!",
      "countrycodekey"->"GB"
    )

    val formatter = addressFormatter("addr2key","addr3key","addr4key","postcodekey", "countrycodekey",
      "all-lines-blank","first-two-blank","invalid-line","blank-postcode","invalid-postcode", "blankcountrycode")

    "Return a formatter which responds suitably to all lines being blank" in {
      formatter.bind("", allBlank).left.getOrElse(Nil).contains(FormError("", "all-lines-blank")) shouldBe true
    }

    "Return a formatter which responds suitably to first two lines being blank" in {
      formatter.bind("", first2Blank).left.getOrElse(Nil).contains(FormError("", "all-lines-blank")) shouldBe true
    }
    "Return a formatter which responds suitably to invalid lines" in {
      formatter.bind("", invalidLine2).left.getOrElse(Nil).contains(FormError("addr2key", "invalid-line")) shouldBe true
    }
    "Return a formatter which responds suitably to blank postcode" in {
      formatter.bind("", blankPostcode).left.getOrElse(Nil).contains(FormError("postcodekey", "blank-postcode")) shouldBe true
    }
    "Return a formatter which responds suitably to invalid postcode" in {
      formatter.bind("", invalidPostcode).left.getOrElse(Nil).contains(FormError("postcodekey", "invalid-postcode")) shouldBe true
    }
  }

}