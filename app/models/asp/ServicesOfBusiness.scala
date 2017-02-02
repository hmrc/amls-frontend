package models.asp

import models.DateOfChange
import jto.validation.forms.UrlFormEncoded
import jto.validation._
import play.api.libs.json._
import cats.data.Validated.{Invalid, Valid}
import utils.TraversableValidators._

case class ServicesOfBusiness(services: Set[Service], dateOfChange: Option[DateOfChange] = None)

sealed trait Service

case object Accountancy extends Service

case object PayrollServices extends Service

case object BookKeeping extends Service

case object Auditing extends Service

case object FinancialOrTaxAdvice extends Service

object Service {

  implicit val servicesFormRead = Rule[String, Service] {
      case "01" => Valid(Accountancy)
      case "02" => Valid(PayrollServices)
      case "03" => Valid(BookKeeping)
      case "04" => Valid(Auditing)
      case "05" => Valid(FinancialOrTaxAdvice)
      case _ =>
          Invalid(Seq((Path \ "services") -> Seq(ValidationError("error.invalid"))))
  }

  implicit val servicesFormWrite = Write[Service, String] {
      case Accountancy => "01"
      case PayrollServices => "02"
      case BookKeeping => "03"
      case Auditing => "04"
      case FinancialOrTaxAdvice => "05"
  }

  import play.api.data.validation.ValidationError
  implicit val jsonServiceReads: Reads[Service] =
    Reads {
      case JsString("01") => JsSuccess(Accountancy)
      case JsString("02") => JsSuccess(PayrollServices)
      case JsString("03") => JsSuccess(BookKeeping)
      case JsString("04") => JsSuccess(Auditing)
      case JsString("05") => JsSuccess(FinancialOrTaxAdvice)
      case _ => JsError((JsPath \ "services") -> ValidationError("error.invalid"))
    }

  implicit val jsonServiceWrites = Writes[Service] {
      case Accountancy => JsString("01")
      case PayrollServices => JsString("02")
      case BookKeeping => JsString("03")
      case Auditing => JsString("04")
      case FinancialOrTaxAdvice => JsString("05")
  }

}

object ServicesOfBusiness {

  import utils.MappingUtils.Implicits._

  implicit def formReads
  (implicit
   p: Path => RuleLike[UrlFormEncoded, Set[Service]]
    ): Rule[UrlFormEncoded, ServicesOfBusiness] =
    From[UrlFormEncoded] { __ =>
       (__ \ "services").read(minLengthR[Set[Service]](1).withMessage("error.required.asp.business.services")) .flatMap(ServicesOfBusiness(_, None))
  }

  implicit def formWrites
  (implicit
   w: Write[Service, String]
    ) = Write[ServicesOfBusiness, UrlFormEncoded] { data =>
    Map("services[]" -> data.services.toSeq.map(w.writes))
  }

  implicit val formats = Json.format[ServicesOfBusiness]
}

