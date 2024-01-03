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

package models.payments

import models.{Enumerable, WithName}
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.radios.RadioItem

sealed trait WaysToPay

object WaysToPay extends Enumerable.Implicits {

  case object Card extends WithName("card") with WaysToPay
  case object Bacs extends WithName("bacs") with WaysToPay

  val all: Seq[WaysToPay] = Seq(Bacs, Card)

  def formValues(implicit messages: Messages): Seq[RadioItem] = all.zipWithIndex.map { case (wayToPay, index) =>
    RadioItem(
      content = Text(messages(s"payments.waystopay.${wayToPay.toString}")),
      value = Some(wayToPay.toString),
      id = Some(s"waysToPay_$index")
    )
  }

  implicit val enumerable: Enumerable[WaysToPay] = Enumerable(all.map(v => v.toString -> v): _*)
}
