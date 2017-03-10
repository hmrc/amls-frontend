package models.tradingpremises

import jto.validation.forms._
import jto.validation.{From, Rule, Write}
import play.api.libs.json.Json

case class IsResidential (isresidential: Boolean)

object IsResidential {

  implicit val format =  Json.format[IsResidential]
  import utils.MappingUtils.Implicits._

  implicit val formRule: Rule[UrlFormEncoded, IsResidential] =
    From[UrlFormEncoded] { __ =>
      import jto.validation.forms.Rules._
    (__ \ "isResidential").read[Boolean].withMessage("tradingpremises.yourtradingpremises.isresidential.required") map IsResidential.apply
  }

  implicit val formWrites = Write[IsResidential, UrlFormEncoded] {
      case IsResidential(value) => Map("isResidential" -> Seq(value.toString))
  }
}