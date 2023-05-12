/*
 * Copyright 2023 HM Revenue & Customs
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
import models.tcsp.TcspTypes.all
import models.{Enumerable, WithName}
import utils.TraversableValidators.minLengthR
import play.api.i18n.Messages
import play.twirl.api.Html
import uk.gov.hmrc.govukfrontend.views.Aliases.{CheckboxItem, Text}

sealed trait TcspService {

  import models.tcsp.ProvidedServices._

  val value: String

  def getMessage(implicit messages: Messages): String = {
    val message = "tcsp.provided_services.service.lbl."
    this match {
      case PhonecallHandling => messages(s"${message}01")
      case EmailHandling => messages(s"${message}02")
      case EmailServer => messages(s"${message}03")
      case SelfCollectMailboxes => messages(s"${message}04")
      case MailForwarding => messages(s"${message}05")
      case Receptionist => messages(s"${message}06")
      case ConferenceRooms => messages(s"${message}07")
      case Other(details) => s"$details"
    }
  }
}

case class ProvidedServices(services: Set[TcspService])

object ProvidedServices extends Enumerable.Implicits {

  case object PhonecallHandling extends WithName("phonecallHandling") with TcspService {
    override val value: String = "01"
  }

  case object EmailHandling extends WithName("emailHandling") with TcspService {
    override val value: String = "02"
  }

  case object EmailServer extends WithName("emailServer") with TcspService {
    override val value: String = "03"
  }

  case object SelfCollectMailboxes extends WithName("selfCollectMailboxes") with TcspService {
    override val value: String = "04"
  }

  case object MailForwarding extends WithName("mailForwarding") with TcspService {
    override val value: String = "05"
  }

  case object Receptionist extends WithName("receptionist") with TcspService {
    override val value: String = "06"
  }

  case object ConferenceRooms extends WithName("conferenceRooms") with TcspService {
    override val value: String = "07"
  }

  case class Other(details: String) extends WithName("other") with TcspService {
    override val value: String = "08"
  }

  val all: Seq[TcspService] = Seq(
    PhonecallHandling,
    EmailHandling,
    EmailServer,
    SelfCollectMailboxes,
    MailForwarding,
    Receptionist,
    ConferenceRooms,
    Other("")
  )

  def formValues(conditionalHtml: Html)(implicit messages: Messages): Seq[CheckboxItem] = {

    val checkboxItems = all.zipWithIndex.map { case (service, index) =>

      val conditional = if(service.value == Other("").value) Some(conditionalHtml) else None

      CheckboxItem(
        content = Text(messages(s"tcsp.provided_services.service.lbl.${service.value}")),
        value = service.toString,
        id = Some(s"services$index"),
        name = Some(s"services[$index]"),
        conditionalHtml = conditional
      )
    }.sortBy(_.content.asHtml.body)

    val from = checkboxItems.indexWhere(_.value == Other("").toString)

    checkboxItems.patch(checkboxItems.length, Seq(checkboxItems(from)), 0).patch(from, Seq(), 1)
  }

  implicit val enumerable: Enumerable[TcspService] = Enumerable(all.map(v => v.toString -> v): _*)

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


