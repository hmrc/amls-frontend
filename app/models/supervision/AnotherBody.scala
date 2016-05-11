package models.supervision

import models.FormTypes._
import org.joda.time.LocalDate
import play.api.data.mapping.forms.Rules._
import play.api.data.mapping._
import play.api.data.mapping.forms._
import play.api.libs.json.{Json, Writes, Reads}

sealed trait AnotherBody

case class AnotherBodyYes(supervisorName: String,
                          startDate: LocalDate,
                          endDate: LocalDate,
                          endingReason: String) extends AnotherBody

case object AnotherBodyNo extends AnotherBody


object AnotherBody {

  import utils.MappingUtils.Implicits._

  private val supervisorMaxLength = 140
  private val reasonMaxLength = 255
  private val supervisorRule = notEmpty.withMessage("error.required.supervision.supervisor") compose
    maxLength(supervisorMaxLength).withMessage("error.invalid.supervision.supervisor")
  private val reasonRule = notEmpty.withMessage("error.required.supervision.reason") compose
    maxLength(reasonMaxLength).withMessage("error.invalid.supervision.reason")

  implicit val formRule: Rule[UrlFormEncoded, AnotherBody] = From[UrlFormEncoded] { __ =>
    import play.api.data.mapping.forms.Rules._

    (__ \ "anotherBody").read[Boolean].withMessage("error.required.supervision.anotherbody") flatMap {
      case true => (
        (__ \ "supervisorName").read(supervisorRule) ~
          (__ \ "startDate").read(localDateRule) ~
          (__ \ "endDate").read(localDateRule) ~
          (__ \ "endingReason").read(reasonRule)
        ) (AnotherBodyYes.apply _)

      case false => Rule.fromMapping { _ => Success(AnotherBodyNo) }

    }
  }

  implicit val formWrites: Write[AnotherBody, UrlFormEncoded] = Write {
    case a: AnotherBodyYes =>
      Map(
        "anotherBody" -> Seq("true"),
        "supervisorName" -> Seq(a.supervisorName),
        "endingReason" -> Seq(a.endingReason)
      ) ++ (
        localDateWrite.writes(a.startDate) map {
          case (key, value) =>
            s"startDate.$key" -> value
        })  ++ (
        localDateWrite.writes(a.endDate) map {
          case (key, value) =>
            s"endDate.$key" -> value
        })
    case AnotherBodyNo => Map("anotherBody" -> Seq("false"))
  }

  implicit val jsonReads: Reads[AnotherBody] = {

    import play.api.libs.functional.syntax._
    import play.api.libs.json.Reads._
    import play.api.libs.json._

    (__ \ "anotherBody").read[Boolean] flatMap {
      case true =>
        (
          (__ \ "supervisorName").read[String] ~
            (__ \ "startDate").read[LocalDate] ~
            (__ \ "endDate").read[LocalDate] ~
            (__ \ "endingReason").read[String]) (AnotherBodyYes.apply _) map identity[AnotherBody]

      case false => AnotherBodyNo
    }
  }

  implicit val jsonWrites = Writes[AnotherBody] {
    case a : AnotherBodyYes => Json.obj(
      "anotherBody" -> true,
      "supervisorName" -> a.supervisorName,
      "startDate" -> a.startDate,
      "endDate" -> a.endDate,
      "endingReason" -> a.endingReason
    )
    case AnotherBodyNo => Json.obj("anotherBody" -> false)
  }

}