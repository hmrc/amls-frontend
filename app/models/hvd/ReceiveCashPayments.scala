package models.hvd

import jto.validation._
import jto.validation.forms._
import play.api.libs.functional.Monoid
import play.api.libs.json.{Writes, _}

case class ReceiveCashPayments(paymentMethods: Option[PaymentMethods])

sealed trait ReceiveCashPayments0 {
  import utils.JsonMapping._

  private implicit def rule[A]
  (implicit
   b: Path => Rule[A, Boolean],
   aR: Path => Rule[A, A],
   paymentMethodsR: Rule[A, PaymentMethods]
  ): Rule[A, ReceiveCashPayments] =
    From[A] { __ =>

      import utils.MappingUtils.Implicits.RichRule

      val booleanR = b andThen { _ withMessage "error.required.hvd.receive.cash.payments" }

      (__ \ "receivePayments").read(booleanR).flatMap[Option[PaymentMethods]] {
        case true =>
          (__ \ "paymentMethods").read[A] andThen paymentMethodsR.repath((Path \ "paymentMethods") ++ _) map Some.apply
        case false =>
          Rule(_ => Success(None))
      } map ReceiveCashPayments.apply
    }

  val formR: Rule[UrlFormEncoded, ReceiveCashPayments] = {
    import jto.validation.forms.Rules._
    implicitly[Rule[UrlFormEncoded, ReceiveCashPayments]]
  }

  implicit val formWrites: Write[ReceiveCashPayments, UrlFormEncoded] = To[UrlFormEncoded] { __ =>
    import jto.validation.forms.Writes._
    (
      (__ \ "receivePayments").write[Boolean].contramap[Option[_]] {
        case Some(_) => true
        case None => false
      } ~
        (__ \ "paymentMethods").write[Option[PaymentMethods]]
      )(a => (a.paymentMethods, a.paymentMethods))
  }

  import play.api.libs.json.{Writes, _}

}

object ReceiveCashPayments {

  private object Cache extends ReceiveCashPayments0
implicit val format = Json.format[ReceiveCashPayments]
  implicit val formR = Cache.formR
  implicit val formW = Cache.formWrites
 // implicit val jsonR = Cache.jsonR
 // implicit val jsonW = Cache.jsonW
}
