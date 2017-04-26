package models.responsiblepeople

import models.Country
import org.joda.time.LocalDate
import org.scalatestplus.play.PlaySpec
import jto.validation.{Invalid, Path, Valid}
import jto.validation.ValidationError
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
          be(Valid(PersonResidenceType(UKResidence("AA346464B"), Country("United Kingdom", "GB"), Some(Country("United Kingdom", "GB")))))
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
          be(Valid(PersonResidenceType(NonUKResidence(new LocalDate(1990, 2, 24), UKPassport("123464646")),
            Country("United Kingdom", "GB"), Some(Country("United Kingdom", "GB")))))
      }

      "fail validation" when {

        "isUKResidence is missing" in {
          val ukModel = Map(
            "isUKResidence" -> Seq(""),
            "dateOfBirth.day" -> Seq("24"),
            "dateOfBirth.month" -> Seq("2"),
            "dateOfBirth.year" -> Seq("1990"),
            "passportType" -> Seq("01"),
            "ukPassportNumber" -> Seq("12346464688"),
            "countryOfBirth" -> Seq("GB"),
            "nationality" -> Seq("GB")
          )

          PersonResidenceType.formRule.validate(ukModel) must
            be(Invalid(Seq(
              Path \ "isUKResidence" -> Seq(ValidationError("error.required.rp.is.uk.resident"))
            )))
        }

        "country has is missing" in {
          val ukModel = Map(
            "isUKResidence" -> Seq("true"),
            "nino" -> Seq("AA346464B"),
            "countryOfBirth" -> Seq(""),
            "nationality" -> Seq("GB")
          )

          PersonResidenceType.formRule.validate(ukModel) must
            be(Invalid(Seq(Path \ "countryOfBirth" -> Seq(ValidationError("error.required.rp.birth.country")))))
        }

        "UK" when {
          "nino is missing" in {
            val ukModel = Map(
              "isUKResidence" -> Seq("true"),
              "nino" -> Seq(""),
              "countryOfBirth" -> Seq("GB"),
              "nationality" -> Seq("GB")
            )

            PersonResidenceType.formRule.validate(ukModel) must
              be(Invalid(Seq(Path \ "nino" -> Seq(ValidationError("error.required.nino")))))
          }

          "nino is invalid" in {
            val ukModel = Map(
              "isUKResidence" -> Seq("true"),
              "nino" -> Seq("`1234567890"),
              "countryOfBirth" -> Seq("GB"),
              "nationality" -> Seq("GB")
            )

            PersonResidenceType.formRule.validate(ukModel) must
              be(Invalid(Seq(Path \ "nino" -> Seq(ValidationError("error.invalid.nino")))))
          }

          "countryOfBirth is missing" in {
            val ukModel = Map(
              "isUKResidence" -> Seq("true"),
              "nino" -> Seq("AA346464B"),
              "countryOfBirth" -> Seq(""),
              "nationality" -> Seq("GB")
            )

            PersonResidenceType.formRule.validate(ukModel) must
              be(Invalid(Seq(Path \ "countryOfBirth" -> Seq(ValidationError("error.required.rp.birth.country")))))
          }

          "countryOfBirth is invalid" in {
            val ukModel = Map(
              "isUKResidence" -> Seq("true"),
              "nino" -> Seq("AA346464B"),
              "countryOfBirth" -> Seq("12345678"),
              "nationality" -> Seq("GB")
            )

            PersonResidenceType.formRule.validate(ukModel) must
              be(Invalid(Seq(Path \ "countryOfBirth" -> Seq(ValidationError("error.required.rp.birth.country")))))
          }
        }

        "non UK" when {

          "missing" when {
            "dateOfBirth" in {
              val ukModel = Map(
                "isUKResidence" -> Seq("false"),
                "dateOfBirth.day" -> Seq(""),
                "dateOfBirth.month" -> Seq(""),
                "dateOfBirth.year" -> Seq(""),
                "passportType" -> Seq("01"),
                "ukPassportNumber" -> Seq("123456789"),
                "countryOfBirth" -> Seq("GB"),
                "nationality" -> Seq("GB")
              )

              PersonResidenceType.formRule.validate(ukModel) must
                be(Invalid(Seq(
                  Path \ "dateOfBirth" -> Seq(ValidationError("error.expected.jodadate.format", "yyyy-MM-dd"))
                )))
            }

            "passportType" in {
              val ukModel = Map(
                "isUKResidence" -> Seq("false"),
                "dateOfBirth.day" -> Seq("01"),
                "dateOfBirth.month" -> Seq("02"),
                "dateOfBirth.year" -> Seq("1990"),
                "ukPassportNumber" -> Seq("123456789"),
                "countryOfBirth" -> Seq("GB"),
                "nationality" -> Seq("GB")
              )

              PersonResidenceType.formRule.validate(ukModel) must
                be(Invalid(Seq(
                  Path \ "passportType" -> Seq(ValidationError("error.required.rp.passport.option"))
                )))
            }

            "countryOfBirth" in {
              val ukModel = Map(
                "isUKResidence" -> Seq("false"),
                "dateOfBirth.day" -> Seq("01"),
                "dateOfBirth.month" -> Seq("02"),
                "dateOfBirth.year" -> Seq("1990"),
                "passportType" -> Seq("01"),
                "ukPassportNumber" -> Seq("123456789"),
                "countryOfBirth" -> Seq(""),
                "nationality" -> Seq("GB")
              )

              PersonResidenceType.formRule.validate(ukModel) must
                be(Invalid(Seq(
                  Path \ "countryOfBirth" -> Seq(ValidationError("error.required.rp.birth.country"))
                )))
            }

            "non uk passport number" in {
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
                be(Invalid(Seq(Path \ "nonUKPassportNumber" -> Seq(ValidationError("error.required.non.uk.passport")))))
            }

          }

          "invalid" when {
            "uk passport" in {
              val ukModel = Map(
                "isUKResidence" -> Seq("false"),
                "dateOfBirth.day" -> Seq("24"),
                "dateOfBirth.month" -> Seq("2"),
                "dateOfBirth.year" -> Seq("1990"),
                "passportType" -> Seq("01"),
                "ukPassportNumber" -> Seq("$87654321"),
                "countryOfBirth" -> Seq("GB"),
                "nationality" -> Seq("GB")
              )

              PersonResidenceType.formRule.validate(ukModel) must
                be(Invalid(Seq(Path \ "ukPassportNumber" -> Seq(ValidationError("error.invalid.uk.passport")))))
            }

            "non UK passport is too long" in {
              val ukModel = Map(
                "isUKResidence" -> Seq("false"),
                "dateOfBirth.day" -> Seq("24"),
                "dateOfBirth.month" -> Seq("2"),
                "dateOfBirth.year" -> Seq("1990"),
                "passportType" -> Seq("02"),
                "nonUKPassportNumber" -> Seq("abc" * 40),
                "countryOfBirth" -> Seq("GB"),
                "nationality" -> Seq("GB")
              )

              PersonResidenceType.formRule.validate(ukModel) must
                be(Invalid(Seq(Path \ "nonUKPassportNumber" -> Seq(ValidationError("error.invalid.non.uk.passport")))))
            }

            "countryOfBirth" in {
              val ukModel = Map(
                "isUKResidence" -> Seq("false"),
                "dateOfBirth.day" -> Seq("01"),
                "dateOfBirth.month" -> Seq("02"),
                "dateOfBirth.year" -> Seq("1990"),
                "passportType" -> Seq("01"),
                "ukPassportNumber" -> Seq("123456789"),
                "countryOfBirth" -> Seq("sdfghjklkjhgfd"),
                "nationality" -> Seq("GB")
              )

              PersonResidenceType.formRule.validate(ukModel) must
                be(Invalid(Seq(
                  Path \ "countryOfBirth" -> Seq(ValidationError("error.required.rp.birth.country"))
                )))
            }

            "dateOfBirth" in {
              val ukModel = Map(
                "isUKResidence" -> Seq("false"),
                "dateOfBirth.day" -> Seq("c"),
                "dateOfBirth.month" -> Seq("b"),
                "dateOfBirth.year" -> Seq("a"),
                "passportType" -> Seq("01"),
                "ukPassportNumber" -> Seq("123456789"),
                "countryOfBirth" -> Seq("GB"),
                "nationality" -> Seq("GB")
              )

              PersonResidenceType.formRule.validate(ukModel) must
                be(Invalid(Seq(
                  Path \ "dateOfBirth" -> Seq(ValidationError("error.expected.jodadate.format", "yyyy-MM-dd"))
                )))
            }

          }

        }

      }

      "successfully read json when nationality field is empty" in {
        val ukModel = Map(
          "isUKResidence" -> Seq("true"),
          "nino" -> Seq("AA346464B"),
          "countryOfBirth" -> Seq("GB")
        )

        PersonResidenceType.formRule.validate(ukModel) must
          be(Valid(PersonResidenceType(UKResidence("AA346464B"),Country("United Kingdom", "GB"), None)))
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