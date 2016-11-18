package models

import play.api.data.mapping._
import play.api.data.mapping.forms.Rules._
import play.api.data.mapping.forms.UrlFormEncoded
import play.api.data.validation.ValidationError
import play.api.libs.json._

sealed trait SatisfactionSurvey

object SatisfactionSurvey {

  case class First(details: Option[String]) extends SatisfactionSurvey
  case class Second(details: Option[String]) extends SatisfactionSurvey
  case class Third(details: Option[String]) extends SatisfactionSurvey
  case class Fourth(details: Option[String]) extends SatisfactionSurvey
  case class Fifth(details: Option[String]) extends SatisfactionSurvey

  import utils.MappingUtils.Implicits._

  val maxDetailsLength = 1200
  val detailsRule = maxLength(maxDetailsLength).withMessage("error.invalid.maxlength.1200")

  implicit val formRule: Rule[UrlFormEncoded, SatisfactionSurvey] = From[UrlFormEncoded] { __ =>
    import play.api.data.mapping.forms.Rules._
    import models.FormTypes._
    (__ \ "satisfaction").read[String].withMessage("error.survey.satisfaction.required") flatMap {
      case "01" => (__ \ "details").read(optionR(detailsRule)) fmap First.apply
      case "02" => (__ \ "details").read(optionR(detailsRule)) fmap Second.apply
      case "03" => (__ \ "details").read(optionR(detailsRule)) fmap Third.apply
      case "04" => (__ \ "details").read(optionR(detailsRule)) fmap Fourth.apply
      case "05" => (__ \ "details").read(optionR(detailsRule)) fmap Fifth.apply
      case _ =>
        (Path \ "satisfaction") -> Seq(ValidationError("error.invalid"))
    }

  }

  implicit val jsonReads = {
    import play.api.libs.json.Reads.StringReads
    (__ \ "satisfaction").read[String].flatMap[SatisfactionSurvey] {
      case "01" => (__ \ "details").readNullable[String] map First.apply
      case "02" => (__ \ "details").readNullable[String] map Second.apply
      case "03" => (__ \ "details").readNullable[String] map Third.apply
      case "04" => (__ \ "details").readNullable[String] map Fourth.apply
      case "05" => (__ \ "details").readNullable[String] map Fifth.apply
      case _ =>
        ValidationError("error.invalid")
    }
  }

  implicit val jsonWrites =  Writes[SatisfactionSurvey] {
    case First(details) => Json.obj(
      "satisfaction" -> "01",
      "details" -> details.fold(""){x => x.toString}
    )
    case Second(details) => Json.obj(
      "satisfaction" -> "02",
      "details" -> details.fold(""){x => x.toString}
    )
    case Third(details) => Json.obj(
      "satisfaction" -> "03",
      "details" -> details.fold(""){x => x.toString}
    )
    case Fourth(details) => Json.obj(
      "satisfaction" -> "04",
      "details" -> details.fold(""){x => x.toString}
    )
    case Fifth(details) => Json.obj(
      "satisfaction" -> "05",
      "details" -> details.fold(""){x => x.toString}
    )
  }

}
