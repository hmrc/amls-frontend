package models.hvd

import models.FormTypes._
import org.joda.time.LocalDate
import play.api.data.mapping.{Success, From, Rule}
import play.api.data.mapping.forms._

sealed trait CashPayment
case class CashPaymentYes (paymentDate: LocalDate) extends CashPayment
case object CashPaymentNo extends CashPayment

object CashPayment {

  import utils.MappingUtils.Implicits._

  implicit val formRule: Rule[UrlFormEncoded, CashPayment] = From[UrlFormEncoded] { __ =>
    import play.api.data.mapping.forms.Rules._
    (__ \ "registeredForVAT").read[Boolean].withMessage("error.required.atb.registered.for.vat") flatMap {
      case true =>
        (__ \ "paymentDate").read(localDateRule) fmap CashPaymentYes.apply
      case false => Rule.fromMapping { _ => Success(CashPaymentNo) }
    }
  }

}
