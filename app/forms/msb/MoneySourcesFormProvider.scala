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

import forms.mappings.Mappings
import models.moneyservicebusiness.MoneySources.{Banks, Customers, Wholesalers}
import models.moneyservicebusiness.{BankMoneySource, MoneySource, MoneySources, WholesalerMoneySource}
import play.api.data.Form
import play.api.data.Forms.{mapping, seq}
import uk.gov.voa.play.form.ConditionalMappings.mandatoryIf

import javax.inject.Inject
import scala.jdk.CollectionConverters._

class MoneySourcesFormProvider @Inject() () extends Mappings {

  val length = 140

  private val emptyError          = "error.invalid.msb.wc.moneySources"
  private val emptyTextPrefix     = "error.invalid.msb.wc"
  private val maxLengthTextPrefix = "error.maxlength.msb.wc"
  private val formatTextPrefix    = "error.format.msb.wc"

  def apply(): Form[MoneySources] = Form[MoneySources](
    mapping(
      "moneySources"    -> seq(enumerable[MoneySource](emptyError, emptyError)(MoneySources.enumerable))
        .verifying(nonEmptySeq(emptyError)),
      "bankNames"       -> mandatoryIf(
        _.values.asJavaCollection.contains(Banks.toString),
        text(s"$emptyTextPrefix.bankNames").verifying(
          firstError(
            maxLength(length, s"$maxLengthTextPrefix.bankNames"),
            regexp(basicPunctuationRegex, s"$formatTextPrefix.banknames")
          )
        )
      ),
      "wholesalerNames" -> mandatoryIf(
        _.values.asJavaCollection.contains(Wholesalers.toString),
        text(s"$emptyTextPrefix.wholesalerNames").verifying(
          firstError(
            maxLength(length, s"$maxLengthTextPrefix.wholesaler"),
            regexp(basicPunctuationRegex, s"$formatTextPrefix.wholesaler")
          )
        )
      )
    )(apply)(unapply)
  )

  private def apply(
    checkboxes: Seq[MoneySource],
    bankNames: Option[String],
    wholesalerNames: Option[String]
  ): MoneySources =
    MoneySources(
      bankNames.map(BankMoneySource),
      wholesalerNames.map(WholesalerMoneySource),
      if (checkboxes.contains(Customers)) Some(true) else Some(false)
    )

  private def unapply(obj: MoneySources): Option[(Seq[MoneySource], Option[String], Option[String])] =
    Some(
      (
        obj.toFormValues,
        obj.bankMoneySource.map(_.bankNames),
        obj.wholesalerMoneySource.map(_.wholesalerNames)
      )
    )
}
