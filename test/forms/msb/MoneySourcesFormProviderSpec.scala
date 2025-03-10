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

import forms.behaviours.{CheckboxFieldBehaviours, StringFieldBehaviours}
import forms.mappings.Constraints
import models.moneyservicebusiness.MoneySources.{Banks, Customers, Wholesalers}
import models.moneyservicebusiness.{BankMoneySource, MoneySource, MoneySources, WholesalerMoneySource}
import play.api.data.FormError

class MoneySourcesFormProviderSpec extends CheckboxFieldBehaviours with StringFieldBehaviours with Constraints {

  val formProvider = new MoneySourcesFormProvider()
  val form         = formProvider()

  "MoneySourcesFormProvider" must {

    val checkboxFieldName       = "moneySources"
    val bankNameFieldName       = "bankNames"
    val wholesalerNameFieldName = "wholesalerNames"
    val requiredKey             = "error.invalid.msb.wc.moneySources"

    val bankName       = "A Bank Name"
    val wholesalerName = "A Wholesaler Name"

    behave like checkboxFieldWithWrapper[MoneySource, MoneySources](
      form,
      checkboxFieldName,
      validValues = MoneySources.all,
      {
        case Banks       => MoneySources(Some(BankMoneySource(bankName)), None, Some(false))
        case Wholesalers => MoneySources(None, Some(WholesalerMoneySource(wholesalerName)), Some(false))
        case Customers   => MoneySources(None, None, Some(true))
        case _           => fail("Invalid Money Source")
      },
      x =>
        x.toList match {
          case b :: w :: c :: Nil =>
            MoneySources(
              Some(BankMoneySource(bankName)),
              Some(WholesalerMoneySource(wholesalerName)),
              Some(true)
            )
        },
      invalidError = FormError(s"$checkboxFieldName[0]", requiredKey),
      bankNameFieldName -> bankName,
      (wholesalerNameFieldName, wholesalerName)
    )

    behave like mandatoryCheckboxField(
      form,
      checkboxFieldName,
      requiredKey
    )

    s"evaluate $bankNameFieldName field correctly" when {

      "the correct checkbox is checked" in {

        val result = form.bind(
          Map(
            s"$checkboxFieldName[0]" -> Banks.toString,
            bankNameFieldName        -> bankName
          )
        )

        result.value          shouldBe Some(MoneySources(Some(BankMoneySource(bankName)), None, Some(false)))
        result.errors.isEmpty shouldBe true
      }

      s"the correct checkbox is checked BUT $bankNameFieldName is empty" in {

        val result = form.bind(
          Map(
            s"$checkboxFieldName[0]" -> Banks.toString,
            bankNameFieldName        -> ""
          )
        )

        result.value  shouldBe None
        result.errors shouldBe Seq(FormError(bankNameFieldName, s"error.invalid.msb.wc.$bankNameFieldName"))
      }

      s"$bankNameFieldName is longer than ${formProvider.length}" in {

        forAll(stringsLongerThan(formProvider.length)) { longStr =>
          val result = form.bind(
            Map(
              s"$checkboxFieldName[0]" -> Banks.toString,
              bankNameFieldName        -> longStr
            )
          )

          result.value  shouldBe None
          result.errors shouldBe Seq(
            FormError(bankNameFieldName, s"error.maxlength.msb.wc.$bankNameFieldName", Seq(formProvider.length))
          )
        }
      }

      s"$bankNameFieldName violates regex" in {
        val result = form.bind(
          Map(
            s"$checkboxFieldName[0]" -> Banks.toString,
            bankNameFieldName        -> "§!@£$@$@£%"
          )
        )

        result.value  shouldBe None
        result.errors shouldBe Seq(
          FormError(bankNameFieldName, s"error.format.msb.wc.banknames", Seq(basicPunctuationRegex))
        )
      }
    }

    s"evaluate $wholesalerNameFieldName field correctly" when {

      "the correct checkbox is checked" in {

        val result = form.bind(
          Map(
            s"$checkboxFieldName[0]" -> Wholesalers.toString,
            wholesalerNameFieldName  -> wholesalerName
          )
        )

        result.value          shouldBe Some(MoneySources(None, Some(WholesalerMoneySource(wholesalerName)), Some(false)))
        result.errors.isEmpty shouldBe true
      }

      s"the correct checkbox is checked BUT $wholesalerNameFieldName is empty" in {

        val result = form.bind(
          Map(
            s"$checkboxFieldName[0]" -> Wholesalers.toString,
            wholesalerNameFieldName  -> ""
          )
        )

        result.value  shouldBe None
        result.errors shouldBe Seq(FormError(wholesalerNameFieldName, s"error.invalid.msb.wc.$wholesalerNameFieldName"))
      }

      s"$wholesalerNameFieldName is longer than ${formProvider.length}" in {

        forAll(stringsLongerThan(formProvider.length)) { longStr =>
          val result = form.bind(
            Map(
              s"$checkboxFieldName[0]" -> Wholesalers.toString,
              wholesalerNameFieldName  -> longStr
            )
          )

          result.value  shouldBe None
          result.errors shouldBe Seq(
            FormError(wholesalerNameFieldName, s"error.maxlength.msb.wc.wholesaler", Seq(formProvider.length))
          )
        }
      }

      s"$wholesalerNameFieldName violates regex" in {
        val result = form.bind(
          Map(
            s"$checkboxFieldName[0]" -> Wholesalers.toString,
            wholesalerNameFieldName  -> "§!@£$@$@£%"
          )
        )

        result.value  shouldBe None
        result.errors shouldBe Seq(
          FormError(wholesalerNameFieldName, s"error.format.msb.wc.wholesaler", Seq(basicPunctuationRegex))
        )
      }
    }
  }
}
