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

package forms.tradingpremises

import forms.behaviours.StringFieldBehaviours
import forms.mappings.Constraints
import models.tradingpremises.AgentRemovalReason
import models.tradingpremises.AgentRemovalReason.Other
import models.tradingpremises.RemovalReasonConstants.Rules
import org.scalacheck.Gen
import play.api.data.{Form, FormError}

class RemoveAgentPremisesReasonsFormProviderSpec extends StringFieldBehaviours with Constraints {

  val formProvider                   = new RemoveAgentPremisesReasonsFormProvider()
  val form: Form[AgentRemovalReason] = formProvider()

  val radioFieldName = "removalReason"
  val radioErrorKey  = "tradingpremises.remove_reasons.missing"

  val textFieldName = "removalReasonOther"

  "RemoveAgentPremisesReasonsFormProvider" when {

    s"$radioFieldName is validated" must {

      behave like fieldThatBindsValidData(
        form,
        radioFieldName,
        Gen.oneOf[String](AgentRemovalReason.all.filterNot(_ == Other).map(_.toString))
      )

      s"only bind Other when $textFieldName is populated with valid data" in {

        forAll(stringsShorterThan(formProvider.length).suchThat(_.nonEmpty)) { other =>
          val result = form.bind(
            Map(
              radioFieldName -> Other.toString,
              textFieldName  -> other
            )
          )

          result.value shouldBe Some(AgentRemovalReason(Rules.toSchemaReason(Other.value), Some(other)))
          assert(result.errors.isEmpty)
        }
      }

      behave like mandatoryField(form, radioFieldName, FormError(radioFieldName, radioErrorKey))

      "fail to bind invalid values" in {

        forAll(Gen.alphaNumStr) { invalidAnswer =>
          val result = form.bind(Map(radioFieldName -> invalidAnswer))

          result.value  shouldBe None
          result.errors shouldBe Seq(FormError(radioFieldName, radioErrorKey))
        }
      }
    }

    s"$textFieldName is validated" must {

      "fail to bind if Other is selected and field is empty" in {

        val result = form.bind(
          Map(
            radioFieldName -> Other.toString,
            textFieldName  -> ""
          )
        )

        result.value  shouldBe None
        result.errors shouldBe Seq(FormError(textFieldName, "tradingpremises.remove_reasons.agent.other.missing"))
      }

      s"fail to bind if Other is selected and field exceeds ${formProvider.length}" in {

        forAll(stringsLongerThan(formProvider.length).suchThat(_.nonEmpty)) { invalid =>
          val result = form.bind(
            Map(
              radioFieldName -> Other.toString,
              textFieldName  -> invalid
            )
          )

          result.value  shouldBe None
          result.errors shouldBe Seq(FormError(textFieldName, "error.invalid.maxlength.255", Seq(formProvider.length)))
        }
      }

      "fail to bind if regex is violated" in {

        forAll(invalidCharForNames.suchThat(_.nonEmpty)) { invalid =>
          val result = form.bind(
            Map(
              radioFieldName -> Other.toString,
              textFieldName  -> invalid
            )
          )

          result.value  shouldBe None
          result.errors shouldBe Seq(FormError(textFieldName, "err.text.validation", Seq(basicPunctuationRegex)))
        }
      }
    }
  }
}
