package models.responsiblepeople

import play.api.data.mapping._
import play.api.data.mapping.forms.Rules._
import play.api.libs.json.{Writes => _}
import utils.MappingUtils.Implicits._
import play.api.data.mapping.forms.UrlFormEncoded


sealed trait ExperienceTraining

case class ExperienceTrainingYes(experienceInformation: String) extends ExperienceTraining

case object ExperienceTrainingNo extends ExperienceTraining

object ExperienceTraining {

  import play.api.libs.json._

  val maxInformationTypeLength = 255

  val experienceInformationType = notEmpty.withMessage("error.required.rp.experiencetraining.experienceInformation") compose
    maxLength(maxInformationTypeLength).withMessage("error.invalid.length.rp.experiencetraining.information")


  implicit val formRule: Rule[UrlFormEncoded, ExperienceTraining] = From[UrlFormEncoded] { __ =>
    import play.api.data.mapping.forms.Rules._
    (__ \ "experienceTraining").read[Boolean].withMessage("error.required.rp.experienceTraining") flatMap {
      case true =>
        (__ \ "experienceInformation").read(experienceInformationType) fmap (ExperienceTrainingYes.apply)
      case false => Rule.fromMapping { _ => Success(ExperienceTrainingNo) }
    }
  }

  implicit val formWrites: Write[ExperienceTraining, UrlFormEncoded] = Write {
    case a: ExperienceTrainingYes => Map(
      "experienceTraining" -> Seq("true"),
      "experienceInformation" -> Seq(a.experienceInformation)
    )
    case ExperienceTrainingNo => Map("experienceTraining" -> Seq("false"))
  }

  implicit val jsonReads: Reads[ExperienceTraining] =
    (__ \ "experienceTraining").read[Boolean] flatMap {
      case true => (__ \ "experienceInformation").read[String] map (ExperienceTrainingYes.apply _)
      case false => Reads(_ => JsSuccess(ExperienceTrainingNo))
    }

  implicit val jsonWrites = Writes[ExperienceTraining] {
    case ExperienceTrainingYes(information) => Json.obj(
      "experienceTraining" -> true,
      "experienceInformation" -> information
    )
    case ExperienceTrainingNo => Json.obj("experienceTraining" -> false)
  }

}
