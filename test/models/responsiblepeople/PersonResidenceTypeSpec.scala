package models.responsiblepeople

import models.Country
import org.joda.time.LocalDate
import org.scalatestplus.play.PlaySpec
import play.api.data.mapping.{Failure, Path, Success}
import play.api.data.validation.ValidationError
import play.api.libs.json.{JsSuccess, Json}

class PersonResidenceTypeSpec extends PlaySpec {

  "PersonResidenceType" must {

    "Form Validation" must {

      "successfully validate uk model" in {
        val ukModel = Map(
          "isUKResidence" -> Seq("true"),
          "nino" -> Seq("AA346464B"),
          "countryOfBirth" -> Seq("GB"),
          "nationality" -> Seq("GB")
        )

        PersonResidenceType.formRule.validate(ukModel) must
          be(Success(PersonResidenceType(UKResidence("AA346464B"), Country("United Kingdom", "GB"), Some(Country("United Kingdom", "GB")))))
      }

      "fail to validate on missing nino" in {
        val ukModel = Map(
          "isUKResidence" -> Seq("true"),
          "nino" -> Seq(""),
          "countryOfBirth" -> Seq("GB"),
          "nationality" -> Seq("GB")
        )

        PersonResidenceType.formRule.validate(ukModel) must
          be(Failure(Seq(Path \ "nino" -> Seq(ValidationError("error.required.nino")))))
      }

      "fail to validate on invalid nino" in {
        val ukModel = Map(
          "isUKResidence" -> Seq("true"),
          "nino" -> Seq("12346464646"),
          "countryOfBirth" -> Seq("GB"),
          "nationality" -> Seq("GB")
        )

        PersonResidenceType.formRule.validate(ukModel) must
          be(Failure(Seq(Path \ "nino" -> Seq(ValidationError("error.invalid.nino")))))
      }


      "fail to validate on invalid country of birth" in {
        val ukModel = Map(
          "isUKResidence" -> Seq("true"),
          "nino" -> Seq("AA346464B"),
          "countryOfBirth" -> Seq("GBuuu"),
          "nationality" -> Seq("GB")
        )

        PersonResidenceType.formRule.validate(ukModel) must
          be(Failure(Seq(Path \ "countryOfBirth" -> Seq(ValidationError("error.required.rp.birth.country")))))
      }

      "successfully read json when nationality field is empty" in {
        val ukModel = Map(
          "isUKResidence" -> Seq("true"),
          "nino" -> Seq("AA346464B"),
          "countryOfBirth" -> Seq("GB")
        )

        PersonResidenceType.formRule.validate(ukModel) must
          be(Success(PersonResidenceType(UKResidence("AA346464B"),Country("United Kingdom", "GB"), None)))
      }

      "successfully validate non uk model" in {
        val ukModel = Map(
          "isUKResidence" -> Seq("false"),
          "dateOfBirth.day" -> Seq("24"),
          "dateOfBirth.month" -> Seq("2"),
          "dateOfBirth.year" -> Seq("1990"),
          "passportType" -> Seq("01"),
          "ukPassportNumber" -> Seq("123464646"),
          "countryOfBirth" -> Seq("GB"),
          "nationality" -> Seq("GB")
        )

        PersonResidenceType.formRule.validate(ukModel) must
          be(Success(PersonResidenceType(NonUKResidence(new LocalDate(1990, 2, 24), UKPassport("123464646")),
            Country("United Kingdom", "GB"), Some(Country("United Kingdom", "GB")))))
      }

      "fail to validate when non uk fields missing or invalid" in {
        val ukModel = Map(
          "isUKResidence" -> Seq("false"),
          "dateOfBirth.day" -> Seq(""),
          "dateOfBirth.month" -> Seq(""),
          "dateOfBirth.year" -> Seq(""),
          "passportType" -> Seq("01"),
          "ukPassportNumber" -> Seq("12346464688"),
          "countryOfBirth" -> Seq("GB"),
          "nationality" -> Seq("GB")
        )

        PersonResidenceType.formRule.validate(ukModel) must
          be(Failure(Seq(Path \ "dateOfBirth" -> Seq(ValidationError("error.expected.jodadate.format", "yyyy-MM-dd")),
            Path \ "ukPassportNumber" -> Seq(ValidationError("error.invalid.uk.passport")))))
      }

      "fail to validate when non uk  missing uk passport number" in {
        val ukModel = Map(
          "isUKResidence" -> Seq("false"),
          "dateOfBirth.day" -> Seq("24"),
          "dateOfBirth.month" -> Seq("2"),
          "dateOfBirth.year" -> Seq("1990"),
          "passportType" -> Seq("01"),
          "ukPassportNumber" -> Seq(""),
          "countryOfBirth" -> Seq("GB"),
          "nationality" -> Seq("GB")
        )

        PersonResidenceType.formRule.validate(ukModel) must
          be(Failure(Seq(Path \ "ukPassportNumber" -> Seq(ValidationError("error.required.uk.passport")))))
      }


      "fail to validate when non uk  missing non uk passport number" in {
        val ukModel = Map(
          "isUKResidence" -> Seq("false"),
          "dateOfBirth.day" -> Seq("24"),
          "dateOfBirth.month" -> Seq("2"),
          "dateOfBirth.year" -> Seq("1990"),
          "passportType" -> Seq("02"),
          "nonUKPassportNumber" -> Seq(""),
          "countryOfBirth" -> Seq("GB"),
          "nationality" -> Seq("GB")
        )

        PersonResidenceType.formRule.validate(ukModel) must
          be(Failure(Seq(Path \ "nonUKPassportNumber" -> Seq(ValidationError("error.required.non.uk.passport")))))
      }

      "fail to validate when non uk  invalid non uk passport number" in {
        val ukModel = Map(
          "isUKResidence" -> Seq("false"),
          "dateOfBirth.day" -> Seq("24"),
          "dateOfBirth.month" -> Seq("2"),
          "dateOfBirth.year" -> Seq("1990"),
          "passportType" -> Seq("02"),
          "nonUKPassportNumber" -> Seq("121" * 20),
          "countryOfBirth" -> Seq("GB"),
          "nationality" -> Seq("GB")
        )

        PersonResidenceType.formRule.validate(ukModel) must
          be(Failure(Seq(Path \ "nonUKPassportNumber" -> Seq(ValidationError("error.invalid.non.uk.passport")))))
      }

      "write correct UKResidence model" in {

        val data = PersonResidenceType(UKResidence("12346464646"), Country("United Kingdom", "GB"), Some(Country("United Kingdom", "GB")))

        PersonResidenceType.formWrites.writes(data) mustBe Map(
          "isUKResidence" -> Seq("true"),
          "nino" -> Seq("12346464646"),
          "countryOfBirth" -> Seq("GB"),
          "nationality" -> Seq("GB")
        )
      }

      "write correct NonUKResidence model" in {

        val data = PersonResidenceType(NonUKResidence(new LocalDate(1990, 2, 24), UKPassport("12346464646")),
          Country("United Kingdom", "GB"), Some(Country("United Kingdom", "GB")))

        PersonResidenceType.formWrites.writes(data) mustBe Map(
          "isUKResidence" -> Seq("false"),
          "dateOfBirth.day" -> Seq("24"),
          "dateOfBirth.month" -> Seq("2"),
          "dateOfBirth.year" -> Seq("1990"),
          "passportType" -> Seq("01"),
          "ukPassportNumber" -> Seq("12346464646"),
          "countryOfBirth" -> Seq("GB"),
          "nationality" -> Seq("GB")
        )
      }
    }

    "Json validation" must {

      "Successfully read uk residence type model" in {
        val ukModel = PersonResidenceType(UKResidence("123464646"),
          Country("United Kingdom", "GB"), Some(Country("United Kingdom", "GB")))

        PersonResidenceType.jsonRead.reads( PersonResidenceType.jsonWrite.writes(ukModel)) must
          be(JsSuccess(ukModel))

      }

      "Successfully validate non uk residence type model" in {
        val nonUKModel = PersonResidenceType(NonUKResidence(new LocalDate(1990, 2, 24), UKPassport("123464646")),
          Country("United Kingdom", "GB"), Some(Country("United Kingdom", "GB")))

        PersonResidenceType.jsonRead.reads(
          PersonResidenceType.jsonWrite.writes(nonUKModel)) must
          be(JsSuccess(nonUKModel))
      }
    }
  }
}