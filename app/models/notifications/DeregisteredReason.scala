package models.notifications

import play.api.libs.json._

sealed trait DeregisteredReason extends StatusReason

object DeregisteredReason {

  case object CeasedTrading extends DeregisteredReason
  case object HVDNoCashPayment extends DeregisteredReason
  case object OutOfScope extends DeregisteredReason
  case object NotTrading extends DeregisteredReason
  case object UnderAnotherSupervisor extends DeregisteredReason
  case object ChangeOfLegalEntity extends DeregisteredReason

  case object Other extends DeregisteredReason

    implicit def reason(reason:String) : DeregisteredReason = {
      reason match {
        case "01" => CeasedTrading
        case "02" => HVDNoCashPayment
        case "03" => OutOfScope
        case "04" => NotTrading
        case "05" => UnderAnotherSupervisor
        case "06" => ChangeOfLegalEntity
        case "99" => Other
      }
   }

  implicit val jsonWrites = Writes[DeregisteredReason] {
    case CeasedTrading => JsString("01")
    case HVDNoCashPayment => JsString("02")
    case OutOfScope => JsString("03")
    case NotTrading => JsString("04")
    case UnderAnotherSupervisor => JsString("05")
    case ChangeOfLegalEntity => JsString("06")
    case Other => JsString("99")
  }
}
