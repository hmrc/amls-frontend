package models.notifications

import play.api.data.validation.ValidationError
import play.api.libs.json._

sealed trait StatusType

object StatusType {

  case object Approved extends StatusType
  case object Rejected extends StatusType
  case object Revoked extends StatusType
  case object DeRegistered extends StatusType
  case object Expired extends StatusType

  implicit val jsonReads: Reads[StatusType] =
    Reads {
      case JsString("04") => JsSuccess(Approved)
      case JsString("06") => JsSuccess(Rejected)
      case JsString("08") => JsSuccess(Revoked)
      case JsString("10") => JsSuccess(DeRegistered)
      case JsString("11") => JsSuccess(Expired)
      case _ => JsError(JsPath -> ValidationError("error.invalid"))
    }

  implicit val jsonWrites =
    Writes[StatusType] {
      case Approved => JsString("04")
      case Rejected => JsString("06")
      case Revoked => JsString("08")
      case DeRegistered => JsString("10")
      case Expired => JsString("11")
    }
}
