package models.hvd

import models.FormTypes._
import org.joda.time.{DateTimeFieldType, LocalDate}
import jto.validation.forms.UrlFormEncoded
import jto.validation._
import play.api.libs.json.{Reads}

sealed trait CashPayment

case class CashPaymentYes(paymentDate: LocalDate) extends CashPayment

case object CashPaymentNo extends CashPayment

object CashPayment {

  import utils.MappingUtils.Implicits._

  implicit val formRule: Rule[UrlFormEncoded, CashPayment] = From[UrlFormEncoded] { __ =>
    import jto.validation.forms.Rules._
    (__ \ "acceptedAnyPayment").read[Boolean].withMessage("error.required.hvd.accepted.cash.payment") flatMap {
      case true =>
        (__ \ "paymentDate").read(localDateRule) fmap CashPaymentYes.apply
      case false => Rule.fromMapping { _ => Success(CashPaymentNo) }
    }
  }

  implicit def formWrites: Write[CashPayment, UrlFormEncoded] = Write {
    case CashPaymentYes(date) =>
      Map("acceptedAnyPayment" -> Seq("true"),
        "paymentDate.day" -> Seq(date.get(DateTimeFieldType.dayOfMonth()).toString),
        "paymentDate.month" -> Seq(date.get(DateTimeFieldType.monthOfYear()).toString),
        "paymentDate.year" -> Seq(date.get(DateTimeFieldType.year()).toString)
    )
    case CashPaymentNo => Map("acceptedAnyPayment" -> Seq("false"))
  }


  implicit val jsonReads: Reads[CashPayment] = {
    import play.api.libs.json._
    import play.api.libs.json.Reads._

    (__ \ "acceptedAnyPayment").read[Boolean] flatMap {
      case true => (__ \ "paymentDate").read[LocalDate] map CashPaymentYes.apply
      case false => Reads(_ => JsSuccess(CashPaymentNo))
    }
  }

  implicit val jsonWrites = {
    import play.api.libs.json.Writes._
    import play.api.libs.json._

    Writes[CashPayment] {
      case CashPaymentYes(b) => Json.obj(
        "acceptedAnyPayment" -> true,
        "paymentDate" -> b.toString
      )
      case CashPaymentNo => Json.obj("acceptedAnyPayment" -> false)
    }
  }
}
