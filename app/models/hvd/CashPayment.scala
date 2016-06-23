package models.hvd

import models.FormTypes._
import org.joda.time.{DateTimeFieldType, LocalDate}
import play.api.data.mapping.{Write, Success, From, Rule}
import play.api.data.mapping.forms._
import play.api.libs.json._

sealed trait CashPayment

case class CashPaymentYes(paymentDate: LocalDate) extends CashPayment

case object CashPaymentNo extends CashPayment

object CashPayment {

  import utils.MappingUtils.Implicits._

  implicit val formRule: Rule[UrlFormEncoded, CashPayment] = From[UrlFormEncoded] { __ =>
    import play.api.data.mapping.forms.Rules._
    (__ \ "registeredForVAT").read[Boolean].withMessage("error.required.hvd.accepted.cash.payment") flatMap {
      case true =>
        (__ \ "paymentDate").read(localDateRule) fmap CashPaymentYes.apply
      case false => Rule.fromMapping { _ => Success(CashPaymentNo) }
    }
  }

  implicit val formWrites: Write[CashPayment, UrlFormEncoded] = Write {
    case CashPaymentYes(date) =>
      Map("registeredForVAT" -> Seq("true"),
      "startDate.day" -> Seq(date.get(DateTimeFieldType.dayOfMonth()).toString),
    "startDate.month" -> Seq(date.get(DateTimeFieldType.monthOfYear()).toString),
    "startDate.year" -> Seq(date.get(DateTimeFieldType.year()).toString)
    )
    case CashPaymentNo => Map("registeredForVAT" -> Seq("false"))
  }
}
