package models.asp

import play.api.data.mapping._
import play.api.data.mapping.forms.Rules._
import play.api.libs.json.{Writes => _}
import utils.MappingUtils.Implicits._
import play.api.data.mapping.forms.UrlFormEncoded

sealed trait OtherBusinessTaxMatters

case class OtherBusinessTaxMattersYes(agentRegNo: String) extends OtherBusinessTaxMatters

case object OtherBusinessTaxMattersNo extends OtherBusinessTaxMatters

object OtherBusinessTaxMatters {

  import play.api.libs.json._

  val maxAgentRegNoLength = 11
  val agentRegNoPattern =  "^([0-9]{11})$".r

  val agentRegNoType = notEmpty.withMessage("error.required.asp.agentRegNo") compose
    maxLength(maxAgentRegNoLength).withMessage("error.invalid.length.asp.agentRegNo") compose
    pattern(agentRegNoPattern).withMessage("error.invalid.asp.agentRegNo")

  implicit val formRule: Rule[UrlFormEncoded, OtherBusinessTaxMatters] = From[UrlFormEncoded] { __ =>
    import play.api.data.mapping.forms.Rules._
    (__ \ "otherBusinessTaxMatters").read[Boolean].withMessage("error.required.asp.other.business.tax.matters") flatMap {
      case true =>
        (__ \ "agentRegNo").read(agentRegNoType) fmap (OtherBusinessTaxMattersYes.apply)
      case false => Rule.fromMapping { _ => Success(OtherBusinessTaxMattersNo) }
    }
  }

  implicit val formWrites: Write[OtherBusinessTaxMatters, UrlFormEncoded] = Write {
    case OtherBusinessTaxMattersYes(value) => Map(
      "otherBusinessTaxMatters" -> Seq("true"),
      "agentRegNo" -> Seq(value)
    )
    case OtherBusinessTaxMattersNo => Map("otherBusinessTaxMatters" -> Seq("false"))
  }

  implicit val jsonReads: Reads[OtherBusinessTaxMatters] =
    (__ \ "otherBusinessTaxMatters").read[Boolean] flatMap {
      case true => (__ \ "agentRegNo").read[String] map OtherBusinessTaxMattersYes.apply
      case false => Reads(__ => JsSuccess(OtherBusinessTaxMattersNo))
    }

  implicit val jsonWrites = Writes[OtherBusinessTaxMatters] {
    case OtherBusinessTaxMattersYes(value) => Json.obj(
          "otherBusinessTaxMatters" -> true,
          "agentRegNo" -> value
    )
    case OtherBusinessTaxMattersNo => Json.obj("otherBusinessTaxMatters" -> false)
  }
}
