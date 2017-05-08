package models

import controllers.responsiblepeople.NinoUtil
import org.scalatestplus.play.PlaySpec
import jto.validation.forms.UrlFormEncoded
import jto.validation.{Invalid, Path, Valid}
import jto.validation.ValidationError

class FormTypesSpec extends PlaySpec with CharacterSets with NinoUtil {

  import FormTypes._

  "successfully validate the middle name" in {
    middleNameType.validate("middleName") must be(Valid("middleName"))
  }

  "fail validation if the middle name is more than 35 characters" in {
    middleNameType.validate("middleNamemiddleNamemiddleNamemiddleNamemiddleNamemiddleName") must
      be(Invalid(Seq(Path -> Seq(ValidationError("error.invalid.length.middlename")))))
  }

  "validPostCodeType" must {

    "successfully validate" in {

      postcodeType.validate("AA11 1AA") must
        be(Valid("AA11 1AA"))
    }

    "fail to validate given an invalid postcode" in {

      postcodeType.validate("XXXXX") must
        be(Invalid(Seq(
          Path -> Seq(ValidationError("error.invalid.postcode"))
        )))
    }

    "fail to validate an empty string" in {

      postcodeType.validate("") must
        be(Invalid(Seq(
          Path -> Seq(ValidationError("error.invalid.postcode"))
        )))
    }

    "fail to validate a string longer than 10" in {

      postcodeType.validate("a" * 11) must
        be(Invalid(Seq(
          Path -> Seq(ValidationError("error.invalid.postcode"))
        )))
    }
  }

  "vrnType" must {

    "successfully validate" in {

      vrnType.validate("123456789") must
        be(Valid("123456789"))
    }

    "fail to validate an empty string" in {

      vrnType.validate("") must
        be(Invalid(Seq(
          Path -> Seq(ValidationError("error.required.vat.number"))
        )))
    }

    "fail to validate a string longer than 9" in {

      vrnType.validate("1" * 10) must
        be(Invalid(Seq(
          Path -> Seq(ValidationError("error.invalid.vat.number"))
        )))
    }
  }

  "phoneNumberType" must {
    "successfully validate" in {

      phoneNumberType.validate("1234567890") must
        be(Valid("1234567890"))
    }

    "fail to validate an empty string" in {

      phoneNumberType.validate("") must
        be(Invalid(Seq(
          Path -> Seq(ValidationError("error.required.phone.number"))
        )))
    }

    "fail to validate a string longer than 30" in {

      phoneNumberType.validate("1" * 31) must
        be(Invalid(Seq(
          Path -> Seq(ValidationError("error.max.length.phone"))
        )))
    }
  }

  "generic common name rule" must {

    "pass with a normal name" in {
      genericNameRule("required error", "length error").validate("firstName lastName") must be(Valid("firstName lastName"))
    }

    "fail with a name with invalid characters" in {
      genericNameRule("required error", "length error").validate("*($£OKFDF") must be(
        Invalid(Seq(Path -> Seq(ValidationError("error.invalid.common_name.validation"))))
      )
    }

    "fail with a name that's too long" in {
      genericNameRule("required error", "length error").validate("d" * 36) must be(
        Invalid(Seq(Path -> Seq(ValidationError("length error"))))
      )
    }

  }

  "emailType" must {

    val validEmailAddresses = Seq(
      "test@test.com", "blah76@blah.com", "t@t", "name@abc-def.com", "test@abc.def.ghi.com", "t@t.com"
    )
    val invalidEmailAddresses = Seq(
      "test@-test.com", "foo@bar,com", "foo", "test@jhfd_jkj.com", "test@blah-.com", "test@-fdhkf-.com", "email@addrese.com;secondemail@address.com"
    )

    validEmailAddresses.foreach { testData =>
      s"succesfully validate $testData" in {
        emailType.validate(testData) must
          be(Valid(testData))
      }
    }

    invalidEmailAddresses.foreach { testData =>
      s"fail to validate $testData" in {
        emailType.validate(testData) must be(Invalid(Seq(
          Path -> Seq(ValidationError("error.invalid.rp.email"))
        )))
      }
    }

    "fail to validate an empty string" in {
      emailType.validate("") must
        equal(Invalid(Seq(
          Path -> Seq(ValidationError("error.required.rp.email"))
        )))
    }

    "fail to validate a string longer than 100" in {

      emailType.validate("1" * 101) must
        be(Invalid(Seq(
          Path -> Seq(ValidationError("error.max.length.rp.email"))
        )))
    }
  }

  "yearType" must {
    "successfully validate" in {

      yearType.validate("1934") must
        be(Valid("1934"))
    }

    "fail to validate an empty string" in {

      yearType.validate("") must
        be(Invalid(Seq(
          Path -> Seq(ValidationError("error.required.tp.year"))
        )))
    }

    "fail to validate a string longer than 4 digits" in {

      yearType.validate("19999") must
        be(Invalid(Seq(
          Path -> Seq(ValidationError("error.invalid.year"))
        )))
    }

    "fail to validate a string shorter than 4 digits" in {

      yearType.validate("1") must
        be(Invalid(Seq(
          Path -> Seq(ValidationError("error.invalid.year"))
        )))
    }

    "fail to validate year before 1990" in {

      yearTypePost1900.validate("1899") must
        be(Invalid(Seq(
          Path -> Seq(ValidationError("error.invalid.year.post1900"))
        )))
    }
  }

  "localDateRule" must {

    import org.joda.time.LocalDate

    val data = Map(
      "day" -> Seq("24"),
      "month" -> Seq("2"),
      "year" -> Seq("1990")
    )

    val model = new LocalDate(1990, 2, 24)

    "successfully validate" in {
      localDateRule.validate(data) must
        be(Valid(model))
    }

    "fail to validate an invalid month" in {
      localDateRule.validate(Map(
        "day" -> Seq("24"),
        "month" -> Seq("13"),
        "year" -> Seq("1990")
      )) must be(Invalid(Seq(
        Path -> Seq(ValidationError("error.expected.jodadate.format", "yyyy-MM-dd"))
      )))
    }

    "fail to validate an invalid day" in {
      localDateRule.validate(Map(
        "day" -> Seq("45"),
        "month" -> Seq("11"),
        "year" -> Seq("1990")
      )) must be(Invalid(Seq(
        Path -> Seq(ValidationError("error.expected.jodadate.format", "yyyy-MM-dd"))
      )))
    }

    "fail to validate a date when fewer than 4 digits are provided for year" in {
      localDateRule.validate(Map(
        "day" -> Seq("24"),
        "month" -> Seq("11"),
        "year" -> Seq("16")
      )) must be(Invalid(Seq(
        Path -> Seq(ValidationError("error.expected.jodadate.format", "yyyy-MM-dd"))
      )))
    }

    "fail to validate a date when more than 4 digits are provided for year" in {
      localDateRule.validate(Map(
        "day" -> Seq("24"),
        "month" -> Seq("11"),
        "year" -> Seq("20166")
      )) must be(Invalid(Seq(
        Path -> Seq(ValidationError("error.expected.jodadate.format", "yyyy-MM-dd"))
      )))
    }

    "fail to validate missing fields" in {
      localDateRule.validate(Map.empty) must
        be(Invalid(Seq(
          Path -> Seq(ValidationError("error.expected.jodadate.format", "yyyy-MM-dd"))
        )))
    }

    "fail to validate trading premises removal when end date is before start date" in {

      val startDate = new LocalDate(1999, 1, 1)

      val form: UrlFormEncoded = Map(
        "premisesStartDate" -> Seq(startDate.toString("yyyy-MM-dd")),
        "endDate.day" -> Seq("1"),
        "endDate.month" -> Seq("1"),
        "endDate.year" -> Seq("1998")
      )

      val result = FormTypes.premisesEndDateRule.validate(form)

      result mustBe Invalid(Seq(Path \ "endDate" -> Seq(ValidationError("error.expected.tp.date.after.start", startDate.toString("dd-MM-yyyy")))))

    }


    "successfully validate a form with 2 dates which should be after one another" in {

      val startDate = new LocalDate(2001, 1, 1)

      val form: UrlFormEncoded = Map(
        "positionStartDate" -> Seq(startDate.toString("yyyy-MM-dd")),
        "userName" -> Seq("User 1"),
        "endDate.day" -> Seq("1"),
        "endDate.month" -> Seq("1"),
        "endDate.year" -> Seq("2000")
      )

      val result = FormTypes.peopleEndDateRule.validate(form)

      result mustBe Invalid(Seq((Path \ "endDate") -> Seq(ValidationError("error.expected.rp.date.after.start", "User 1", startDate.toString("dd-MM-yyyy")))))

    }
  }

  "localDateFutureRule" must {
    val data = Map(
      "day" -> Seq("24"),
      "month" -> Seq("2"),
      "year" -> Seq("1990")
    )

    "fail to validate a future date" in {
      localDateFutureRule.validate(Map(
        "day" -> Seq("1"),
        "month" -> Seq("1"),
        "year" -> Seq("2020")
      )) must be(Invalid(Seq(
        Path -> Seq(ValidationError("error.future.date"))
      )))
    }

    "fail to validate an invalid month" in {
      localDateFutureRule.validate(Map(
        "day" -> Seq("24"),
        "month" -> Seq("13"),
        "year" -> Seq("1990")
      )) must be(Invalid(Seq(
        Path -> Seq(ValidationError("error.expected.jodadate.format", "yyyy-MM-dd"))
      )))
    }

    "fail to validate an invalid day" in {
      localDateFutureRule.validate(Map(
        "day" -> Seq("45"),
        "month" -> Seq("11"),
        "year" -> Seq("1990")
      )) must be(Invalid(Seq(
        Path -> Seq(ValidationError("error.expected.jodadate.format", "yyyy-MM-dd"))
      )))
    }

    "fail to validate a date when fewer than 4 digits are provided for year" in {
      localDateFutureRule.validate(Map(
        "day" -> Seq("24"),
        "month" -> Seq("11"),
        "year" -> Seq("16")
      )) must be(Invalid(Seq(
        Path -> Seq(ValidationError("error.expected.jodadate.format", "yyyy-MM-dd"))
      )))
    }

    "fail to validate a date when more than 4 digits are provided for year" in {
      localDateFutureRule.validate(Map(
        "day" -> Seq("24"),
        "month" -> Seq("11"),
        "year" -> Seq("10166")
      )) must be(Invalid(Seq(
        Path -> Seq(ValidationError("error.expected.jodadate.format", "yyyy-MM-dd"))
      )))
    }

    "fail to validate missing fields" in {
      localDateFutureRule.validate(Map.empty) must
        be(Invalid(Seq(
          Path -> Seq(ValidationError("error.expected.jodadate.format", "yyyy-MM-dd"))
        )))
    }

  }

  "localDateWrite" must {

    import org.joda.time.LocalDate

    val data = Map(
      "day" -> Seq("24"),
      "month" -> Seq("2"),
      "year" -> Seq("1990")
    )

    val model = new LocalDate(1990, 2, 24)

    "successfully serialise" in {
      localDateWrite.writes(model) must be(data)
    }
  }

  "removeCharacterRule" must {
    "strip the character from the incoming string" in {
      val inputStr = "=AAAA==BBBB==CCCC=="
      removeCharacterRule('=').validate(inputStr)  must be (Valid("AAAABBBBCCCC"))
    }
  }

  "removeDashRule" must {
    "strip dashes from the incoming string" in {
      val inputStr = "-AAAA---BBBB--CCCC---"
      removeDashRule.validate(inputStr)  must be (Valid("AAAABBBBCCCC"))
    }
  }

  "removeSpacesRule" must {
    "strip space s from the incoming string" in {
      val inputStr = " AAAA   BBBB CCCC  "
      removeSpacesRule.validate(inputStr)  must be (Valid("AAAABBBBCCCC"))    }
  }

  "For the Declaration Add Persons 'name' fields" must {

    "fail validation if blank value is supplied for the name" in {
      declarationNameType.validate(" ") must be(Invalid(Seq(Path -> Seq(ValidationError("error.required")))))
    }

    "pass validation if name supplied is at, but no more than max length" in {
      declarationNameType.validate("a" * maxNameTypeLength) must be(Valid("a" * maxNameTypeLength))
    }

    "validate other value length supplied" in {
      declarationNameType.validate("a" * (maxNameTypeLength + 1)) must be(
        Invalid(Seq(Path -> Seq(ValidationError("error.maxLength", maxNameTypeLength)))))
    }
  }

  "nino" must {

    "successfully validate" in {

      val nino = nextNino

      ninoType.validate(nino) must
        be(Valid(nino))
    }

    "successfully validate disregarding case" in {
      val nino = nextNino
      ninoType.validate(nino.toLowerCase) mustBe Valid(nino)
    }

    "successfully validate nino including spaces and dashes" in {
      val testNino = nextNino
      val spacedNino = testNino.grouped(2).mkString(" ")
      val withDashes = spacedNino.substring(0, 8) + "-" + spacedNino.substring(8, spacedNino.length) // ## ## ##- ## #
      ninoType.validate(withDashes) mustBe Valid(testNino)
    }

    "fail to validate an empty string" in {

      ninoType.validate("") must
        equal(Invalid(Seq(
          Path -> Seq(ValidationError("error.required.nino"))
        )))
    }

    "fail validation on exceeding maxlength" in {

      ninoType.validate("1" * 10) must
        be(Invalid(Seq(
          Path -> Seq(ValidationError("error.invalid.nino"))
        )))
    }

    "fail to validate invalid data" in {

      ninoType.validate("1@@@@@") must
        be(Invalid(Seq(
          Path -> Seq(ValidationError("error.invalid.nino"))
        )))
    }
  }



  "basicPunctuation140CharsPattern" must {

    "successfully validate a valid name" in {
      basicPunctuationPattern().validate("FirstName LastName^") mustBe Valid("FirstName LastName^")
    }

    "successfully validate a valid name with carriage return" in {
      basicPunctuationPattern().validate("FirstName LastName \n  sdsfsdfsdf \n  gdafsdga") mustBe Valid("FirstName LastName \n  sdsfsdfsdf \n  gdafsdga")
    }

    "successfully validate a valid name with special chars" in {
      basicPunctuationPattern().validate("& - +=1234567890ABCDEZMN.,_*%£:;~@") mustBe Valid("& - +=1234567890ABCDEZMN.,_*%£:;~@")
    }

    "fail validation when given an invalid name" in {
      basicPunctuationPattern().validate("FirstName LastName{}") must be(Invalid(Seq(
        Path -> Seq(ValidationError("err.text.validation"))
      )))
    }

    "fail validation when given an invalid name with <>" in {
      basicPunctuationPattern().validate("FirstName LastName<>") must be(Invalid(Seq(
        Path -> Seq(ValidationError("err.text.validation"))
      )))
    }
  }

  "basicTelephoneNumberType1" must {

    "successfully validate a telephone number" in {
      phoneNumberType.validate("(541) 754-3010") must be(Valid("(541) 754-3010"))
      phoneNumberType.validate("+1-541-754-3010") must be(Valid("+1-541-754-3010"))
      phoneNumberType.validate("1-541-754-3010") must be(Valid("1-541-754-3010"))
      phoneNumberType.validate("001-541-754-3010") must be(Valid("001-541-754-3010"))
      phoneNumberType.validate("+44 22 2222 2222") must be(Valid("+44 22 2222 2222"))
    }

    "fail validation on invalid number" in {
      phoneNumberType.validate("(541) *754-3010") must be(Invalid(Seq(
        Path -> Seq(ValidationError("err.invalid.phone.number"))
      )))

      phoneNumberType.validate("AAAAAA_&^G") must be(Invalid(Seq(
        Path -> Seq(ValidationError("err.invalid.phone.number"))
      )))
    }
  }

  "basicAddressTypeValidationPattern8" must {

    "successfully validate a address lines" in {
      validateAddress.validate(symbols8) must be(Valid(symbols8))
      validateAddress.validate("aa (1aa),aa") must be(Valid("aa (1aa),aa"))
      validateAddress.validate("aa.b!") must be(Valid("aa.b!"))

    }

    "fail validation" when {
      "given too many characters" in {
        validateAddress.validate("a" * 36) must be(Invalid(Seq(
          Path -> Seq(ValidationError("error.max.length.address.line"))
        )))
      }

      "given invalid characters" in {
        validateAddress.validate("aa & aa") must be(Invalid(Seq(
          Path -> Seq(ValidationError("err.text.validation"))
        )))

        validateAddress.validate("%%") must be(Invalid(Seq(
          Path -> Seq(ValidationError("err.text.validation"))
        )))

        validateAddress.validate("$$$$$$$") must be(Invalid(Seq(
          Path -> Seq(ValidationError("err.text.validation"))
        )))

        validateAddress.validate("#######") must be(Invalid(Seq(
          Path -> Seq(ValidationError("err.text.validation"))
        )))
        validateAddress.validate("******") must be(Invalid(Seq(
          Path -> Seq(ValidationError("err.text.validation"))
        )))
      }
    }
  }

}
