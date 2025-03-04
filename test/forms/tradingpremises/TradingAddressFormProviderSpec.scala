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

import forms.behaviours.AddressFieldBehaviours
import forms.mappings.Constraints
import models.tradingpremises.YourTradingPremises
import org.scalacheck.Gen
import play.api.data.{Form, FormError}

import scala.collection.mutable.{Map => MutableMap}

class TradingAddressFormProviderSpec extends AddressFieldBehaviours with Constraints {

  val formProvider = new TradingAddressFormProvider()

  override val form: Form[YourTradingPremises] = formProvider()

  override val maxLength: Int = formProvider.length

  override val regexString: String = formProvider.addressTypeRegex

  val tradingNameField = "tradingName"

  val tradingNameGen: Gen[String] = validAddressLineGen(formProvider.tradingNameLength).suchThat(_.nonEmpty)

  "CorrespondenceAddressUKFormProvider" when {

    "tradingName is validated" must {

      "bind when valid data is submitted" in {

        forAll(tradingNameGen) { name =>
          val formData = addressLinesData += (tradingNameField -> name)
          val result   = bindForm(formData)(tradingNameField)

          result.value  shouldBe Some(name)
          result.errors shouldBe Nil
        }
      }

      behave like mandatoryField(
        form,
        tradingNameField,
        FormError(tradingNameField, "error.required.tp.trading.name")
      )

      s"not bind strings longer than ${formProvider.tradingNameLength} characters" in {

        forAll(Gen.alphaStr.suchThat(_.length > formProvider.tradingNameLength)) { string =>
          val formData: MutableMap[String, String] = addressLinesData += (tradingNameField -> string)
          val newForm                              = bindForm(formData)

          newForm(tradingNameField).errors shouldEqual Seq(
            FormError(tradingNameField, "error.invalid.tp.trading.name", Seq(formProvider.tradingNameLength))
          )
        }
      }

      "not bind strings that violate regex" in {

        forAll(tradingNameGen, invalidCharForNames.suchThat(_.nonEmpty)) { (line, invalidChar) =>
          val invalidLine                          = line.dropRight(1) + invalidChar
          val formData: MutableMap[String, String] = addressLinesData += (tradingNameField -> invalidLine)
          val newForm                              = bindForm(formData)

          newForm(tradingNameField).errors shouldEqual Seq(
            FormError(tradingNameField, "error.invalid.char.tp.agent.company.details", Seq(basicPunctuationRegex))
          )
        }
      }
    }

    behave like formWithAddressFields(
      "error.required.address",
      "error.max.length.address",
      "error.text.validation.address"
    )

    behave like postcodeField(postcodeRegex)
  }

}
