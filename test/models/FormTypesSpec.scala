package models

import org.scalatestplus.play.PlaySpec
import org.specs2.mock.mockito.MockitoMatchers
import play.api.data.mapping.{Failure, Path, Success}
import play.api.data.validation.ValidationError

class FormTypesSpec extends PlaySpec with MockitoMatchers {

  import FormTypes._

  "indivNameType" must {

    "successfully validate" in {

      indivNameType.validate("foobar") must
        be(Success("foobar"))
    }

    "fail to validate an empty string" in {

      indivNameType.validate("") must
        be(Failure(Seq(
          Path -> Seq(ValidationError("error.required"))
        )))
    }

    "fail to validate a string longer than 35" in {

      indivNameType.validate("a" * 36) must
        be(Failure(Seq(
          Path -> Seq(ValidationError("error.maxLength", FormTypes.maxNameTypeLength))
        )))
    }
  }

  "descriptionType" must {

    "successfully validate" in {

      descriptionType.validate("foobar") must
        be(Success("foobar"))
    }

    "fail to validate an empty string" in {

      descriptionType.validate("") must
        be(Failure(Seq(
          Path -> Seq(ValidationError("error.required"))
        )))
    }

    "fail to validate a string longer than 255" in {

      descriptionType.validate("a" * 256) must
        be(Failure(Seq(
          Path -> Seq(ValidationError("error.maxLength", FormTypes.maxDescriptionTypeLength))
        )))
    }
  }

  "validPostCodeType" must {

    "successfully validate" in {

      postcodeType.validate("177A") must
        be(Success("177A"))
    }

    "fail to validate an empty string" in {

      postcodeType.validate("") must
        be(Failure(Seq(
          Path -> Seq(ValidationError("error.required.postcode"))
        )))
    }

    "fail to validate a string longer than 10" in {

      postcodeType.validate("a" * 11) must
        be(Failure(Seq(
          Path -> Seq(ValidationError("error.invalid.postcode"))
        )))
    }
  }

  "vrnType" must {

    "successfully validate" in {

      vrnType.validate("123456789") must
        be(Success("123456789"))
    }

    "fail to validate an empty string" in {

      vrnType.validate("") must
        be(Failure(Seq(
          Path -> Seq(ValidationError("error.required.vat.number"))
        )))
    }

    "fail to validate a string longer than 9" in {

      vrnType.validate("1" * 10) must
        be(Failure(Seq(
          Path -> Seq(ValidationError("error.invalid.vat.number"))
        )))
    }
  }



  "phoneNumberType" must {
    "successfully validate" in {

      phoneNumberType.validate("1234567890") must
        be(Success("1234567890"))
    }

    "fail to validate an empty string" in {

      phoneNumberType.validate("") must
        be(Failure(Seq(
          Path -> Seq(ValidationError("error.required.rp.phone"))
        )))
    }

    "fail to validate a string longer than 30" in {

      phoneNumberType.validate("1" * 31) must
        be(Failure(Seq(
          Path -> Seq(ValidationError("error.maxLength", 30))
        )))
    }
  }

  "emailType" must {

    "successfully validate" in {

      emailType.validate("test@test.com") must
        be(Success("test@test.com"))
    }

    "successfully validate 2" in {

      emailType.validate("t@t") must
        be(Success("t@t"))
    }

    "fail to validate an empty string" in {

      emailType.validate("") must
        equal(Failure(Seq(
          Path -> Seq(ValidationError("error.required.rp.email"))
        )))
    }

    "fail to validate a string longer than 100" in {

      emailType.validate("1" * 101) must
        be(Failure(Seq(
          Path -> Seq(ValidationError("error.maxLength", 100))
        )))
    }

    "fail to validate an email without a `@` in it" in {

      emailType.validate("foo") must be(Failure(Seq(
                Path -> Seq(ValidationError("error.invalid.rp.email"))
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
        be(Success(model))
    }

    "fail to validate an invalid date" in {
      localDateRule.validate(Map(
        "day" -> Seq("24"),
        "month" -> Seq("13"),
        "year" -> Seq("1990")
      )) must be(Failure(Seq(
        Path -> Seq(ValidationError("error.expected.jodadate.format", "yyyy-MM-dd"))
      )))
    }

    "fail to validate missing fields" in {
      localDateRule.validate(Map.empty) must
        be(Failure(Seq(
          Path -> Seq(ValidationError("error.required")),
          Path -> Seq(ValidationError("error.required")),
          Path -> Seq(ValidationError("error.required"))
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

    "must be mandatory" in {
      accountNameType.validate("") must be(
        Failure(Seq(Path -> Seq(ValidationError("error.required")))))
    }

    "be not more than 40 characters" in {
      accountNameType.validate("This name is definitely longer than 40 characters.") must be(
        Failure(Seq(Path -> Seq(ValidationError("error.maxLength", FormTypes.maxAccountName))))
      )
    }
  }

  "sortCode must" must {

    "validate when 6 digits are supplied without - " in {
      sortCodeType.validate("654321") must be(Success("654321"))
    }

    "fail validation when more than 6 digits are supplied without - " in {
      sortCodeType.validate("87654321") must be(
      Failure(Seq(Path -> Seq(ValidationError("error.pattern", sortCodeRegex)))))
    }

    "fail when 8 non digits are supplied with - " in {
      sortCodeType.validate("ab-cd-ef") must be(
        Failure(Seq(Path -> Seq(ValidationError("error.pattern", sortCodeRegex)))))
    }

    "fail validation for sort code with any other pattern" in {
      sortCodeType.validate("8712341241431243124124654321") must be(
        Failure(Seq(Path -> Seq(ValidationError("error.pattern", sortCodeRegex))))
      )
    }
  }


  "UK Bank Account must successfully" must {

    "validate when 8 digits are supplied " in {
      ukBankAccountNumberType.validate("87654321") must be(Success("87654321"))
    }

    "fail validation when anything other than 8 characters are supplied" in {
      ukBankAccountNumberType.validate("123456") must be(
        Failure(Seq(Path -> Seq(ValidationError("error.pattern", ukBankAccountNumberRegex)))))
    }

    ukBankAccountNumberType.validate("1234567890") must be(
      Failure(Seq(Path -> Seq(ValidationError("error.maxLength", maxUKBankAccountNumberLength)))))
  }

  "For the Overseas Bank Account" must {

    "validate IBAN supplied " in {
      ibanType.validate("IBAN_4323268686686") must be(Success("IBAN_4323268686686"))
    }

    "fail validation if IBAN is longer than the permissible length" in {
      ibanType.validate("12345678901234567890123456789012345678901234567890") must be(
        Failure(Seq(Path -> Seq(ValidationError("error.maxLength", maxIBANLength)))))
    }

    "validate Non UK Account supplied " in {
      nonUKBankAccountNumberType.validate("IND22380310500093") must be(Success("IND22380310500093"))
    }

    "fail validation if Non UK Account is longer than the permissible length" in {
      nonUKBankAccountNumberType.validate("12345678901234567890123456789012345678901234567890") must be(
        Failure(Seq(Path -> Seq(ValidationError("error.maxLength", maxNonUKBankAccountNumberLength)))))
    }

  }

  "For the Declaration Add Persons 'name' fields" must {

    "fail validation if blank value is supplied for the name" in {
      declarationNameType.validate(" ") must be(Failure(Seq(Path -> Seq(ValidationError("error.required")))))
    }

    "pass validation if name supplied is 255 characters" in {
      declarationNameType.validate("1" * maxNameTypeLength) must be(Success("1" * maxNameTypeLength))
    }

    "validate other value length supplied" in {
      declarationNameType.validate("1" * (maxNameTypeLength + 1)) must be(
        Failure(Seq(Path -> Seq(ValidationError("error.maxLength", maxNameTypeLength)))))
    }
  }

  "For the Declaration Add Persons page, business activity 'other' field" must {

    "pass validation if value length supplied is 255 characters" in {
      roleWithinBusinessOtherType.validate("1" * 255) must be(Success("1" * 255))
    }

    "validate other value length supplied" in {
      roleWithinBusinessOtherType.validate("1" * 256) must be(
        Failure(Seq(Path -> Seq(ValidationError("error.maxLength", maxRoleWithinBusinessOtherType)))))
    }

    "fail validation if business type field is not selected" in {
      roleWithinBusinessOtherType.validate("") must be(
        Failure(Seq(Path -> Seq(ValidationError("error.required")))))
    }
  }

  "nino" must {

    "successfully validate" in {

      ninoType.validate("AB123456B") must
        be(Success("AB123456B"))
    }

    "fail to validate an empty string" in {

      ninoType.validate("") must
        equal(Failure(Seq(
          Path -> Seq(ValidationError("error.required.nino"))
        )))
    }

    "fail validation on exceeding maxlength" in {

      ninoType.validate("1" * 10) must
        be(Failure(Seq(
          Path -> Seq(ValidationError("error.invalid.nino"))
        )))
    }

    "fail to validate invalid data" in {

      ninoType.validate("1@@@@@") must
        be(Failure(Seq(
          Path -> Seq(ValidationError("error.invalid.nino"))
        )))
    }

  }

}
