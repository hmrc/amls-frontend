package models.notifications

import play.api.libs.json._
import play.api.mvc.PathBindable

sealed trait ContactType

object ContactType {

  case object ApplicationApproval extends ContactType
  case object RenewalApproval extends ContactType

  case object RejectionReasons extends ContactType
  case object RevocationReasons extends ContactType

  case object AutoExpiryOfRegistration extends ContactType

  case object ReminderToPayForApplication extends ContactType
  case object ReminderToPayForRenewal extends ContactType
  case object ReminderToPayForVariation extends ContactType
  case object ReminderToPayForManualCharges extends ContactType
  case object RenewalReminder extends ContactType

  case object MindedToReject extends ContactType
  case object MindedToRevoke extends ContactType
  case object NoLongerMindedToReject extends ContactType
  case object NoLongerMindedToRevoke extends ContactType

  case object RegistrationVariationApproval extends ContactType
  case object ApplicationAutorejectionForFailureToPay extends ContactType

  case object Others extends ContactType

  implicit val jsonReads: Reads[ContactType] =
    Reads {
      case JsString("REJR") => JsSuccess(RejectionReasons)
      case JsString("REVR") => JsSuccess(RevocationReasons)
      case JsString("MTRJ") => JsSuccess(MindedToReject)
      case JsString("NMRJ") => JsSuccess(NoLongerMindedToReject)
      case JsString("MTRV") => JsSuccess(MindedToRevoke)
      case JsString("NMRV") => JsSuccess(NoLongerMindedToRevoke)
      case JsString("OTHR") => JsSuccess(Others)
      case JsString("APA1") => JsSuccess(ApplicationApproval)
      case JsString("APR1") => JsSuccess(RenewalApproval)
      case JsString("EXPR") => JsSuccess(AutoExpiryOfRegistration)
      case JsString("RREM") => JsSuccess(RenewalReminder)
      case JsString("RPA1") => JsSuccess(ReminderToPayForApplication)
      case JsString("RPR1") => JsSuccess(ReminderToPayForRenewal)
      case JsString("RPV1") => JsSuccess(ReminderToPayForVariation)
      case JsString("RPM1") => JsSuccess(ReminderToPayForManualCharges)

      case _ => JsError((JsPath \ "contact_type") -> play.api.data.validation.ValidationError("error.invalid"))
    }

  implicit val jsonWrites =
    Writes[ContactType] {
      case RejectionReasons => JsString("REJR")
      case RevocationReasons => JsString("REVR")
      case MindedToReject => JsString("MTRJ")
      case NoLongerMindedToReject => JsString("NMRJ")
      case MindedToRevoke => JsString("MTRV")
      case NoLongerMindedToRevoke => JsString("NMRV")
      case Others => JsString("OTHR")
      case ApplicationApproval => JsString("APA1")
      case RenewalApproval => JsString("APR1")
      case AutoExpiryOfRegistration => JsString("EXPR")
      case RenewalReminder => JsString("RREM")
      case ReminderToPayForApplication => JsString("RPA1")
      case ReminderToPayForRenewal => JsString("RPR1")
      case ReminderToPayForVariation => JsString("RPV1")
      case ReminderToPayForManualCharges => JsString("RPM1")
    }

  implicit def pathBinder(implicit stringBinder:PathBindable[String]) = new PathBindable[ContactType] {


    override def bind(key: String, value: String): Either[String, ContactType] = {
      stringBinder.bind(key,value).right map {

        case "RejectionReasons" => ContactType.RejectionReasons
        case "RevocationReasons" => ContactType.RevocationReasons
        case "MindedToReject" => ContactType.MindedToReject
        case "NoLongerMindedToReject" => ContactType.NoLongerMindedToReject
        case "MindedToRevoke" => ContactType.MindedToRevoke
        case "NoLongerMindedToRevoke" => ContactType.NoLongerMindedToRevoke
        case "Others" => ContactType.Others
        case "ApplicationApproval" => ContactType.ApplicationApproval
        case "RenewalApproval" => ContactType.RenewalApproval
        case "AutoExpiryOfRegistration" => ContactType.AutoExpiryOfRegistration
        case "RenewalReminder" => ContactType.RenewalReminder
        case "ReminderToPayForApplication" => ContactType.ReminderToPayForApplication
        case "ReminderToPayForRenewal" => ContactType.ReminderToPayForRenewal
        case "ReminderToPayForVariation" => ContactType.ReminderToPayForVariation
        case "ReminderToPayForManualCharges" => ContactType.ReminderToPayForManualCharges
        case "RegistrationVariationApproval" => ContactType.RegistrationVariationApproval
        case "ApplicationAutorejectionForFailureToPay" => ContactType.ApplicationAutorejectionForFailureToPay
        case _ => throw new RuntimeException("No correct contact type")
      }
    }

    override def unbind(key: String, contactType: ContactType): String = {
      contactType.toString
    }
  }

}
