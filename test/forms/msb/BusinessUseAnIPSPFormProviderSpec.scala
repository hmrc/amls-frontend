/*
 * Copyright 2024 HM Revenue & Customs
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

package forms.msb

import forms.behaviours.BooleanFieldBehaviours
import forms.mappings.Constraints
import models.moneyservicebusiness.{BusinessUseAnIPSP, BusinessUseAnIPSPNo, BusinessUseAnIPSPYes}
import org.scalacheck.Gen
import play.api.data.{Form, FormError}

class BusinessUseAnIPSPFormProviderSpec extends BooleanFieldBehaviours[BusinessUseAnIPSP] with Constraints {

  val formProvider: BusinessUseAnIPSPFormProvider = new BusinessUseAnIPSPFormProvider()

  override val form: Form[BusinessUseAnIPSP] = formProvider()
  override val fieldName: String             = "useAnIPSP"
  override val errorMessage: String          = "error.required.msb.ipsp"

  val inputFieldName: String       = "name"
  val secondInputFieldName: String = "referenceNumber"

  private val refNoLength = 15

  def refNoGen: Gen[String] = stringOfLengthGen(refNoLength).suchThat(_.length == refNoLength)

  "form" must {

    "bind" when {

      "'No' is submitted" in {

        val boundForm = form.bind(Map(fieldName -> "false"))

        boundForm.value  shouldBe Some(BusinessUseAnIPSPNo)
        boundForm.errors shouldBe Nil
      }

      "'Yes' is submitted and details are given" in {

        forAll(stringOfLengthGen(formProvider.length).suchThat(_.nonEmpty), refNoGen) { (name, refNo) =>
          val boundForm = form.bind(
            Map(
              fieldName            -> "true",
              inputFieldName       -> name,
              secondInputFieldName -> refNo
            )
          )

          boundForm.value  shouldBe Some(BusinessUseAnIPSPYes(name, refNo))
          boundForm.errors shouldBe Nil
        }
      }
    }

    "fail to bind and give the correct error" when {

      "an empty value is submitted" in {

        val boundForm = form.bind(Map(fieldName -> ""))

        boundForm.errors.headOption shouldBe Some(FormError(fieldName, errorMessage))
      }

      "an invalid value is submitted" in {

        forAll(stringsLongerThan(1)) { invalidFormValue =>
          val boundForm = form.bind(Map(fieldName -> invalidFormValue))

          boundForm.errors.headOption shouldBe Some(FormError(fieldName, errorMessage))
        }
      }

      "'Yes' is submitted without a name OR ref number" in {

        val boundForm = form.bind(Map(fieldName -> "true"))

        boundForm.errors shouldBe Seq(
          FormError(inputFieldName, "error.required.msb.ipsp.name"),
          FormError(secondInputFieldName, "error.invalid.mlr.number")
        )
      }

      "'Yes' is submitted with a name" which {
        "is too long" in {

          forAll(stringsLongerThan(formProvider.length + 1), refNoGen) { (longName, refNo) =>
            val boundForm = form.bind(
              Map(
                fieldName            -> "true",
                inputFieldName       -> longName,
                secondInputFieldName -> refNo
              )
            )
            boundForm.errors.headOption shouldBe Some(
              FormError(inputFieldName, "error.invalid.msb.ipsp.name", Seq(formProvider.length))
            )
          }
        }

        "is invalid" in {
          val boundForm = form.bind(
            Map(
              fieldName            -> "true",
              inputFieldName       -> "Busines§ Name",
              secondInputFieldName -> "ASDFGH1234567890"
            )
          )
          boundForm.errors.headOption shouldBe Some(
            FormError(inputFieldName, "error.invalid.msb.ipsp.format", Seq(basicPunctuationRegex))
          )
        }
      }

      "'Yes' is submitted with a ref number" which {

        "is invalid" in {
          forAll(
            stringOfLengthGen(formProvider.length).suchThat(_.nonEmpty),
            invalidCharForNames,
            stringOfLengthGen(14).suchThat(_.nonEmpty)
          ) { (name, invalid, refNo) =>
            val boundForm = form.bind(
              Map(
                fieldName            -> "true",
                inputFieldName       -> name,
                secondInputFieldName -> (refNo + invalid)
              )
            )
            boundForm.errors.headOption shouldBe Some(
              FormError(secondInputFieldName, "error.invalid.mlr.number", Seq("""^[0-9]{8}|[a-zA-Z0-9]{15}$"""))
            )
          }
        }
      }
    }
  }
}
