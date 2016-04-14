package models.responsiblepeople

import play.api.data.mapping._
import play.api.data.mapping.forms.Rules._
import play.api.data.mapping.forms._
import play.api.libs.json.{Writes => _}
import utils.MappingUtils.Implicits._
import play.api.data.mapping.forms.UrlFormEncoded
import play.api.libs.json._


sealed trait Training

case class HadTrainingYes(datesandduration: String) extends Training

case object HadTrainingNo extends Training

object Training {

  import play.api.libs.json._

  val maxNameTypeLength = 35

  val otherFirstNameType = notEmpty.withMessage("error.required.datesandduration") compose
    maxLength(maxNameTypeLength).withMessage("error.invalid.length.firstname")


  implicit val formRule: Rule[UrlFormEncoded, Training] = From[UrlFormEncoded] { __ =>
    import play.api.data.mapping.forms.Rules._
    (__ \ "hadTraining").read[Boolean].withMessage("error.required.sa.registration") flatMap {
      case true =>
        (__ \ "datesandduration").read(otherFirstNameType) fmap (HadTrainingYes.apply)
      case false => Rule.fromMapping { _ => Success(HadTrainingNo) }
    }
  }

  implicit val formWrites: Write[Training, UrlFormEncoded] = Write {
    case a: HadTrainingYes => Map(
      "hadTraining" -> Seq("true"),
      "datesandduration" -> Seq(a.datesandduration)
    )
    case HadTrainingNo => Map("hadTraining" -> Seq("false"))
  }

  implicit val jsonReads: Reads[Training] =
    (__ \ "hadTraining").read[Boolean] flatMap {
      case true => (__ \ "datesandduration").read[String] map (HadTrainingYes.apply _)
      case false => Reads(_ => JsSuccess(HadTrainingNo))
    }

  implicit val jsonWrites = Writes[Training] {
    case HadTrainingYes(datesandduration) => Json.obj(
      "hadTraining" -> true,
      "datesandduration" -> datesandduration
    )
    case HadTrainingNo => Json.obj("hadTraining" -> false)
  }

}
