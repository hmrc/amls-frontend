package models.moneyservicebusiness

import jto.validation.{Write, From, Rule}
import jto.validation.forms._
import play.api.libs.json.Json

case class SendMoneyToOtherCountry(money: Boolean)

object SendMoneyToOtherCountry {

  import utils.MappingUtils.Implicits._

  implicit val format =  Json.format[SendMoneyToOtherCountry]

  implicit val formRule: Rule[UrlFormEncoded, SendMoneyToOtherCountry] = From[UrlFormEncoded] { __ =>
    import jto.validation.forms.Rules._
    (__ \ "money").read[Boolean].withMessage("error.required.msb.send.money") map SendMoneyToOtherCountry.apply
  }

  implicit val formWrites: Write[SendMoneyToOtherCountry, UrlFormEncoded] = Write {x =>
    "money" -> x.money.toString
  }
}



