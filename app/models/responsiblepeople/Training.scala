package models.responsiblepeople

import play.api.data.mapping._
import play.api.data.mapping.forms.Rules._
import play.api.libs.json.{Writes => _}
import utils.MappingUtils.Implicits._
import play.api.data.mapping.forms.UrlFormEncoded


sealed trait Training

case class HadTrainingYes(information: String) extends Training

case object HadTrainingNo extends Training

object Training {

  import play.api.libs.json._

  val maxInformationTypeLength = 255

  val informationType = notEmpty.withMessage("error.required.rp.training.information") compose
    maxLength(maxInformationTypeLength).withMessage("error.invalid.length.rp.training.information")


  implicit val formRule: Rule[UrlFormEncoded, Training] = From[UrlFormEncoded] { __ =>
    import play.api.data.mapping.forms.Rules._
    (__ \ "hadTraining").read[Boolean].withMessage("error.required.rp.training") flatMap {
      case true =>
        (__ \ "information").read(informationType) fmap (HadTrainingYes.apply)
      case false => Rule.fromMapping { _ => Success(HadTrainingNo) }
    }
  }

  implicit val formWrites: Write[Training, UrlFormEncoded] = Write {
    case a: HadTrainingYes => Map(
      "hadTraining" -> Seq("true"),
      "information" -> Seq(a.information)
    )
    case HadTrainingNo => Map("hadTraining" -> Seq("false"))
  }

  implicit val jsonReads: Reads[Training] =
    (__ \ "hadTraining").read[Boolean] flatMap {
      case true => (__ \ "information").read[String] map (HadTrainingYes.apply _)
      case false => Reads(_ => JsSuccess(HadTrainingNo))
    }

  implicit val jsonWrites = Writes[Training] {
    case HadTrainingYes(information) => Json.obj(
      "hadTraining" -> true,
      "information" -> information
    )
    case HadTrainingNo => Json.obj("hadTraining" -> false)
  }

}
