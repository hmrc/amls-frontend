/*
 * Copyright 2021 HM Revenue & Customs
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

import play.api.i18n.{Messages, Lang}
import play.api.Play.current
import play.api.i18n.Messages.Implicits._

sealed trait SalesChannel {
  def getMessage(implicit lang: Lang): String = this match {
    case Retail => Messages("hvd.how-will-you-sell-goods.channels.retail")
    case Wholesale => Messages("hvd.how-will-you-sell-goods.channels.wholesale")
    case Auction => Messages("hvd.how-will-you-sell-goods.channels.auction")
  }
}

case object Retail extends SalesChannel

case object Wholesale extends SalesChannel

case object Auction extends SalesChannel

