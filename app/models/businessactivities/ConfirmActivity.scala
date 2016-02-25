package models.businessactivities

import models.aboutthebusiness.VATRegisteredYes
import play.api.data.mapping.forms.Writes
import play.api.data.mapping.{Write, Success, From, Rule}
import play.api.data.mapping.forms._
import play.api.libs.json._

sealed trait involvedInOther

case class involvedInOtherYes(details: String) extends involvedInOther

case object involvedInOtherNo extends involvedInOther

object ConfirmActivity {

  import models.FormTypes._

  implicit val formRule: Rule[UrlFormEncoded, ConfirmActivity] = From[UrlFormEncoded] { __ =>
    import play.api.data.mapping.forms.Rules._
    (__ \ "registeredForVAT").read[Boolean] flatMap {
      case true =>
        (__ \ "vrnNumber").read(OtherBusinessActivityType) fmap (involvedInOtherYes.apply)
      case false => Rule.fromMapping { _ => Success(involvedInOtherNo) }
    }
  }

  implicit val formWrites: Write[ConfirmActivity, UrlFormEncoded] = Write {
    case involvedInOtherYes(value) =>
      Map("registeredForVAT" -> Seq("true"),
        "vrnNumber" -> Seq(value)
      )
    case involvedInOtherNo => Map("registeredForVAT" -> Seq("false"))
  }

  implicit val jsonReads: Reads[ConfirmActivity] =
    (__ \ "registeredForVAT").read[Boolean] flatMap {
      case true => (__ \ "vrnNumber").read[String] map (involvedInOtherYes.apply _)
      case false => Reads(_ => JsSuccess(involvedInOtherNo))
    }

  implicit val jsonWrites = Writes[ConfirmActivity] {
    case involvedInOtherYes(value) => Json.obj(
      "registeredForVAT" -> true,
      "vrnNumber" -> value
    )
    case involvedInOtherNo => Json.obj("registeredForVAT" -> false)
  }


}
