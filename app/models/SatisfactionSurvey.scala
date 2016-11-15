package models

import play.api.data.mapping._
import play.api.data.mapping.forms.Rules._
import play.api.data.mapping.forms.UrlFormEncoded
import play.api.data.validation.ValidationError
import play.api.libs.json._

sealed trait SatisfactionSurvey

object SatisfactionSurvey {

  case class First(details: String) extends SatisfactionSurvey
  case class Second(details: String) extends SatisfactionSurvey
  case class Third(details: String) extends SatisfactionSurvey
  case class Fourth(details: String) extends SatisfactionSurvey
  case class Fifth(details: String) extends SatisfactionSurvey

  import utils.MappingUtils.Implicits._

  val maxDetailsLength = 255
  val detailsRule = maxLength(maxDetailsLength).withMessage("error.invalid")

  implicit val formRule: Rule[UrlFormEncoded, SatisfactionSurvey] = From[UrlFormEncoded] { __ =>
    import play.api.data.mapping.forms.Rules._
    import models.FormTypes._
    (__ \ "satisfaction").read[String].withMessage("error.invalid") flatMap {
      case "01" => (__ \ "details").read(detailsRule) fmap First.apply
      case "02" => (__ \ "details").read(detailsRule) fmap Second.apply
      case "03" => (__ \ "details").read(detailsRule) fmap Third.apply
      case "04" => (__ \ "details").read(detailsRule) fmap Fourth.apply
      case "05" => (__ \ "details").read(detailsRule) fmap Fifth.apply
      case _ =>
        (Path \ "satisfaction") -> Seq(ValidationError("error.invalid"))
    }

  }

  implicit val formWrites: Write[SatisfactionSurvey, UrlFormEncoded] = Write {
    case First(details) => Map(
      "satisfaction" -> Seq("01"),
      "details" -> Seq(details)
    )
    case Second(details) => Map(
      "satisfaction" -> Seq("02"),
      "details" -> Seq(details)
    )
    case Third(details) => Map(
      "satisfaction" -> Seq("03"),
      "details" -> Seq(details)
    )
    case Fourth(details) => Map(
      "satisfaction" -> Seq("04"),
      "details" -> Seq(details)
    )
    case Fifth(details) => Map(
      "satisfaction" -> Seq("05"),
      "details" -> Seq(details)
    )
  }

  implicit val jsonReads = {
    import play.api.libs.json.Reads.StringReads
    (__ \ "satisfaction").read[String].flatMap[SatisfactionSurvey] {
      case "01" => (__ \ "details").read[String] map First.apply
      case "02" => (__ \ "details").read[String] map Second.apply
      case "03" => (__ \ "details").read[String] map Third.apply
      case "04" => (__ \ "details").read[String] map Fourth.apply
      case "05" => (__ \ "details").read[String] map Fifth.apply
      case _ =>
        ValidationError("error.invalid")
    }
  }

  implicit val jsonWrites = Writes[SatisfactionSurvey] {
    case First(details) => Json.obj(
      "satisfaction" -> "01",
      "details" -> details
    )
    case Second(details) => Json.obj(
      "satisfaction" -> "02",
      "details" -> details
    )
    case Third(details) => Json.obj(
      "satisfaction" -> "03",
      "details" -> details
    )
    case Fourth(details) => Json.obj(
      "satisfaction" -> "04",
      "details" -> details
    )
    case Fifth(details) => Json.obj(
      "satisfaction" -> "05",
      "details" -> details
    )
  }
}
