/*
 * Copyright 2018 HM Revenue & Customs
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

import jto.validation.forms.UrlFormEncoded
import jto.validation._
import play.api.libs.json.{Writes, Reads}
import utils.TraversableValidators
import utils.MappingUtils.Implicits._


case class HowWillYouSellGoods(channels : Seq[SalesChannel])

trait HowWillYouSellGoods0 {
  private implicit def rule[A]
  (implicit
   a : Path => RuleLike[A, Seq[String]]
  ) = From[A] { __ =>
    (__ \ "salesChannels")
      .read(TraversableValidators.minLengthR[Seq[String]](1))
      .withMessage("error.required.hvd.how-will-you-sell-goods").map {s =>
        HowWillYouSellGoods(s.map{str =>
          val sc:SalesChannel = str match {
            case "Retail" => Retail
            case "Wholesale" => Wholesale
            case "Auction" => Auction
          }
          sc
      })}
  }

  private implicit def write[A]
  (implicit
    a : Path => WriteLike[Seq[String], A])= To[A] { __ =>
    (__ \ "salesChannels").write[Seq[String]].contramap { hwysg:HowWillYouSellGoods =>
      hwysg.channels.map {
        case Retail => "Retail"
        case Wholesale => "Wholesale"
        case Auction => "Auction"
      }
    }
  }

  val formR: Rule[UrlFormEncoded, HowWillYouSellGoods] = {
    import jto.validation.forms.Rules._
    implicitly
  }

  val formW: Write[HowWillYouSellGoods, UrlFormEncoded] = {
    import jto.validation.forms.Writes._
    import utils.MappingUtils.writeM
    implicitly
  }

  val jsonR: Reads[HowWillYouSellGoods] = {
    import jto.validation.playjson.Rules.{JsValue => _, pickInJson => _, _}
    import utils.JsonMapping._
    implicitly[Reads[HowWillYouSellGoods]]
  }


  val jsonW: Writes[HowWillYouSellGoods] = {
    import jto.validation.playjson.Writes._
    import utils.JsonMapping._
    implicitly[Writes[HowWillYouSellGoods]]

  }
}

object HowWillYouSellGoods {
  private object Cache extends HowWillYouSellGoods0

  implicit val formW: Write[HowWillYouSellGoods, UrlFormEncoded] = Cache.formW
  implicit val formR: Rule[UrlFormEncoded, HowWillYouSellGoods] = Cache.formR
  implicit val jsonR: Reads[HowWillYouSellGoods] = Cache.jsonR
  implicit val jsonW: Writes[HowWillYouSellGoods] = Cache.jsonW
}
