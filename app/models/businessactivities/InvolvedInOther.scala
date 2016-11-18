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

  val maxOtherBusinessActivityTypeLength = 255
  val OtherBusinessActivityType = notEmptyStrip compose
                                  notEmpty.withMessage("error.required.ba.involved.in.other.text") compose
                                  maxLength(maxOtherBusinessActivityTypeLength).withMessage("error.invalid.maxlength.255")


  implicit val formRule: Rule[UrlFormEncoded, InvolvedInOther] = From[UrlFormEncoded] { __ =>
    import play.api.data.mapping.forms.Rules._
    (__ \ "involvedInOther").read[Boolean].withMessage("error.required.ba.involved.in.other") flatMap {
      case true =>
        (__ \ "details").read(OtherBusinessActivityType) fmap InvolvedInOtherYes.apply
      case false => Rule.fromMapping { _ => Success(InvolvedInOtherNo) }
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
