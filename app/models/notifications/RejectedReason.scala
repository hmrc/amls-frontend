package models.notifications

import play.api.libs.json._

sealed trait RejectedReason extends StatusReason

object RejectedReason {

  case object NonCompliant extends RejectedReason
  case object FailedToRespond extends RejectedReason
  case object FailedToPayCharges extends RejectedReason
  case object FitAndProperFailure extends RejectedReason
  case object OtherFailed extends RejectedReason
  case object OtherRefused extends RejectedReason

  implicit def reason(reason:String) : RejectedReason = {
    reason match {
      case "01" => NonCompliant
      case "02" => FailedToRespond
      case "03" => FailedToPayCharges
      case "04" => FitAndProperFailure
      case "98" => OtherFailed
      case "99" => OtherRefused
    }
  }

  implicit val jsonWrites = Writes[RejectedReason] {
    case NonCompliant => JsString("01")
    case FailedToRespond => JsString("02")
    case FailedToPayCharges => JsString("03")
    case FitAndProperFailure => JsString("04")
    case OtherFailed => JsString("98")
    case OtherRefused => JsString("99")
  }
}
