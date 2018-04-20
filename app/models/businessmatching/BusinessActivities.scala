/*
 * Copyright 2018 HM Revenue & Customs
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

import jto.validation.forms.UrlFormEncoded
import jto.validation.{From, Rule, ValidationError, _}
import models.moneyservicebusiness.MoneyServiceBusiness
import models.{DateOfChange, FormTypes}
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import play.api.i18n.{Lang, Messages}
import play.api.libs.json.{Reads, Writes, _}
import utils.TraversableValidators._
import play.api.libs.functional.syntax._

case class BusinessActivities(businessActivities: Set[BusinessActivity],
                              additionalActivities: Option[Set[BusinessActivity]] = None,
                              removeActivities: Option[Set[BusinessActivity]] = None,
                              dateOfChange: Option[DateOfChange] = None) {

  def hasBusinessOrAdditionalActivity(activity: BusinessActivity) = {
    businessActivities.union(additionalActivities.getOrElse(Set.empty)) contains activity
  }

}

sealed trait BusinessActivity {

  def getMessage(implicit lang: Lang): String = {
    val message = "businessmatching.registerservices.servicename.lbl."
    this match {
      case AccountancyServices => Messages(s"${message}01")
      case BillPaymentServices => Messages(s"${message}02")
      case EstateAgentBusinessService => Messages(s"${message}03")
      case HighValueDealing => Messages(s"${message}04")
      case MoneyServiceBusiness => Messages(s"${message}05")
      case TrustAndCompanyServices => Messages(s"${message}06")
      case TelephonePaymentService => Messages(s"${message}07")
    }
  }
}

case object AccountancyServices extends BusinessActivity
case object BillPaymentServices extends  BusinessActivity
case object EstateAgentBusinessService extends BusinessActivity
case object HighValueDealing extends BusinessActivity
case object MoneyServiceBusiness extends BusinessActivity
case object TrustAndCompanyServices extends BusinessActivity
case object TelephonePaymentService extends BusinessActivity

object BusinessActivity {

  implicit val activityFormRead = Rule[String, BusinessActivity] {
      case "01" => Valid(AccountancyServices)
      case "02" => Valid(BillPaymentServices)
      case "03" => Valid(EstateAgentBusinessService)
      case "04" => Valid(HighValueDealing)
      case "05" => Valid(MoneyServiceBusiness)
      case "06" => Valid(TrustAndCompanyServices)
      case "07" => Valid(TelephonePaymentService)
      case _ => Invalid(Seq((Path \ "businessActivities") -> Seq(ValidationError("error.invalid"))))
  }



  implicit val activityFormWrite = Write[BusinessActivity, String] {
      case AccountancyServices => "01"
      case BillPaymentServices => "02"
      case EstateAgentBusinessService => "03"
      case HighValueDealing => "04"
      case MoneyServiceBusiness => "05"
      case TrustAndCompanyServices => "06"
      case TelephonePaymentService => "07"
  }

  implicit val jsonActivityReads: Reads[BusinessActivity] = Reads {
    case JsString("01") => JsSuccess(AccountancyServices)
    case JsString("02") => JsSuccess(BillPaymentServices)
    case JsString("03") => JsSuccess(EstateAgentBusinessService)
    case JsString("04") => JsSuccess(HighValueDealing)
    case JsString("05") => JsSuccess(MoneyServiceBusiness)
    case JsString("06") => JsSuccess(TrustAndCompanyServices)
    case JsString("07") => JsSuccess(TelephonePaymentService)
    case _ => JsError((JsPath \ "businessActivities") -> play.api.data.validation.ValidationError("error.invalid"))
  }

  implicit val jsonActivityWrite = Writes[BusinessActivity] {
    case AccountancyServices => JsString("01")
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
    BillPaymentServices,
    EstateAgentBusinessService,
    HighValueDealing,
    MoneyServiceBusiness,
    TrustAndCompanyServices,
    TelephonePaymentService
  )

  // TODO: These can potentially be removed once the new 'MSB/TCSP' add service flow goes in
  lazy val allWithoutMsbTcsp = all filterNot {
    case MoneyServiceBusiness | TrustAndCompanyServices => true
    case _ => false
  }
  lazy val allWithoutMsb = all filterNot {
    case MoneyServiceBusiness => true
    case _ => false
  }

  implicit def formReads(implicit p: Path => RuleLike[UrlFormEncoded, Set[BusinessActivity]]): Rule[UrlFormEncoded, BusinessActivities] =
    FormTypes.businessActivityRule("error.required.bm.register.service")

  implicit def formWrites(implicit w: Write[BusinessActivity, String]) = Write[BusinessActivities, UrlFormEncoded](activitiesWriter _)

  private def activitiesWriter(activities: BusinessActivities)(implicit w: Write[BusinessActivity, String]) =
    Map("businessActivities[]" -> activities.additionalActivities.fold(activities.businessActivities){act => act}.toSeq.map(w.writes))

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
      case "02" => Reads(_ => JsSuccess(BillPaymentServices)) map identity[BusinessActivity]
      case "03" => Reads(_ => JsSuccess(EstateAgentBusinessService)) map identity[BusinessActivity]
      case "04" => Reads(_ => JsSuccess(HighValueDealing)) map identity[BusinessActivity]
      case "05" => Reads(_ => JsSuccess(MoneyServiceBusiness)) map identity[BusinessActivity]
      case "06" => Reads(_ => JsSuccess(TrustAndCompanyServices)) map identity[BusinessActivity]
      case "07" => Reads(_ => JsSuccess(TelephonePaymentService)) map identity[BusinessActivity]
      case _ =>
        Reads(_ => JsError((JsPath \ path) -> play.api.data.validation.ValidationError("error.invalid")))
    }
  }

  def getValue(ba:BusinessActivity): String =
    ba match {
      case AccountancyServices => "01"
      case BillPaymentServices => "02"
      case EstateAgentBusinessService => "03"
      case HighValueDealing => "04"
      case MoneyServiceBusiness => "05"
      case TrustAndCompanyServices => "06"
      case TelephonePaymentService => "07"
    }

  def getBusinessActivity(ba:String): BusinessActivity =
    ba match {
      case "01" => AccountancyServices
      case "02" => BillPaymentServices
      case "03" => EstateAgentBusinessService
      case "04" => HighValueDealing
      case "05" => MoneyServiceBusiness
      case "06" => TrustAndCompanyServices
      case "07" => TelephonePaymentService
    }

}