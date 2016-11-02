package models.hvd

import play.api.data.mapping.forms.UrlFormEncoded
import play.api.data.mapping._
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
      .withMessage("error.required.hvd.how-will-you-sell-goods").fmap {s =>
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
    import play.api.data.mapping.forms.Rules._
    implicitly
  }

  val formW: Write[HowWillYouSellGoods, UrlFormEncoded] = {
    import play.api.data.mapping.forms.Writes._
    import utils.MappingUtils.writeM
    implicitly
  }

  val jsonR: Reads[HowWillYouSellGoods] = {
    import play.api.data.mapping.json.Rules.{JsValue => _, pickInJson => _, _}
    import utils.JsonMapping._
    implicitly[Reads[HowWillYouSellGoods]]
  }


  val jsonW: Writes[HowWillYouSellGoods] = {
    import play.api.data.mapping.json.Writes._
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
