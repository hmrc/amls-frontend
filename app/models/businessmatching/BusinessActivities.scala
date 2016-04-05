package models.businessmatching

import play.api.data.mapping.forms.UrlFormEncoded
import play.api.data.mapping._
import play.api.data.validation.ValidationError
import play.api.i18n.{Messages, Lang}
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
      case "01" => Success(AccountancyServices)
      case "02" => Success(BillPaymentServices)
      case "03" => Success(EstateAgentBusinessService)
      case "04" => Success(HighValueDealing)
      case "05" => Success(MoneyServiceBusiness)
      case "06" => Success(TrustAndCompanyServices)
      case "07" => Success(TelephonePaymentService)
      case _ => Failure(Seq((Path \ "businessActivities") -> Seq(ValidationError("error.invalid"))))
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
    case _ => JsError((JsPath \ "businessActivities") -> ValidationError("error.invalid"))
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
     (__ \ "businessActivities").read(minLength[Set[BusinessActivity]](1).withMessage("error.required.bm.register.service")) fmap BusinessActivities.apply
   }

  implicit def formWrites
  (implicit w: Write[BusinessActivity, String]) = Write[BusinessActivities, UrlFormEncoded] { data =>
    Map("businessActivities[]" -> data.businessActivities.toSeq.map(w.writes))
  }

  implicit val formats = Json.format[BusinessActivities]
}


