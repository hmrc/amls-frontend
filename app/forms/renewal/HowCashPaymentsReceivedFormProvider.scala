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

package forms.renewal

import forms.mappings.Mappings
import models.renewal.PaymentMethods.{Courier, Direct, Other}
import models.renewal.{PaymentMethod, PaymentMethods}
import models.renewal.HowCashPaymentsReceived
import play.api.data.Form
import play.api.data.Forms.{mapping, seq}
import uk.gov.voa.play.form.ConditionalMappings.mandatoryIf

import javax.inject.Inject
import scala.jdk.CollectionConverters._

class HowCashPaymentsReceivedFormProvider @Inject() () extends Mappings {

  private val checkboxError = "error.required.renewal.hvd.choose.option"

  val length = 255 // TODO This looks way too long

  def apply(): Form[HowCashPaymentsReceived] = Form[HowCashPaymentsReceived](
    mapping(
      "paymentMethods" -> seq(enumerable[PaymentMethod](checkboxError, checkboxError)(PaymentMethods.enumerable))
        .verifying(nonEmptySeq(checkboxError)),
      "details"        -> mandatoryIf(
        _.values.asJavaCollection.contains(Other("").toString),
        text("error.required.renewal.hvd.describe").verifying(
          firstError(
            maxLength(length, "error.required.renewal.hvd.describe.invalid.length"),
            regexp(basicPunctuationRegex, "error.required.renewal.hvd.describe.invalid.characters")
          )
        )
      )
    )(apply)(unapply)
  )

  private def apply(paymentMethods: Seq[PaymentMethod], maybeDetails: Option[String]): HowCashPaymentsReceived =
    (paymentMethods.contains(Other("")), maybeDetails) match {
      case (true, Some(detail)) =>
        val modifiedTransactions = paymentMethods.map(method => if (method == Other("")) Other(detail) else method)
        HowCashPaymentsReceived(PaymentMethods(modifiedTransactions, Some(detail)))
      case (false, Some(_))     =>
        throw new IllegalArgumentException("Cannot have method details without Other payment method")
      case (true, None)         => throw new IllegalArgumentException("Cannot have Other payment method without method details")
      case (false, None)        => HowCashPaymentsReceived(PaymentMethods(paymentMethods, None))
    }

  private def unapply(obj: HowCashPaymentsReceived): Option[(Seq[PaymentMethod], Option[String])] = {

    val courier = if (obj.paymentMethods.courier) Some(Courier) else None
    val direct  = if (obj.paymentMethods.direct) Some(Direct) else None
    val other   = if (obj.paymentMethods.other.isDefined) Some(Other("")) else None

    Some((Seq(courier, direct, other).flatten, obj.paymentMethods.other))
  }
}
