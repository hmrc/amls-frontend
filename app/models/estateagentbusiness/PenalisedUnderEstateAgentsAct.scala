package models.estateagentbusiness

import play.api.data.mapping.forms.UrlFormEncoded
import play.api.data.mapping.{From, Rule, Success, Write}
import play.api.libs.json._

sealed trait PenalisedUnderEstateAgentsAct

case class PenalisedUnderEstateAgentsActYes(value: String) extends PenalisedUnderEstateAgentsAct

case object PenalisedUnderEstateAgentsActNo extends PenalisedUnderEstateAgentsAct

object PenalisedUnderEstateAgentsAct {

  import models.FormTypes._

  implicit val formRule: Rule[UrlFormEncoded, PenalisedUnderEstateAgentsAct] = From[UrlFormEncoded] { __ =>
    import play.api.data.mapping.forms.Rules._
    (__ \ "penalisedUnderEstateAgentsAct").read[Boolean] flatMap {
      case true => (__ \ "penalisedUnderEstateAgentsActDetails").read(penalisedType) fmap (PenalisedUnderEstateAgentsActYes.apply)
      case false => Rule.fromMapping { _ => Success(PenalisedUnderEstateAgentsActNo) }
    }
  }

  implicit val formWrites: Write[PenalisedUnderEstateAgentsAct, UrlFormEncoded] = Write {
    case PenalisedUnderEstateAgentsActYes(value) =>
      Map("penalisedUnderEstateAgentsAct" -> Seq("true"),
        "penalisedUnderEstateAgentsActDetails" -> Seq(value)
      )
    case PenalisedUnderEstateAgentsActNo => Map("penalisedUnderEstateAgentsAct" -> Seq("false"))
  }

  implicit val jsonReads: Reads[PenalisedUnderEstateAgentsAct] =
    (__ \ "penalisedUnderEstateAgentsAct").read[Boolean] flatMap {
      case true => (__ \ "penalisedUnderEstateAgentsActDetails").read[String] map (PenalisedUnderEstateAgentsActYes.apply _)
      case false => Reads(_ => JsSuccess(PenalisedUnderEstateAgentsActNo))
    }

  implicit val jsonWrites = Writes[PenalisedUnderEstateAgentsAct] {
    case PenalisedUnderEstateAgentsActYes(value) => Json.obj(
      "penalisedUnderEstateAgentsAct" -> true,
      "penalisedUnderEstateAgentsActDetails" -> value
    )
    case PenalisedUnderEstateAgentsActNo => Json.obj("penalisedUnderEstateAgentsAct" -> false)
  }

}
