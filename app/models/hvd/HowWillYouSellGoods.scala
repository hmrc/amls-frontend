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

import models.hvd.SalesChannel._
import play.api.libs.json._

case class HowWillYouSellGoods(channels: Set[SalesChannel])

trait HowWillYouSellGoods0 {

  implicit val jsonReads: Reads[HowWillYouSellGoods] =
    (__ \ "salesChannels").read[Set[String]].flatMap { x: Set[String] =>
      x.map {
        case "Retail"    => Reads(_ => JsSuccess(Retail)) map identity[SalesChannel]
        case "Wholesale" => Reads(_ => JsSuccess(Wholesale)) map identity[SalesChannel]
        case "Auction"   => Reads(_ => JsSuccess(Auction)) map identity[SalesChannel]
      }.foldLeft[Reads[Set[SalesChannel]]](
        Reads[Set[SalesChannel]](_ => JsSuccess(Set.empty))
      ) { (result, data) =>
        data flatMap { m =>
          result.map { n =>
            n + m
          }
        }
      }
    } map HowWillYouSellGoods.apply

  val jsonW: Writes[HowWillYouSellGoods] =
    (__ \ "salesChannels").write[Seq[String]].contramap { hwysg: HowWillYouSellGoods =>
      hwysg.channels.toSeq.map {
        case Retail    => "Retail"
        case Wholesale => "Wholesale"
        case Auction   => "Auction"
      }
    }
}

object HowWillYouSellGoods {
  private object Cache extends HowWillYouSellGoods0

  implicit val jsonR: Reads[HowWillYouSellGoods]  = Cache.jsonReads
  implicit val jsonW: Writes[HowWillYouSellGoods] = Cache.jsonW
}
