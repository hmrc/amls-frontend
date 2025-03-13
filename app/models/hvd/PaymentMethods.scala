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

package models.hvd

import models.{Enumerable, WithName}
import play.api.i18n.Messages
import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.json._
import play.twirl.api.Html
import uk.gov.hmrc.govukfrontend.views.Aliases.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.checkboxes.CheckboxItem

case class PaymentMethods(
  courier: Boolean,
  direct: Boolean,
  other: Option[String]
) {
  def getSummaryMessages(implicit messages: Messages): Seq[String] =
    Seq(
      if (courier) Some(messages("hvd.receiving.option.01")) else None,
      if (direct) Some(messages("hvd.receiving.option.02")) else None,
      other
    ).flatten
}

sealed trait PaymentMethod

object PaymentMethods extends Enumerable.Implicits {

  def apply(methods: Seq[PaymentMethod], detail: Option[String]): PaymentMethods =
    PaymentMethods(
      methods.contains(Courier),
      methods.contains(Direct),
      if (methods.exists(_.isInstanceOf[Other])) detail else None
    )

  case object Courier extends WithName("courier") with PaymentMethod

  case object Direct extends WithName("direct") with PaymentMethod

  case class Other(method: String) extends WithName("other") with PaymentMethod

  val all: Seq[PaymentMethod] = Seq(Courier, Direct, Other(""))

  def formValues(html: Html)(implicit messages: Messages): Seq[CheckboxItem] = all.zipWithIndex map {
    case (method, index) =>
      val conditional = if (method.toString == Other("").toString) Some(html) else None

      CheckboxItem(
        content = Text(messages(s"hvd.receiving.option.0${index + 1}")),
        value = method.toString,
        id = Some(s"paymentMethods_$index"),
        name = Some(s"paymentMethods[$index]"),
        conditionalHtml = conditional
      )
  }

  implicit val enumerable: Enumerable[PaymentMethod] = Enumerable(all.map(v => v.toString -> v): _*)

  implicit val reads: Reads[PaymentMethods] =
    (
      (JsPath \ "courier").read[Boolean] and
        (JsPath \ "direct").read[Boolean] and
        (JsPath \ "other").read[Boolean] and
        (JsPath \ "details").readNullable[String]
    )((courier, direct, _, details) =>
      PaymentMethods(
        courier,
        direct,
        details
      )
    )

  implicit val writes: OWrites[PaymentMethods] = (obj: PaymentMethods) =>
    Json.obj(
      "courier" -> obj.courier,
      "direct"  -> obj.direct,
      "other"   -> obj.other.isDefined,
      "details" -> obj.other
    )
}
