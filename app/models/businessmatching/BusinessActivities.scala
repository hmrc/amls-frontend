/*
 * Copyright 2019 HM Revenue & Customs
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

package models.businessmatching

import cats.data.Validated.{Invalid, Valid}
import jto.validation.forms.UrlFormEncoded
import jto.validation.{Rule, ValidationError, _}
import models.{DateOfChange, FormTypes}
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import play.api.i18n.{Lang, Messages}
import play.api.libs.functional.syntax._
import play.api.libs.json.{Reads, Writes, _}

case class BusinessActivities(businessActivities: Set[BusinessActivity],
                              additionalActivities: Option[Set[BusinessActivity]] = None,
                              removeActivities: Option[Set[BusinessActivity]] = None,
                              dateOfChange: Option[DateOfChange] = None) {

  def hasBusinessOrAdditionalActivity(activity: BusinessActivity) = {
    businessActivities.union(additionalActivities.getOrElse(Set.empty)) contains activity
  }

}

sealed trait BusinessActivity {

  def getMessage(usePhrasedMessage:Boolean = false)(implicit lang: Lang): String = {
    val phrasedString = if(usePhrasedMessage) ".phrased" else ""
    val message = s"businessmatching.registerservices.servicename.lbl."
    this match {
      case AccountancyServices => Messages(s"${message}01${phrasedString}")
      case ArtMarketParticipant => Messages(s"${message}02${phrasedString}")
      case BillPaymentServices => Messages(s"${message}03${phrasedString}")
      case EstateAgentBusinessService => Messages(s"${message}04${phrasedString}")
      case HighValueDealing => Messages(s"${message}05${phrasedString}")
      case MoneyServiceBusiness => Messages(s"${message}06${phrasedString}")
      case TrustAndCompanyServices => Messages(s"${message}07${phrasedString}")
      case TelephonePaymentService => Messages(s"${message}08${phrasedString}")
    }
  }
}

case object AccountancyServices extends BusinessActivity
case object ArtMarketParticipant extends BusinessActivity
case object BillPaymentServices extends  BusinessActivity
case object EstateAgentBusinessService extends BusinessActivity
case object HighValueDealing extends BusinessActivity
case object MoneyServiceBusiness extends BusinessActivity
case object TrustAndCompanyServices extends BusinessActivity
case object TelephonePaymentService extends BusinessActivity

object BusinessActivity {

  implicit val activityFormRead = Rule[String, BusinessActivity] {
      case "01" => Valid(AccountancyServices)
      case "02" => Valid(ArtMarketParticipant)
      case "03" => Valid(BillPaymentServices)
      case "04" => Valid(EstateAgentBusinessService)
      case "05" => Valid(HighValueDealing)
      case "06" => Valid(MoneyServiceBusiness)
      case "07" => Valid(TrustAndCompanyServices)
      case "08" => Valid(TelephonePaymentService)
      case _ => Invalid(Seq((Path \ "businessActivities") -> Seq(ValidationError("error.invalid"))))
  }

  implicit val activityFormWrite = Write[BusinessActivity, String] {
      case AccountancyServices => "01"
      case ArtMarketParticipant => "02"
      case BillPaymentServices => "03"
      case EstateAgentBusinessService => "04"
      case HighValueDealing => "05"
      case MoneyServiceBusiness => "06"
      case TrustAndCompanyServices => "07"
      case TelephonePaymentService => "08"
  }

  implicit val jsonActivityReads: Reads[BusinessActivity] = Reads {
    case JsString("01") => JsSuccess(AccountancyServices)
    case JsString("08") => JsSuccess(ArtMarketParticipant)
    case JsString("02") => JsSuccess(BillPaymentServices)
    case JsString("03") => JsSuccess(EstateAgentBusinessService)
    case JsString("04") => JsSuccess(HighValueDealing)
    case JsString("05") => JsSuccess(MoneyServiceBusiness)
    case JsString("06") => JsSuccess(TrustAndCompanyServices)
    case JsString("07") => JsSuccess(TelephonePaymentService)
    case _ => JsError((JsPath \ "businessActivities") -> play.api.libs.json.JsonValidationError("error.invalid"))
  }

  implicit val jsonActivityWrite = Writes[BusinessActivity] {
    case AccountancyServices => JsString("01")
    case ArtMarketParticipant => JsString("08")
    case BillPaymentServices => JsString("02")
    case EstateAgentBusinessService => JsString("03")
    case HighValueDealing => JsString("04")
    case MoneyServiceBusiness => JsString("05")
    case TrustAndCompanyServices => JsString("06")
    case TelephonePaymentService => JsString("07")
  }
}

object BusinessActivities {

  import utils.MappingUtils.Implicits._

  val all: Set[BusinessActivity] = Set(
    AccountancyServices,
    ArtMarketParticipant,
    BillPaymentServices,
    EstateAgentBusinessService,
    HighValueDealing,
    MoneyServiceBusiness,
    TrustAndCompanyServices,
    TelephonePaymentService
  )

  implicit def formReads(implicit p: Path => RuleLike[UrlFormEncoded, Set[BusinessActivity]]): Rule[UrlFormEncoded, BusinessActivities] =
    FormTypes.businessActivityRule("error.required.bm.register.service")

  implicit def formWrites(implicit w: Write[BusinessActivity, String]) = Write[BusinessActivities, UrlFormEncoded](activitiesWriter _)

  private def activitiesWriter(activities: BusinessActivities)(implicit w: Write[BusinessActivity, String]) =
    Map("businessActivities[]" -> activities.additionalActivities.fold(activities.businessActivities){act => act}.toSeq.map(w.writes))

  import jto.validation.forms.Rules._
  import utils.TraversableValidators.minLengthR

  def formReaderMinLengthR(msg: String): Rule[UrlFormEncoded, Set[BusinessActivity]] = From[UrlFormEncoded] { __ =>
    (__ \ "businessActivities").read(minLengthR[Set[BusinessActivity]](1).withMessage(msg))
  }

  def maxLengthValidator(count: Int): Rule[Set[BusinessActivity], Set[BusinessActivity]] = Rule.fromMapping[Set[BusinessActivity], Set[BusinessActivity]] {
    case s if s.size == 2 && s.size == count => Invalid(Seq(ValidationError("error.required.bm.remove.leave.twobusinesses")))
    case s if s.size == count => Invalid(Seq(ValidationError("error.required.bm.remove.leave.one")))
    case s => Valid(s)
  }

  def combinedReader(count: Int, msg: String) = formReaderMinLengthR(msg) andThen maxLengthValidator(count).repath(_ => Path \ "businessActivities")

  implicit def activitySetWrites(implicit w: Write[BusinessActivity, String]) = Write[Set[BusinessActivity], UrlFormEncoded] { activities =>
    Map("businessActivities[]" -> activities.toSeq.map { a => BusinessActivities.getValue(a) })
  }

  implicit val format = Json.writes[BusinessActivities]

  implicit val jsonReads: Reads[BusinessActivities] = {
    import play.api.libs.json.Reads.StringReads
    (
      (__ \ "businessActivities").read[Set[String]].flatMap[Set[BusinessActivity]]{ ba =>
        activitiesReader(ba, "businessActivities").foldLeft[Reads[Set[BusinessActivity]]](Reads[Set[BusinessActivity]](_ =>
          JsSuccess(Set.empty))) { (result, data) =>
          data flatMap { r =>
            result.map{_ + r}
          }
        }
    } and
      (__ \ "additionalActivities").readNullable[Set[String]].flatMap[Option[Set[BusinessActivity]]] {
        case Some(a) =>
          activitiesReader(a, "additionalActivities").foldLeft[Reads[Option[Set[BusinessActivity]]]](Reads[Option[Set[BusinessActivity]]](_ =>
            JsSuccess(None))) { (result, data) =>
            data flatMap { r =>
              result.map {
                case Some(n) => Some(n + r)
                case _ => Some(Set(r))
              }
            }
          }
        case _ => None
    } and (__ \ "removeActivities").readNullable[Set[String]].flatMap[Option[Set[BusinessActivity]]] {
        case Some(a) =>
          activitiesReader(a, "removeActivities").foldLeft[Reads[Option[Set[BusinessActivity]]]](Reads[Option[Set[BusinessActivity]]](_ =>
            JsSuccess(None))) { (result, data) =>
            data flatMap { r =>
              result.map {
                case Some(n) => Some(n + r)
                case _ => Some(Set(r))
              }
            }
          }
        case _ => None
    } and (__ \ "dateOfChange").readNullable[DateOfChange])((a,b,c,d) => BusinessActivities(a,b,c,d))
  }

  private def activitiesReader(values: Set[String], path: String): Set[Reads[_ <: BusinessActivity]] = {
    values map {
      case "01" => Reads(_ => JsSuccess(AccountancyServices)) map identity[BusinessActivity]
      case "08" => Reads(_ => JsSuccess(ArtMarketParticipant)) map identity[BusinessActivity]
      case "02" => Reads(_ => JsSuccess(BillPaymentServices)) map identity[BusinessActivity]
      case "03" => Reads(_ => JsSuccess(EstateAgentBusinessService)) map identity[BusinessActivity]
      case "04" => Reads(_ => JsSuccess(HighValueDealing)) map identity[BusinessActivity]
      case "05" => Reads(_ => JsSuccess(MoneyServiceBusiness)) map identity[BusinessActivity]
      case "06" => Reads(_ => JsSuccess(TrustAndCompanyServices)) map identity[BusinessActivity]
      case "07" => Reads(_ => JsSuccess(TelephonePaymentService)) map identity[BusinessActivity]
      case _ =>
        Reads(_ => JsError((JsPath \ path) -> play.api.libs.json.JsonValidationError("error.invalid")))
    }
  }

  def getValue(ba:BusinessActivity): String =
    ba match {
      case AccountancyServices => "01"
      case ArtMarketParticipant => "02"
      case BillPaymentServices => "03"
      case EstateAgentBusinessService => "04"
      case HighValueDealing => "05"
      case MoneyServiceBusiness => "06"
      case TrustAndCompanyServices => "07"
      case TelephonePaymentService => "08"
    }

  def getBusinessActivity(ba:String): BusinessActivity =
    ba match {
      case "01" => AccountancyServices
      case "02" => ArtMarketParticipant
      case "03" => BillPaymentServices
      case "04" => EstateAgentBusinessService
      case "05" => HighValueDealing
      case "06" => MoneyServiceBusiness
      case "07" => TrustAndCompanyServices
      case "08" => TelephonePaymentService
    }
}