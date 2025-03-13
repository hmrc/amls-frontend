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

package forms.responsiblepeople

import forms.behaviours.{CheckboxFieldBehaviours, StringFieldBehaviours}
import forms.mappings.Constraints
import models.responsiblepeople.{Other, PositionWithinBusiness}
import org.scalacheck.Gen
import play.api.data.{Form, FormError}

class PositionWithinBusinessFormProviderSpec
    extends CheckboxFieldBehaviours
    with StringFieldBehaviours
    with Constraints {

  val formProvider = new PositionWithinBusinessFormProvider()

  val form: Form[Set[PositionWithinBusiness]] = formProvider()

  val checkboxFieldName = "positions"
  val textFieldName     = "otherPosition"

  "PositionWithinBusinessFormProvider" when {

    s"$checkboxFieldName is evaluated" must {

      behave like fieldThatBindsValidData(
        form,
        checkboxFieldName,
        Gen.oneOf(PositionWithinBusiness.all.map(_.toString))
      )

      behave like checkboxFieldWithWrapper[PositionWithinBusiness, Set[PositionWithinBusiness]](
        form,
        checkboxFieldName,
        PositionWithinBusiness.all.filterNot(_ == Other("")),
        Set(_),
        x => x.toSet,
        FormError(s"$checkboxFieldName[0]", "error.required.positionWithinBusiness")
      )

      behave like mandatoryCheckboxField(form, checkboxFieldName, "error.required.positionWithinBusiness")
    }

    s"$textFieldName is evaluated" must {

      behave like fieldThatBindsValidData(
        form,
        textFieldName,
        stringsShorterThan(formProvider.length).suchThat(_.nonEmpty)
      )

      s"bind when Other is selected and $textFieldName is populated" in {

        forAll(stringsShorterThan(formProvider.length).suchThat(_.nonEmpty)) { other =>
          val result = form.bind(
            Map(
              s"$checkboxFieldName[0]" -> Other("").toString,
              textFieldName            -> other
            )
          )

          result.value shouldBe Some(Set(Other(other)))
          assert(result.errors.isEmpty)
        }
      }

      "fail to bind" when {

        s"Other is selected but $textFieldName is empty" in {

          val result = form.bind(
            Map(
              s"$checkboxFieldName[0]" -> Other("").toString,
              textFieldName            -> ""
            )
          )

          result.value  shouldBe None
          result.errors shouldBe Seq(
            FormError(textFieldName, "responsiblepeople.position_within_business.other_position.othermissing")
          )
        }

        s"Other is selected but $textFieldName is longer than ${formProvider.length}" in {

          forAll(stringsLongerThan(formProvider.length).suchThat(_.nonEmpty)) { invalid =>
            val result = form.bind(
              Map(
                s"$checkboxFieldName[0]" -> Other("").toString,
                textFieldName            -> invalid
              )
            )

            result.value  shouldBe None
            result.errors shouldBe Seq(
              FormError(
                textFieldName,
                "error.invalid.rp.position_within_business.other_position.maxlength.255",
                Seq(formProvider.length)
              )
            )
          }
        }

        s"Other is selected but $textFieldName violates regex" in {

          forAll(invalidCharForNames.suchThat(_.nonEmpty)) { invalid =>
            val result = form.bind(
              Map(
                s"$checkboxFieldName[0]" -> Other("").toString,
                textFieldName            -> invalid
              )
            )

            result.value  shouldBe None
            result.errors shouldBe Seq(
              FormError(
                textFieldName,
                "error.invalid.rp.position_within_business.other_position",
                Seq(basicPunctuationRegex)
              )
            )
          }
        }
      }
//
//      behave like fieldWithMaxLength(
//        form,
//        textFieldName,
//        formProvider.length,
//        FormError(
//          textFieldName, "error.invalid.rp.position_within_business.other_position.maxlength.255", Seq(formProvider.length)
//        )
//      )
    }
  }
}
