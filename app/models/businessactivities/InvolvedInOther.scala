package models.businessactivities

import play.api.data.mapping._
import play.api.data.mapping.forms.Rules._
import play.api.data.mapping.forms.UrlFormEncoded
import play.api.libs.json._

sealed trait InvolvedInOther

case class InvolvedInOtherYes(details: String) extends InvolvedInOther

case object InvolvedInOtherNo extends InvolvedInOther

object InvolvedInOther {

  import models.FormTypes._
  import utils.MappingUtils.Implicits._

  val OtherBusinessActivityType = notEmptyStrip compose
                                  customNotEmpty("error.required.ba.enter.text") compose
                                  maxLength(maxOtherBusinessActivityTypeLength) //TODO need message for max length


  implicit val formRule: Rule[UrlFormEncoded, InvolvedInOther] = From[UrlFormEncoded] { __ =>
    import play.api.data.mapping.forms.Rules._
    (__ \ "involvedInOther").read[Option[Boolean]] flatMap {
      case Some(true) =>
        (__ \ "details").read(OtherBusinessActivityType) fmap InvolvedInOtherYes.apply
      case Some(false) => Rule.fromMapping { _ => Success(InvolvedInOtherNo) }
      case _ => (Path \ "involvedInOther") -> Seq(ValidationError("error.required.ba.involved.in.other"))
    }
  }

  implicit val formWrites: Write[InvolvedInOther, UrlFormEncoded] = Write {
    case InvolvedInOtherYes(value) =>
      Map("involvedInOther" -> Seq("true"),
        "details" -> Seq(value)
      )
    case involvedInOtherNo => Map("involvedInOther" -> Seq("false"))
  }

  implicit val jsonReads: Reads[InvolvedInOther] =
    (__ \ "involvedInOther").read[Boolean] flatMap {
      case true => (__ \ "details").read[String] map InvolvedInOtherYes.apply
      case false => Reads(_ => JsSuccess(InvolvedInOtherNo))
    }

  implicit val jsonWrites = Writes[InvolvedInOther] {
    case InvolvedInOtherYes(details) => Json.obj(
                                          "involvedInOther" -> true,
                                          "details" -> details
                                        )
    case involvedInOtherNo => Json.obj("involvedInOther" -> false)
  }

}
