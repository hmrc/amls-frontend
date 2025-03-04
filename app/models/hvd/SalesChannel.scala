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
import uk.gov.hmrc.govukfrontend.views.Aliases.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.checkboxes.CheckboxItem

sealed trait SalesChannel {
  import SalesChannel._
  def getMessage(implicit messages: Messages): String = this match {
    case Retail    => messages("hvd.how-will-you-sell-goods.channels.retail")
    case Wholesale => messages("hvd.how-will-you-sell-goods.channels.wholesale")
    case Auction   => messages("hvd.how-will-you-sell-goods.channels.auction")
  }
}

object SalesChannel extends Enumerable.Implicits {
  case object Retail extends WithName("retail") with SalesChannel

  case object Wholesale extends WithName("wholesale") with SalesChannel

  case object Auction extends WithName("auction") with SalesChannel

  val all: Seq[SalesChannel] = Seq(Auction, Retail, Wholesale)

  def formValues(implicit messages: Messages): Seq[CheckboxItem] = all.zipWithIndex.map { case (salesChannel, index) =>
    CheckboxItem(
      content = Text(salesChannel.getMessage),
      value = salesChannel.toString,
      id = Some(s"salesChannels_$index"),
      name = Some(s"salesChannels[$index]")
    )
  }

  implicit val enumerable: Enumerable[SalesChannel] = Enumerable(all.map(v => v.toString -> v): _*)
}
