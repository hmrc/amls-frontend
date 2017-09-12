/*
 * Copyright 2017 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
  case object DeRegistrationEffectiveDateChange extends ContactType

  case object Others extends ContactType

  case object NoSubject extends ContactType

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
        case "DeRegistrationEffectiveDateChange" => ContactType.DeRegistrationEffectiveDateChange
        case "NoSubject" => ContactType.NoSubject
        case _ => throw new RuntimeException("No correct contact type")
      }
    }

    override def unbind(key: String, contactType: ContactType): String = {
      contactType.toString
    }
  }

}
