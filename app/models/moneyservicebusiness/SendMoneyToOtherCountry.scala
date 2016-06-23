package models.moneyservicebusiness

import play.api.data.mapping.{Write, From, Rule}
import play.api.data.mapping.forms._
import play.api.libs.json.Json

case class SendMoneyToOtherCountry(money: Boolean)

object SendMoneyToOtherCountry {

  import utils.MappingUtils.Implicits._

  implicit val format =  Json.format[SendMoneyToOtherCountry]

  implicit val formRule: Rule[UrlFormEncoded, SendMoneyToOtherCountry] = From[UrlFormEncoded] { __ =>
    import play.api.data.mapping.forms.Rules._
    (__ \ "money").read[Boolean].withMessage("error.required.msb.send.money") fmap SendMoneyToOtherCountry.apply
  }

  implicit val formWrites: Write[SendMoneyToOtherCountry, UrlFormEncoded] = Write {x =>
    "money" -> x.money.toString
  }
}



