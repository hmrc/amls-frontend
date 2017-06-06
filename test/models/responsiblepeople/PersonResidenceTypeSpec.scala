/*
 * Copyright 2017 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package models.responsiblepeople

import controllers.responsiblepeople.NinoUtil
import models.Country
import org.joda.time.LocalDate
import org.scalatestplus.play.PlaySpec
import jto.validation.{Invalid, Path, Valid}
import jto.validation.ValidationError
import play.api.libs.json.{JsSuccess, Json}

class PersonResidenceTypeSpec extends PlaySpec with NinoUtil {

  "PersonResidenceType" must {

    "Form Validation" must {

      "successfully validate uk model" in {
        val nino = nextNino
        val ukModel = Map(
          "isUKResidence" -> Seq("true"),
          "passportType" -> Seq("01"),
          "ukPassportNumber" -> Seq("000000000"),
          "nino" -> Seq(nino),
          "countryOfBirth" -> Seq("GB"),
          "nationality" -> Seq("GB")
        )

        PersonResidenceType.formRule.validate(ukModel) must
          be(Valid(PersonResidenceType(UKResidence(nino), Some(Country("United Kingdom", "GB")), Some(Country("United Kingdom", "GB")))))
      }

      "successfully validate non uk model" in {
        val ukModel = Map(
          "isUKResidence" -> Seq("false"),
          "dateOfBirth.day" -> Seq("24"),
          "dateOfBirth.month" -> Seq("2"),
          "dateOfBirth.year" -> Seq("1990"),
          "passportType" -> Seq("03"),
          "countryOfBirth" -> Seq("GB"),
          "nationality" -> Seq("GB")
        )

        PersonResidenceType.formRule.validate(ukModel) must
          be(Valid(PersonResidenceType(NonUKResidence(new LocalDate(1990, 2, 24), NoPassport),
            Some(Country("United Kingdom", "GB")), Some(Country("United Kingdom", "GB")))))
      }

      "fail validation" when {

        "isUKResidence is missing" in {
          val ukModel = Map(
            "isUKResidence" -> Seq(""),
            "dateOfBirth.day" -> Seq("24"),
            "dateOfBirth.month" -> Seq("2"),
            "dateOfBirth.year" -> Seq("1990"),
            "passportType" -> Seq("01"),
            "ukPassportNumber" -> Seq("00000000000"),
            "countryOfBirth" -> Seq("GB"),
            "nationality" -> Seq("GB")
          )

          PersonResidenceType.formRule.validate(ukModel) must
            be(Invalid(Seq(
              Path \ "isUKResidence" -> Seq(ValidationError("error.required.rp.is.uk.resident"))
            )))
        }

        "country has is missing" in {
          val nino = nextNino
          val ukModel = Map(
            "isUKResidence" -> Seq("true"),
            "nino" -> Seq(nino),
            "countryOfBirth" -> Seq(""),
            "nationality" -> Seq("GB")
          )

          PersonResidenceType.formRule.validate(ukModel) must
            be(Valid(PersonResidenceType(UKResidence(nino), None, Some(Country("United Kingdom", "GB")))))
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

          "countryOfBirth is invalid" in {
            val ukModel = Map(
              "isUKResidence" -> Seq("true"),
              "nino" -> Seq(nextNino),
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
                "ukPassportNumber" -> Seq("000000000"),
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
                "ukPassportNumber" -> Seq("000000000"),
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
                "ukPassportNumber" -> Seq("000000000"),
                "countryOfBirth" -> Seq(""),
                "nationality" -> Seq("GB")
              )

              PersonResidenceType.formRule.validate(ukModel) must
                be(Valid(PersonResidenceType(NonUKResidence(new LocalDate(1990, 2, 1),UKPassport("000000000")),None,Some(Country("United Kingdom","GB")))))
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

            "dateOfBirth" in {
              val ukModel = Map(
                "isUKResidence" -> Seq("false"),
                "dateOfBirth.day" -> Seq("c"),
                "dateOfBirth.month" -> Seq("b"),
                "dateOfBirth.year" -> Seq("a"),
                "passportType" -> Seq("01"),
                "ukPassportNumber" -> Seq("000000000"),
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
        val nino = nextNino
        val ukModel = Map(

          "isUKResidence" -> Seq("true"),
          "nino" -> Seq(nino),
          "countryOfBirth" -> Seq("GB")
        )

        PersonResidenceType.formRule.validate(ukModel) must
          be(Valid(PersonResidenceType(UKResidence(nino), Some(Country("United Kingdom", "GB")), None)))
      }

      "write correct UKResidence model" in {

        val data = PersonResidenceType(UKResidence("12346464646"), Some(Country("United Kingdom", "GB")), Some(Country("United Kingdom", "GB")))

        PersonResidenceType.formWrites.writes(data) mustBe Map(
          "isUKResidence" -> Seq("true"),
          "nino" -> Seq("12346464646"),
          "countryOfBirth" -> Seq("GB"),
          "nationality" -> Seq("GB")
        )
      }

      "write correct NonUKResidence model" in {

        val data = PersonResidenceType(NonUKResidence(new LocalDate(1990, 2, 24), UKPassport("12346464646")),
          Some(Country("United Kingdom", "GB")), Some(Country("United Kingdom", "GB")))

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
          Some(Country("United Kingdom", "GB")), Some(Country("United Kingdom", "GB")))

        PersonResidenceType.jsonRead.reads(PersonResidenceType.jsonWrite.writes(ukModel)) must
          be(JsSuccess(ukModel))

      }

      "Successfully validate non uk residence type model" in {
        val nonUKModel = PersonResidenceType(NonUKResidence(new LocalDate(1990, 2, 24), UKPassport("123464646")),
          Some(Country("United Kingdom", "GB")), Some(Country("United Kingdom", "GB")))

        PersonResidenceType.jsonRead.reads(
          PersonResidenceType.jsonWrite.writes(nonUKModel)) must
          be(JsSuccess(nonUKModel))
      }
    }
  }
}