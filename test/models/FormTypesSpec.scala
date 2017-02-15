package models

import org.scalatestplus.play.PlaySpec
import jto.validation.forms.UrlFormEncoded
import jto.validation.{Invalid, Path, Valid}
import jto.validation.ValidationError

class FormTypesSpec extends PlaySpec with CharacterSets {

  import FormTypes._

  "successfully validate the first name" in {
    firstNameType.validate("John") must be(Valid("John"))
  }

  "fail validation if the first name is not provided" in {
    firstNameType.validate("") must be(Invalid(Seq(Path -> Seq(ValidationError("error.required.firstname")))))
  }

  "fail validation if the first name is more than 35 characters" in {
    firstNameType.validate("JohnJohnJohnJohnJohnJohnJohnJohnJohnJohn") must
      be(Invalid(Seq(Path -> Seq(ValidationError("error.invalid.length.firstname")))))
  }

  "successfully validate the middle name" in {
    middleNameType.validate("John") must be(Valid("John"))
  }

  "fail validation if the middle name is more than 35 characters" in {
    middleNameType.validate("EnvyEnvyEnvyEnvyEnvyEnvyEnvyEnvyEnvyEnvy") must
      be(Invalid(Seq(Path -> Seq(ValidationError("error.invalid.length.middlename")))))
  }

  "successfully validate the last name" in {
    lastNameType.validate("Doe") must be(Valid("Doe"))
  }

  "fail validation if the last name is not provided" in {
    lastNameType.validate("") must be(Invalid(Seq(Path -> Seq(ValidationError("error.required.lastname")))))
  }

  "fail validation if the last name is more than 35 characters" in {
    lastNameType.validate("DoeDoeDoeDoeDoeDoeDoeDoeDoeDoeDoeDoeDoe") must
      be(Invalid(Seq(Path -> Seq(ValidationError("error.invalid.length.lastname")))))
  }

  "validPostCodeType" must {

    "successfully validate" in {

      postcodeType.validate("177A") must
        be(Valid("177A"))
    }

    "fail to validate an empty string" in {

      postcodeType.validate("") must
        be(Invalid(Seq(
          Path -> Seq(ValidationError("error.required.postcode"))
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
          Path -> Seq(ValidationError("error.required.rp.phone"))
        )))
    }

    "fail to validate a string longer than 30" in {

      phoneNumberType.validate("1" * 31) must
        be(Invalid(Seq(
          Path -> Seq(ValidationError("error.max.length.rp.phone"))
        )))
    }
  }

  "emailType" must {

    val validEmailAddresses = Seq("test@test.com", "blah76@blah.com", "t@t", "name@abc-def.com", "test@abc.def.ghi.com", "t@t.com")
    val invalidEmailAddresses = Seq("test@-test.com", "foo@bar,com", "foo", "test@jhfd_jkj.com", "test@blah-.com", "test@-fdhkf-.com")

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
          Path -> Seq(ValidationError("error.invalid.tp.year"))
        )))
    }

    "fail to validate a string shorter than 4 digits" in {

      yearType.validate("1") must
        be(Invalid(Seq(
          Path -> Seq(ValidationError("error.invalid.tp.year"))
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

  "accountName" must {

    "be mandatory" in {
      accountNameType.validate("") must be(
        Invalid(Seq(Path -> Seq(ValidationError("error.bankdetails.accountname")))))
    }

    "accept all characters from the allowed set" in {
      println(">>>>>" + tradingNames.size)
      accountNameType.validate(tradingNames.toString) must be(Valid(tradingNames.toString))
    }

    "be not more than 171 characters" in {
      accountNameType.validate("This name is definitely longer than 10 characters." * 17) must be(
        Invalid(Seq(Path -> Seq(ValidationError("error.invalid.bankdetails.accountname"))))
      )
    }
  }

  "sortCode must" must {

    "validate when 6 digits are supplied without - " in {
      sortCodeType.validate("654321") must be(Valid("654321"))
    }

    "fail validation when more than 6 digits are supplied without - " in {
      sortCodeType.validate("87654321") must be(
      Invalid(Seq(Path -> Seq(ValidationError("error.invalid.bankdetails.sortcode")))))
    }

    "fail when 8 non digits are supplied with - " in {
      sortCodeType.validate("ab-cd-ef") must be(
        Invalid(Seq(Path -> Seq(ValidationError("error.invalid.bankdetails.sortcode")))))
    }

    "pass validation when dashes are used to seperate number groups" in {
      sortCodeType.validate("65-43-21") must be(Valid("654321"))
    }
    "pass validation when spaces are used to seperate number groups" in {
      sortCodeType.validate("65 43 21") must be(Valid("654321"))
    }

    "fail validation for sort code with any other pattern" in {
      sortCodeType.validate("8712341241431243124124654321") must be(
        Invalid(Seq(Path -> Seq(ValidationError("error.invalid.bankdetails.sortcode"))))
      )
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

  "UK Bank Account must successfully" must {

    "validate when 8 digits are supplied " in {
      ukBankAccountNumberType.validate("87654321") must be(Valid("87654321"))
    }

    "fail validation when less than 8 characters are supplied" in {
      ukBankAccountNumberType.validate("123456") must be(
        Invalid(Seq(Path -> Seq(ValidationError("error.invalid.bankdetails.accountnumber")))))
    }

    "fail validation when more than 8 characters are supplied" in {
      ukBankAccountNumberType.validate("1234567890") must be(
        Invalid(Seq(Path -> Seq(ValidationError("error.max.length.bankdetails.accountnumber")))))
    }
  }

  "For the Overseas Bank Account" must {

    "validate IBAN supplied " in {
      ibanType.validate("IBAN_4323268686686") must be(Valid("IBAN_4323268686686"))
    }

    "fail validation if IBAN is longer than the permissible length" in {
      ibanType.validate("12345678901234567890123456789012345678901234567890") must be(
        Invalid(Seq(Path -> Seq(ValidationError("error.max.length.bankdetails.iban")))))
    }

    "validate Non UK Account supplied " in {
      nonUKBankAccountNumberType.validate("IND22380310500093") must be(Valid("IND22380310500093"))
    }

    "fail validation if Non UK Account is longer than the permissible length" in {
      nonUKBankAccountNumberType.validate("12345678901234567890123456789012345678901234567890") must be(
        Invalid(Seq(Path -> Seq(ValidationError("error.max.length.bankdetails.account")))))
    }

  }

  "For the Declaration Add Persons 'name' fields" must {

    "fail validation if blank value is supplied for the name" in {
      declarationNameType.validate(" ") must be(Invalid(Seq(Path -> Seq(ValidationError("error.required")))))
    }

    "pass validation if name supplied is 255 characters" in {
      declarationNameType.validate("1" * maxNameTypeLength) must be(Valid("1" * maxNameTypeLength))
    }

    "validate other value length supplied" in {
      declarationNameType.validate("1" * (maxNameTypeLength + 1)) must be(
        Invalid(Seq(Path -> Seq(ValidationError("error.maxLength", maxNameTypeLength)))))
    }
  }

  "For the Declaration Add Persons page, business activity 'other' field" must {

    "pass validation if value length supplied is 255 characters" in {
      roleWithinBusinessOtherType.validate("1" * 255) must be(Valid("1" * 255))
    }

    "validate other value length supplied" in {
      roleWithinBusinessOtherType.validate("1" * 256) must be(
        Invalid(Seq(Path -> Seq(ValidationError("error.maxLength", maxRoleWithinBusinessOtherType)))))
    }

    "fail validation if business type field is not selected" in {
      roleWithinBusinessOtherType.validate("") must be(
        Invalid(Seq(Path -> Seq(ValidationError("error.required")))))
    }
  }

  "nino" must {

    "successfully validate" in {

      ninoType.validate("AB123456B") must
        be(Valid("AB123456B"))
    }

    "successfully validate disregarding case" in {
      ninoType.validate("ab123456c") mustBe Valid("AB123456C")
    }

    "successfully validate nino including spaces and dashes" in {
      ninoType.validate("AB 36 72- 73 B") mustBe Valid("AB367273B")
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

  "Uk passport number" must {
    "successfully validate numbers" in {
      ukPassportType.validate("123456789") mustBe Valid("123456789")
    }

    "fail when the passport number includes letters" in {
      ukPassportType.validate("123abc789") mustBe Invalid(
        Seq(Path -> Seq(ValidationError("error.invalid.uk.passport")))
      )
    }
  }

  "basicPunctuation140CharsPattern" must {
    "successfully validate a valid name" in {
      basicPunctuationPattern.validate("FirstName LastName") mustBe Valid("FirstName LastName")
    }
    "fail validation when given an invalid name" in {
      basicPunctuationPattern.validate("FirstName LastName{}") must be(Invalid(Seq(
        Path -> Seq(ValidationError("err.text.validation"))
      )))
    }
  }

}
