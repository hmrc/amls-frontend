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

package models.businessmatching

import jto.validation.forms.UrlFormEncoded
import jto.validation._
import jto.validation.ValidationError
import play.api.i18n.{Lang, Messages}
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import play.api.libs.json._
import utils.TraversableValidators._

case class BusinessActivities(businessActivities: Set[BusinessActivity]){

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

  implicit def formReads
  (implicit p: Path => RuleLike[UrlFormEncoded, Set[BusinessActivity]]): Rule[UrlFormEncoded, BusinessActivities] =
    From[UrlFormEncoded] { __ =>
     (__ \ "businessActivities").read(minLengthR[Set[BusinessActivity]](1).withMessage("error.required.bm.register.service")) map BusinessActivities.apply
   }

  implicit def formWrites
  (implicit w: Write[BusinessActivity, String]) = Write[BusinessActivities, UrlFormEncoded] { data =>
    Map("businessActivities[]" -> data.businessActivities.toSeq.map(w.writes))
  }

  implicit val formats = Json.format[BusinessActivities]

}