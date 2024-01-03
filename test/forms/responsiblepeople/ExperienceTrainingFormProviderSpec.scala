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

import forms.behaviours.StringFieldBehaviours
import forms.mappings.Constraints
import models.responsiblepeople.{ExperienceTraining, ExperienceTrainingYes, ExperienceTrainingNo}
import play.api.data.{Form, FormError}

class ExperienceTrainingFormProviderSpec extends StringFieldBehaviours with Constraints {

  val formProvider = new ExperienceTrainingFormProvider()

  val form: Form[ExperienceTraining] = formProvider()
  val booleanFieldName: String = "experienceTraining"
  val stringFieldName: String = "experienceInformation"

  "ExperienceTrainingFormProvider" must {

    "bind" when {

      "true is submitted with other names" in {

        forAll(stringsShorterThan(formProvider.length).suchThat(_.nonEmpty)) { info =>

          val result = form.bind(Map(
            booleanFieldName -> "true",
            stringFieldName -> info
          ))

          result.value shouldBe Some(ExperienceTrainingYes(info))
          assert(result.errors.isEmpty)
        }
      }

      "false is submitted" in {

        val result = form.bind(Map(
          booleanFieldName -> "false"
        ))

        result.value shouldBe Some(ExperienceTrainingNo)
        assert(result.errors.isEmpty)
      }
    }

    "fail to bind" when {

      s"$booleanFieldName is an invalid value" in {

        forAll(alphaStringsShorterThan(formProvider.length).suchThat(_.nonEmpty)) { invalid =>

          val result = form.bind(Map(
            booleanFieldName -> invalid
          ))

          result.value shouldBe None
          result.errors shouldBe Seq(FormError(booleanFieldName, "error.required.rp.experiencetraining"))
        }
      }

      s"$booleanFieldName is empty" in {

        val result = form.bind(Map(
          booleanFieldName -> ""
        ))

        result.value shouldBe None
        result.errors shouldBe Seq(FormError(booleanFieldName, "error.required.rp.experiencetraining"))
      }

      s"$stringFieldName is empty when $booleanFieldName is true" in {

        val result = form.bind(Map(
          booleanFieldName -> "true",
          stringFieldName -> ""
        ))

        result.value shouldBe None
        result.errors shouldBe Seq(FormError(stringFieldName, "error.required.rp.experiencetraining.information"))
      }

      s"$stringFieldName is longer than ${formProvider.length} when $booleanFieldName is true" in {

        forAll(stringsLongerThan(formProvider.length).suchThat(_.nonEmpty)) { info =>
          val result = form.bind(Map(
            booleanFieldName -> "true",
            stringFieldName -> info
          ))

          result.value shouldBe None
          result.errors shouldBe Seq(
            FormError(
              stringFieldName,
              "error.rp.invalid.experiencetraining.information.maxlength.255",
              Seq(formProvider.length)
            )
          )
        }
      }

      s"$stringFieldName violates regex when $booleanFieldName is true" in {

        val result = form.bind(Map(
          booleanFieldName -> "true",
          stringFieldName -> "§±@*(&%!£"
        ))

        result.value shouldBe None
        result.errors shouldBe Seq(
          FormError(
            stringFieldName,
            "error.rp.invalid.experiencetraining.information",
            Seq(basicPunctuationRegex)
          )
        )
      }
    }
  }

}
