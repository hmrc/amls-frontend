/*
 * Copyright 2020 HM Revenue & Customs
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

package models.tcsp

import models.FormTypes._
import jto.validation.forms.UrlFormEncoded
import jto.validation._
import jto.validation.ValidationError
import play.api.libs.json._
import play.api.libs.json.Reads.StringReads
import jto.validation.forms.Rules.{minLength => _, _}
import utils.TraversableValidators.minLengthR
import play.api.i18n.{Messages, Lang}
import play.api.Play.current
import play.api.i18n.Messages.Implicits._

sealed trait TcspService {

  val value: String = this match {
    case PhonecallHandling => "01"
    case EmailHandling => "02"
    case EmailServer => "03"
    case SelfCollectMailboxes => "04"
    case MailForwarding => "05"
    case Receptionist => "06"
    case ConferenceRooms => "07"
    case Other(_) => "08"
  }

  def getMessage(implicit lang: Lang): String = {
    val message = "tcsp.provided_services.service.lbl."
    this match {
      case PhonecallHandling => Messages(s"${message}01")
      case EmailHandling => Messages(s"${message}02")
      case EmailServer => Messages(s"${message}03")
      case SelfCollectMailboxes => Messages(s"${message}04")
      case MailForwarding => Messages(s"${message}05")
      case Receptionist => Messages(s"${message}06")
      case ConferenceRooms => Messages(s"${message}07")
      case Other(details) => s"$details"
    }
  }
}

case object PhonecallHandling extends TcspService
case object EmailHandling extends TcspService
case object EmailServer extends TcspService
case object SelfCollectMailboxes extends TcspService
case object MailForwarding extends TcspService
case object Receptionist extends TcspService
case object ConferenceRooms extends TcspService
case class Other(details: String) extends TcspService

case class ProvidedServices(services: Set[TcspService])

object ProvidedServices {

  import utils.MappingUtils.Implicits._

  val serviceDetailsMaxLength = 255

  val serviceDetailsType = notEmptyStrip andThen
    notEmpty.withMessage("error.required.tcsp.provided_services.details") andThen
    maxWithMsg(serviceDetailsMaxLength, "error.required.tcsp.provided_services.details.length") andThen
    basicPunctuationPattern("error.required.tcsp.provided_services.details.punctuation")

  val serviceType = minLengthR[Set[String]](1).withMessage("error.required.tcsp.provided_services.services")

  implicit val formReads: Rule[UrlFormEncoded, ProvidedServices] =
    From[UrlFormEncoded] { __ =>
          (__ \ "services").read(serviceType) flatMap { z =>
            z.map {
              case "01" => Rule[UrlFormEncoded, TcspService](_ => Valid(PhonecallHandling))
              case "02" => Rule[UrlFormEncoded, TcspService](_ => Valid(EmailHandling))
              case "03" => Rule[UrlFormEncoded, TcspService](_ => Valid(EmailServer))
              case "04" => Rule[UrlFormEncoded, TcspService](_ => Valid(SelfCollectMailboxes))
              case "05" => Rule[UrlFormEncoded, TcspService](_ => Valid(MailForwarding))
              case "06" => Rule[UrlFormEncoded, TcspService](_ => Valid(Receptionist))
              case "07" => Rule[UrlFormEncoded, TcspService](_ => Valid(ConferenceRooms))
              case "08" =>
                (__ \ "details").read(serviceDetailsType) map Other.apply
              case _ =>
                Rule[UrlFormEncoded, TcspService] { _ =>
                  Invalid(Seq((Path \ "services") -> Seq(ValidationError("error.invalid"))))
                }
            }.foldLeft[Rule[UrlFormEncoded, Set[TcspService]]](
              Rule[UrlFormEncoded, Set[TcspService]](_ => Valid(Set.empty))
            ) {
              case (m, n) =>
                  n flatMap { x =>
                    m map {
                      _ + x
                    }
                  }
            } map ProvidedServices.apply
          }
    }

  implicit def formWrites = Write[ProvidedServices, UrlFormEncoded] { ps =>
      Map(
        "services[]" -> (ps.services map { _.value }).toSeq
      ) ++ ps.services.foldLeft[UrlFormEncoded](Map.empty) {
        case (m, Other(name)) =>
          m ++ Map("details" -> Seq(name))
        case (m, _) =>
          m
      }
  }

  implicit val jsonReads: Reads[ProvidedServices] =
      (__ \ "services").read[Set[String]].flatMap { x =>
        x.map {
            case "01" => Reads(_ => JsSuccess(PhonecallHandling)) map identity[TcspService]
            case "02" => Reads(_ => JsSuccess(EmailHandling)) map identity[TcspService]
            case "03" => Reads(_ => JsSuccess(EmailServer)) map identity[TcspService]
            case "04" => Reads(_ => JsSuccess(SelfCollectMailboxes)) map identity[TcspService]
            case "05" => Reads(_ => JsSuccess(MailForwarding)) map identity[TcspService]
            case "06" => Reads(_ => JsSuccess(Receptionist)) map identity[TcspService]
            case "07" => Reads(_ => JsSuccess(ConferenceRooms)) map identity[TcspService]
            case "08" =>
              (JsPath \ "details").read[String].map (Other.apply  _) map identity[TcspService]
            case _ =>
              Reads(_ => JsError((JsPath \ "services") -> play.api.libs.json.JsonValidationError("error.invalid")))
          }.foldLeft[Reads[Set[TcspService]]](
            Reads[Set[TcspService]](_ => JsSuccess(Set.empty))
         ){
          (result, data) =>
            data flatMap {m =>
             result.map {n =>
               n + m
             }
           }
        }
      } map ProvidedServices.apply

  implicit val jsonWrites = Writes[ProvidedServices] { ps =>
    Json.obj(
      "services" -> (ps.services map {_.value}).toSeq
    ) ++ ps.services.foldLeft[JsObject](Json.obj()) {
          case (m, Other(details)) =>
            m ++ Json.obj("details" -> details)
          case (m, _) =>
            m
        }
  }

}


