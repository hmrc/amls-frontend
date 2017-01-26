package models.estateagentbusiness

import jto.validation.forms.Rules._
import jto.validation.forms.UrlFormEncoded
import jto.validation.{From, Rule, Success, Write}
import play.api.libs.json._

sealed trait PenalisedUnderEstateAgentsAct

case class PenalisedUnderEstateAgentsActYes(value: String) extends PenalisedUnderEstateAgentsAct

case object PenalisedUnderEstateAgentsActNo extends PenalisedUnderEstateAgentsAct

object PenalisedUnderEstateAgentsAct {

  import utils.MappingUtils.Implicits._

  val maxPenalisedTypeLength = 255
  val penalisedType = notEmpty.withMessage("error.required.eab.info.about.penalty") andThen
    maxLength(maxPenalisedTypeLength).withMessage("error.invalid.eab.info.about.penalty")

  implicit val formRule: Rule[UrlFormEncoded, PenalisedUnderEstateAgentsAct] = From[UrlFormEncoded] { __ =>
    import jto.validation.forms.Rules._

    (__ \ "penalisedUnderEstateAgentsAct").read[Boolean].withMessage("error.required.eab.penalised.under.act") flatMap {
      case true => (__ \ "penalisedUnderEstateAgentsActDetails").read(penalisedType) fmap PenalisedUnderEstateAgentsActYes.apply
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
      case true => (__ \ "penalisedUnderEstateAgentsActDetails").read[String] map PenalisedUnderEstateAgentsActYes.apply
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
