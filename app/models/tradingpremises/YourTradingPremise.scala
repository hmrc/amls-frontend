package models.tradingpremises

import models.FormTypes._
import play.api.data.mapping._
import play.api.data.mapping.forms.UrlFormEncoded
import play.api.data.validation.ValidationError
import play.api.libs.json._

case class YourTradingPremises(name:String, city:String)

object YourTradingPremises {

  implicit val formRule: Rule[UrlFormEncoded, YourTradingPremises] =
    From[UrlFormEncoded] { __ =>
      import models.FormTypes._
      import play.api.data.mapping.forms.Rules._
      (
        (__ \ "name").read[String] and
          (__ \ "city").read[String]
        )(YourTradingPremises.apply _)
    }

  implicit val formWrites: Write[YourTradingPremises, UrlFormEncoded] = To[UrlFormEncoded] { __ =>
    import play.api.data.mapping.forms.Writes._
    import play.api.libs.functional.syntax.unlift
    (
      (__ \ "name").write[String] and
        (__ \ "city").write[String]
      ) (unlift(YourTradingPremises.unapply _))
  }

  implicit val formats = Json.format[YourTradingPremises]
}