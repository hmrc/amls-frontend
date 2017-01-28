package models.hvd

import jto.validation._
import jto.validation.forms._
import models.Country
import play.api.libs.json.{Writes, _}

case class ReceiveCashPayments(paymentMethods: Option[PaymentMethods])

sealed trait ReceiveCashPayments0 {

  import utils.MappingUtils.Implicits._

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


  private implicit def write
  (implicit
   mon:cats.Monoid[UrlFormEncoded],
   a: Path => WriteLike[Boolean, UrlFormEncoded],
   b: Path => WriteLike[Option[PaymentMethods] , UrlFormEncoded]
  ): Write[ReceiveCashPayments, UrlFormEncoded] =
    To[UrlFormEncoded] { __ =>
      (
        (__ \ "receivePayments").write[Boolean].contramap[Option[_]] {
          case Some(_) => true
          case None => false
        } ~
          (__ \ "paymentMethods").write[Option[PaymentMethods]]
        )(a => (a.paymentMethods, a.paymentMethods))
    }


  val formR: Rule[UrlFormEncoded, ReceiveCashPayments] = {
    import jto.validation.forms.Rules._
    implicitly[Rule[UrlFormEncoded, ReceiveCashPayments]]
  }

  val jsonR: Reads[ReceiveCashPayments] = {
    import utils.JsonMapping._
    import jto.validation.playjson.Rules.{JsValue => _, pickInJson => _, _}
    implicitly
  }

  val formW: Write[ReceiveCashPayments, UrlFormEncoded] = {
    import cats.implicits._
    import jto.validation.forms.Writes._
    implicitly[Write[ReceiveCashPayments, UrlFormEncoded]]
  }

  val jsonW: Writes[ReceiveCashPayments] = {
    implicitly
  }
}

object ReceiveCashPayments {

  private object Cache extends ReceiveCashPayments0

  implicit val formR: Rule[UrlFormEncoded, ReceiveCashPayments] = Cache.formR
  implicit val jsonR: Reads[ReceiveCashPayments] = Cache.jsonR
  implicit val formW: Write[ReceiveCashPayments, UrlFormEncoded] = Cache.formW
  implicit val jsonW: Writes[ReceiveCashPayments] = Cache.jsonW
}
