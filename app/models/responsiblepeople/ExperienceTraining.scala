package models.responsiblepeople

import jto.validation._
import jto.validation.forms.Rules._
import models.FormTypes._
import play.api.libs.json.{Writes => _}
import utils.MappingUtils.Implicits._
import jto.validation.forms.UrlFormEncoded
import cats.data.Validated.{Invalid, Valid}


sealed trait ExperienceTraining

case class ExperienceTrainingYes(experienceInformation: String) extends ExperienceTraining

case object ExperienceTrainingNo extends ExperienceTraining

object ExperienceTraining {

  import play.api.libs.json._

  val maxInformationTypeLength = 255

  val experienceInformationType = notEmptyStrip andThen
    notEmpty.withMessage("error.required.rp.experiencetraining.information") andThen
    maxLength(maxInformationTypeLength).withMessage("error.invalid.length.rp.experiencetraining.information") andThen
    basicPunctuationPattern


  implicit val formRule: Rule[UrlFormEncoded, ExperienceTraining] = From[UrlFormEncoded] { __ =>
    import jto.validation.forms.Rules._
    (__ \ "experienceTraining").read[Boolean].withMessage("error.required.rp.experiencetraining") flatMap {
      case true =>
        (__ \ "experienceInformation").read(experienceInformationType) map (ExperienceTrainingYes.apply)
      case false => Rule.fromMapping { _ => Valid(ExperienceTrainingNo) }
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
