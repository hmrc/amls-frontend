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

      validAddressType.validate("177A") must
        be(Success("177A"))
    }

    "fail to validate an empty string" in {

      validAddressType.validate("") must
        be(Failure(Seq(
          Path -> Seq(ValidationError("error.required"))
        )))
    }

    "fail to validate a string longer than 35" in {

      validAddressType.validate("a" * 36) must
        be(Failure(Seq(
          Path -> Seq(ValidationError("error.maxLength", FormTypes.maxAddressLength))
        )))
    }
  }

  "validPostCodeType" must {

    "successfully validate" in {

      validPostCodeType.validate("177A") must
        be(Success("177A"))
    }

    "fail to validate an empty string" in {

      validPostCodeType.validate("") must
        be(Failure(Seq(
          Path -> Seq(ValidationError("error.required"))
        )))
    }

    "fail to validate a string longer than 255" in {

      validPostCodeType.validate("a" * 11) must
        be(Failure(Seq(
          Path -> Seq(ValidationError("error.maxLength", FormTypes.maxPostCodeLength))
        )))
    }
  }

  "validCountryType" must {

    "successfully validate" in {

      validCountryType.validate("IN") must
        be(Success("IN"))
    }

    "fail to validate an empty string" in {

      validCountryType.validate("") must
        be(Failure(Seq(
          Path -> Seq(ValidationError("error.required"))
        )))
    }

    "fail to validate a string longer than 255" in {

      validCountryType.validate("a" * 3) must
        be(Failure(Seq(
          Path -> Seq(ValidationError("error.maxLength", FormTypes.maxCountryTypeLength))
        )))
    }
  }
}
