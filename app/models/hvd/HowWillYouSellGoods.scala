package models.hvd

import play.api.data.mapping.{Success, Rule, Write}
import play.api.data.mapping.forms._
import play.api.libs.json.{Json, Writes, JsSuccess, Reads}

case class HowWillYouSellGoods(channels : Seq[SalesChannel])

object HowWillYouSellGoods {
  implicit val readJ = Read[HowWillYouSellGoods] { __ =>
    JsSuccess(HowWillYouSellGoods(Seq(Auction)))
  }

  implicit val writeJ = Writes { x : HowWillYouSellGoods =>
    Json.obj("howWillYouSellGoods" ->
      x.channels.map {
        case Auction => "Auction"
        case Retail => "Retail"
        case Wholesale => "Wholesale"
      }
    )

  }

  implicit val writeF : Write[HowWillYouSellGoods, UrlFormEncoded] = Write[HowWillYouSellGoods, UrlFormEncoded] {_ => Map[String, Seq[String]]()}
  implicit val readF : Rule[UrlFormEncoded, HowWillYouSellGoods] = Rule[UrlFormEncoded, HowWillYouSellGoods] {_ => Success(HowWillYouSellGoods(Seq(Auction)))}
}