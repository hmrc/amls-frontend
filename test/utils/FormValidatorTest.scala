package utils

import org.scalatest.mock.MockitoSugar
import uk.gov.hmrc.play.test.UnitSpec
import play.api.data.FormError
import utils.TestHelper._



class FormValidatorTest extends UnitSpec with MockitoSugar with amls.FakeAmlsApp {
//  "isNotFutureDate" must {
//    "return false the date is later than current date" in {
//      val testDate = LocalDate.now().plusDays(1)
//      val result = FormValidator.isNotFutureDate(testDate)
//      result should be(false)
//    }
//
//    "return true the date is equal to current date" in {
//      val testDate = LocalDate.now()
//      val result = FormValidator.isNotFutureDate(testDate)
//      result should be(true)
//    }
//
//    "return true the date is earlier than current date" in {
//      val testDate = LocalDate.now().minusDays(1)
//      val result = FormValidator.isNotFutureDate(testDate)
//      result should be(true)
//    }
//  }
//
//  "validateCountryCode" must {
//    "reject an invalid country code" in {
//      FormValidator.validateCountryCode("UK") should be(false)
//    }
//    "accept a valid country code" in {
//      FormValidator.validateCountryCode("GB") should be(true)
//    }
//  }
//
  "validateNinoFormat" must {
    "return true if the nino format is correct" in {
      val testNino = "AB123456C"
      val result = FormValidator.validateNinoFormat(testNino)
      result should be(true)
    }

    "return true if the nino format is correct with spaces" in {
      val testNino = "AB 12 34 56 C"
      val result = FormValidator.validateNinoFormat(testNino)
      result should be(true)
    }

    "return false if the nino format is not correct" in {
      val testNino = "123456789"
      val result = FormValidator.validateNinoFormat(testNino)
      result should be(false)
    }
  }

//  "existsInKeys" must {
//    "return true if valid list map key" in {
//      val result = FormValidator.existsInKeys(TestHelper.MaritalStatusSingle, FieldMappings.maritalStatusMap)
//      result should be(true)
//    }
//
//    "return false if invalid list map key" in {
//      val result = FormValidator.existsInKeys("7", FieldMappings.maritalStatusMap)
//      result should be(false)
//    }
//  }
//
//  "validateRole" must {
//    "return true if valid role" in {
//      val result = FormValidator.validateApplicantRole(TestHelper.RoleLeadExecutor)
//      result should be(true)
//    }
//    "return false if invalid role" in {
//      val result = FormValidator.validateApplicantRole(TestHelper.RoleDonee)
//      result should be(false)
//    }
//  }
//
//  "currency" should {
//    "Return valid integer based values" in {
//      optionalCurrency.bind(Map("" -> "0")) shouldBe Right(Some(0))
//      optionalCurrency.bind(Map("" -> "0.00")) shouldBe Right(Some(0))
//      optionalCurrency.bind(Map("" -> "   0   ")) shouldBe Right(Some(0))
//      optionalCurrency.bind(Map("" -> "1234")) shouldBe Right(Some(1234))
//      optionalCurrency.bind(Map("" -> "1234.01")) shouldBe Right(Some(1234.01))
//      optionalCurrency.bind(Map("" -> "1234.01")) shouldBe Right(Some(1234.01))
//      optionalCurrency.bind(Map("" -> "1,234")) shouldBe Right(Some(1234))
//      optionalCurrency.bind(Map("" -> "1,234.01")) shouldBe Right(Some(1234.01))
//      optionalCurrency.bind(Map("" -> "1,23,4.01")) shouldBe Right(Some(1234.01))
//      optionalCurrency.bind(Map("" -> "1234.1")) shouldBe Right(Some(1234.1))
//      optionalCurrency.bind(Map("" -> "-1234.1")) shouldBe Right(Some(1234.1))
//      optionalCurrency.bind(Map("" -> "9999999999.00")) shouldBe Right(Some(9999999999.00))
//    }
//    "Report an invalid money error" in {
//      optionalCurrency.bind(Map("" -> "£")) shouldBe Left(List(FormError("", "error.currency")))
//      optionalCurrency.bind(Map("" -> "a")) shouldBe Left(List(FormError("", "error.currency")))
//      optionalCurrency.bind(Map("" -> "1234.001")) shouldBe Left(List(FormError("", "error.currency")))
//      optionalCurrency.bind(Map("" -> "1.234.001")) shouldBe Left(List(FormError("", "error.currency")))
//      optionalCurrency.bind(Map("" -> "99999999999.00")) shouldBe Left(List(FormError("", "error.currency")))
//    }
//
//    "Report correctly for invalid numeric value" in {
//      optionalCurrency.bind(Map("" -> "")) shouldBe Right(None)
//    }
//
//  }
//
//  "mandatoryPhoneNumberFormatter" should {
//    "Return expected mapping validation for various inputs, valid and invalid" in {
//      import play.api.data.FormError
//
//      val formatter = mandatoryPhoneNumberFormatter("blank message", "invalid length", "invalid value")
//
//      formatter.bind("a", Map("a" -> ""))  shouldBe Left(Seq(FormError("a", "blank message")))
//      formatter.bind("a", Map("a" -> "1111111111111111111111111111"))  shouldBe Left(Seq(FormError("a", "invalid length")))
//      formatter.bind("a", Map("a" -> "$5gggF"))  shouldBe Left(Seq(FormError("a", "invalid value")))
//      formatter.bind("a", Map("a" -> "+44 0191 6678 899"))  shouldBe Right("0044 0191 6678 899")
//      formatter.bind("a", Map("a" -> "(0191) 6678 899")) shouldBe Right("(0191) 6678 899")
//
//      formatter.bind("a", Map("a" -> "(0191) 6678 899#4456")) shouldBe Right("(0191) 6678 899#4456")
//      formatter.bind("a", Map("a" -> "(0191) 6678 899*6")) shouldBe Right("(0191) 6678 899*6")
//      formatter.bind("a", Map("a" -> "(0191) 6678-899")) shouldBe Right("(0191) 6678-899")
//      formatter.bind("a", Map("a" -> "01912224455")) shouldBe Right("01912224455")
//      formatter.bind("a", Map("a" -> "01912224455 ext 5544")) shouldBe Right("01912224455 EXT 5544")
//      formatter.bind("a", Map("a" -> "019122244+55 ext 5544")) shouldBe Left(Seq(FormError("a", "invalid value")))
//
//    }
//  }
//
//  "ihtAddress" should {
//
//
//    val allBlank = Map(
//      "addr1key"->"",
//      "addr2key"->"",
//      "addr3key"->"",
//      "addr4key"->"",
//      "postcodekey"->"",
//      "countrycodekey"->""
//    )
//
//    val first2Blank = Map(
//      "addr1key"->"",
//      "addr2key"->"",
//      "postcodekey"->"CA3 9SD",
//      "countrycodekey"->"GB"
//    )
//
//    val invalidLine2 = Map(
//      "addr1key"->"addr1",
//      "addr2key"->"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
//      "addr3key"->"addr3",
//      "addr4key"->"addr4",
//      "postcodekey"->"pcode",
//      "countrycodekey"->"GB"
//    )
//
//    val blankPostcode = Map(
//      "addr1key"->"addr1",
//      "addr2key"->"addr2",
//      "addr3key"->"addr3",
//      "addr4key"->"addr4",
//      "postcodekey"->"",
//      "countrycodekey"->"GB"
//    )
//
//    val invalidPostcode = Map(
//      "addr1key"->"addr1",
//      "addr2key"->"addr2",
//      "addr3key"->"addr3",
//      "addr4key"->"addr4",
//      "postcodekey"->"CC!",
//      "countrycodekey"->"GB"
//    )
//
//    val allowedBlankPostcode = Map(
//      "addr1key"->"addr1",
//      "addr2key"->"addr2",
//      "addr3key"->"addr3",
//      "addr4key"->"addr4",
//      "postcodekey"->"",
//      "countrycodekey"->"IL"
//    )
//
//    val formatter = ihtAddress("addr2key","addr3key","addr4key","postcodekey", "countrycodekey",
//      "all-lines-blank","first-two-blank","invalid-line","blank-postcode","invalid-postcode", "blankcountrycode")
//
//    "Return a formatter which responds suitably to all lines being blank" in {
//      formatter.bind("", allBlank).left.get.contains(FormError("", "all-lines-blank")) shouldBe true
//    }
//
//    "Return a formatter which responds suitably to first two lines being blank" in {
//      formatter.bind("", first2Blank).left.get.contains(FormError("", "all-lines-blank")) shouldBe true
//    }
//    "Return a formatter which responds suitably to invalid lines" in {
//      formatter.bind("", invalidLine2).left.get.contains(FormError("addr2key", "invalid-line")) shouldBe true
//    }
//    "Return a formatter which responds suitably to blank postcode" in {
//      formatter.bind("", blankPostcode).left.get.contains(FormError("postcodekey", "blank-postcode")) shouldBe true
//    }
//    "Return a formatter which responds suitably to invalid postcode" in {
//      formatter.bind("", invalidPostcode).left.get.contains(FormError("postcodekey", "invalid-postcode")) shouldBe true
//    }
//  }
//
//
//
//  "ihtRadio" should {
//    val formatter = ihtRadio("no-selection", ListMap("a"->"a"))
//    "Return a formatter which responds suitably to no item being selected" in {
//      formatter.bind("radiokey", Map( "option1"->"option1" ))
//        .left.get.contains(FormError("radiokey", "no-selection")) shouldBe true
//    }
//
//  }

  "amlsMandatoryEmailWithDomain" must {
    "return the email if the email format is correct" in {
      val formatter = FormValidator.amlsMandatoryEmailWithDomain(
        "blank message", "invalid length", "invalid value")
      formatter.bind(Map("" -> "aaaa@aaa.com")) shouldBe Right("aaaa@aaa.com")
    }

    "return correct form error if the email format is incorrect" in {
      val formatter = FormValidator.amlsMandatoryEmailWithDomain(
        "blank message", "invalid length", "invalid value")

      isErrorMessageKeyEqual(formatter.bind(Map("" -> "")), "blank message") shouldBe true
      isErrorMessageKeyEqual(formatter.bind(Map("" -> "a" * 250)), "invalid length") shouldBe true
      isErrorMessageKeyEqual(formatter.bind(Map("" -> "@aaa.com.uk@467")), "invalid value") shouldBe true
    }
  }



}