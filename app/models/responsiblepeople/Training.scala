package models.responsiblepeople

import jto.validation._
import jto.validation.forms.Rules._
import play.api.libs.json.{Writes => _}
import utils.MappingUtils.Implicits._
import jto.validation.forms.UrlFormEncoded


sealed trait Training

case class TrainingYes(information: String) extends Training

case object TrainingNo extends Training

object Training {

  import play.api.libs.json._

  val maxInformationTypeLength = 255

  val informationType = notEmpty.withMessage("error.required.rp.training.information") andThen
    maxLength(maxInformationTypeLength).withMessage("error.invalid.length.rp.training.information")


  implicit val formRule: Rule[UrlFormEncoded, Training] = From[UrlFormEncoded] { __ =>
    import jto.validation.forms.Rules._
    (__ \ "training").read[Boolean].withMessage("error.required.rp.training") flatMap {
      case true =>
        (__ \ "information").read(informationType) fmap (TrainingYes.apply)
      case false => Rule.fromMapping { _ => Success(TrainingNo) }
    }
  }

  implicit val formWrites: Write[Training, UrlFormEncoded] = Write {
    case a: TrainingYes => Map(
      "training" -> Seq("true"),
      "information" -> Seq(a.information)
    )
    case TrainingNo => Map("training" -> Seq("false"))
  }

  implicit val jsonReads: Reads[Training] =
    (__ \ "training").read[Boolean] flatMap {
      case true => (__ \ "information").read[String] map (TrainingYes.apply _)
      case false => Reads(_ => JsSuccess(TrainingNo))
    }

  implicit val jsonWrites = Writes[Training] {
    case TrainingYes(information) => Json.obj(
      "training" -> true,
      "information" -> information
    )
    case TrainingNo => Json.obj("training" -> false)
  }

}
