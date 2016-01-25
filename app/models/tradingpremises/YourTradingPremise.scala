package models.tradingpremises

import models.FormTypes._
import play.api.data.mapping._
import play.api.data.mapping.forms.UrlFormEncoded
import play.api.data.validation.ValidationError
import play.api.libs.json._

sealed trait YourTradingPremises

case object DummyObject  extends YourTradingPremises

object YourTradingPremises {

  implicit val formRule: Rule[UrlFormEncoded, YourTradingPremises] = From[UrlFormEncoded] { __ =>
    import play.api.data.mapping.forms.Rules._
    (__ \ "dummyObject").read[Boolean] flatMap {
      case false => Rule.fromMapping { _ => Success(DummyObject) }
    }
  }

  implicit val formWrites: Write[YourTradingPremises, UrlFormEncoded] = Write {
    case DummyObject => Map("" -> Seq(""))
  }

  implicit val jsonReads =
    (__ \ "dummyObject").read[Boolean] flatMap[YourTradingPremises] {
      case x => Reads(_ => JsSuccess(""))
  }

  implicit val jsonWrites = Writes[YourTradingPremises] {
    case DummyObject => Json.obj("dummyObject" -> false)
  }
}