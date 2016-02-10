package models

import org.scalatestplus.play.PlaySpec
import org.specs2.mock.mockito.MockitoMatchers
import play.api.data.mapping.{Path, Failure, Success}
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

  "prevMLRRegNoType" must {

    "successfully validate" in {

      prevMLRRegNoType.validate("12345678") must
        be(Success("12345678"))

      prevMLRRegNoType.validate("123456789012345") must
        be(Success("123456789012345"))
    }

    "fail to validate an empty string" in {

      prevMLRRegNoType.validate("") must
        be(Failure(Seq(
          Path -> Seq(ValidationError("error.required"))
        )))
    }

    "fail to validate a string longer than 15" in {

      prevMLRRegNoType.validate("1" * 16) must
        be(Failure(Seq(
          Path -> Seq(ValidationError("error.maxLength", FormTypes.maxPrevMLRRegNoLength))
        )))
    }
  }

  "validateAddressType" must {

    "successfully validate" in {

      addressType.validate("177A") must
        be(Success("177A"))
    }

    "fail to validate an empty string" in {

      addressType.validate("") must
        be(Failure(Seq(
          Path -> Seq(ValidationError("error.required"))
        )))
    }

    "fail to validate a string longer than 35" in {

      addressType.validate("a" * 36) must
        be(Failure(Seq(
          Path -> Seq(ValidationError("error.maxLength", FormTypes.maxAddressLength))
        )))
    }
  }

  "validPostCodeType" must {

    "successfully validate" in {

      postCodeType.validate("177A") must
        be(Success("177A"))
    }

    "fail to validate an empty string" in {

      postCodeType.validate("") must
        be(Failure(Seq(
          Path -> Seq(ValidationError("error.required"))
        )))
    }

    "fail to validate a string longer than 10" in {

      postCodeType.validate("a" * 11) must
        be(Failure(Seq(
          Path -> Seq(ValidationError("error.maxLength", FormTypes.maxPostCodeTypeLength))
        )))
    }
  }

  "validCountryType" must {

    "successfully validate" in {

      countryType.validate("IN") must
        be(Success("IN"))
    }

    "fail to validate an empty string" in {

      countryType.validate("") must
        be(Failure(Seq(
          Path -> Seq(ValidationError("error.required"))
        )))
    }

    "fail to validate a string longer than 3" in {

      countryType.validate("a" * 3) must
        be(Failure(Seq(
          Path -> Seq(ValidationError("error.maxLength", FormTypes.maxCountryTypeLength))
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
          Path -> Seq(ValidationError("error.required"))
        )))
    }

    "fail to validate a string longer than 9" in {

      vrnType.validate("1" * 10) must
        be(Failure(Seq(
          Path -> Seq(ValidationError("error.maxLength", 9))
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
          Path -> Seq(ValidationError("error.required"))
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

    "fail to validate an empty string" in {

      emailType.validate("") must
        be(Failure(Seq(
          Path -> Seq(ValidationError("error.required"))
        )))
    }

    "fail to validate a string longer than 100" in {

      emailType.validate("1" * 101) must
        be(Failure(Seq(
          Path -> Seq(ValidationError("error.maxLength", 100))
        )))
    }
  }

  "premisesTradingNameType" must {

    "successfully validate" in {
      premisesTradingNameType.validate("asdf") must
        be(Success("asdf"))
    }

    "fail to validate a string longer than 120" in {
      premisesTradingNameType.validate("a" * 121) must
        be(Failure(Seq(
          Path -> Seq(ValidationError("error.maxLength", 120))
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

  "redressOtherType" must {

    "successfully validate" in {

      redressOtherType.validate("foobar") must
        be(Success("foobar"))
    }

    "fail to validate an empty string" in {

      redressOtherType.validate("") must
        be(Failure(Seq(
          Path -> Seq(ValidationError("error.required"))
        )))
    }

    "fail to validate a string longer than 255" in {

      redressOtherType.validate("a" * 256) must
        be(Failure(Seq(
          Path -> Seq(ValidationError("error.maxLength", FormTypes.maxRedressOtherTypeLength))
        )))
    }
  }
}
