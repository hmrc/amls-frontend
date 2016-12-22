package models.hvd

import play.api.data.mapping._
import play.api.data.mapping.forms._
import play.api.libs.functional.Monoid
import play.api.libs.json.{Writes, _}

case class ReceiveCashPayments(paymentMethods: Option[PaymentMethods])

sealed trait ReceiveCashPayments0 {

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
          (__ \ "paymentMethods").read[A] compose paymentMethodsR.repath((Path \ "paymentMethods") ++ _) fmap Some.apply
        case false =>
          Rule(_ => Success(None))
      } fmap ReceiveCashPayments.apply
    }

  private implicit def opW[I, O]
  (implicit
   mon: Monoid[O],
   w: Write[I, O]
  ): Write[Option[I], O] =
    Write {
      case Some(i) =>
        w.writes(i)
      case None =>
        mon.identity
    }

  private implicit def write[A]
  (implicit
   mon: Monoid[A],
   b: Path => Write[Boolean, A],
   aW: Path => Write[A, A],
   paymentMethodsW: Write[Option[PaymentMethods], A]
  ): Write[ReceiveCashPayments, A] =
    To[A] { __ =>

      import utils.MappingUtils.Implicits.RichWrite

      (
        (__ \ "receivePayments").write[Boolean].contramap[Option[_]] {
          case Some(_) => true
          case None => false
        } and
          (__ \ "paymentMethods").write[A].andThen(paymentMethodsW)
      )(a => (a.paymentMethods, a.paymentMethods))
    }

  val formR: Rule[UrlFormEncoded, ReceiveCashPayments] = {
    import play.api.data.mapping.forms.Rules._
    implicitly[Rule[UrlFormEncoded, ReceiveCashPayments]]
  }

  val jsonR: Reads[ReceiveCashPayments] = {
    import play.api.data.mapping.json.Rules.{pickInJson => _, _}
    import utils.JsonMapping.{genericJsonR, pickInJson}
    implicitly[Reads[ReceiveCashPayments]]
  }

  val formW: Write[ReceiveCashPayments, UrlFormEncoded] = {
    import play.api.data.mapping.forms.Writes._
    implicitly[Write[ReceiveCashPayments, UrlFormEncoded]]
  }

  val jsonW: Writes[ReceiveCashPayments] = {
    import utils.JsonMapping.genericJsonW
    import play.api.data.mapping.json.Writes._
    implicitly[Writes[ReceiveCashPayments]]
  }
}

object ReceiveCashPayments {

  private object Cache extends ReceiveCashPayments0

  implicit val formR = Cache.formR
  implicit val formW = Cache.formW
  implicit val jsonR = Cache.jsonR
  implicit val jsonW = Cache.jsonW
}
