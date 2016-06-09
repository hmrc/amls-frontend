package models.tcsp

import models.FormTypes._
import play.api.data.mapping.forms.UrlFormEncoded
import play.api.data.mapping._
import play.api.data.validation.ValidationError
import play.api.libs.json._
import play.api.libs.json.Reads.StringReads
import play.api.data.mapping.forms.Rules.{minLength => _, _}
import utils.TraversableValidators.minLengthR
import play.api.i18n.{Messages, Lang}

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
      case Other(details) => Messages(s"${message}08") + s":$details"
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
  val serviceDetailsType = notEmptyStrip compose
                           notEmpty.withMessage("error.required.tcsp.provided_services.details") compose
                           maxLength(serviceDetailsMaxLength)

  val serviceType = minLengthR[Set[String]](1).withMessage("error.required.tcsp.provided_services.services")

  implicit val formReads: Rule[UrlFormEncoded, ProvidedServices] =
    From[UrlFormEncoded] { __ =>
          (__ \ "services").read(serviceType) flatMap { z =>
            z.map {
              case "01" => Rule[UrlFormEncoded, TcspService](_ => Success(PhonecallHandling))
              case "02" => Rule[UrlFormEncoded, TcspService](_ => Success(EmailHandling))
              case "03" => Rule[UrlFormEncoded, TcspService](_ => Success(EmailServer))
              case "04" => Rule[UrlFormEncoded, TcspService](_ => Success(SelfCollectMailboxes))
              case "05" => Rule[UrlFormEncoded, TcspService](_ => Success(MailForwarding))
              case "06" => Rule[UrlFormEncoded, TcspService](_ => Success(Receptionist))
              case "07" => Rule[UrlFormEncoded, TcspService](_ => Success(ConferenceRooms))
              case "08" =>
                (__ \ "details").read(serviceDetailsType) fmap Other.apply
              case _ =>
                Rule[UrlFormEncoded, TcspService] { _ =>
                  Failure(Seq((Path \ "services") -> Seq(ValidationError("error.invalid"))))
                }
            }.foldLeft[Rule[UrlFormEncoded, Set[TcspService]]](
              Rule[UrlFormEncoded, Set[TcspService]](_ => Success(Set.empty))
            ) {
              case (m, n) =>
                  n flatMap { x =>
                    m fmap {
                      _ + x
                    }
                  }
            } fmap ProvidedServices.apply
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
              Reads(_ => JsError((JsPath \ "services") -> ValidationError("error.invalid")))
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


