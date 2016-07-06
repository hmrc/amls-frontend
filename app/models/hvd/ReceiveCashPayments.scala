package models.hvd

import play.api.data.mapping._
import play.api.data.mapping.forms._
import play.api.libs.functional.Monoid
import play.api.libs.json.{JsValue, Reads, Writes}

case class ReceiveCashPayments(paymentMethod: Option[Set[PaymentMethod]])

sealed trait ReceiveCashPayments0 {

  private implicit def rule[A]
  (implicit
   b: Path => Rule[A, Boolean],
   s: Path => Rule[A, Set[PaymentMethod]]
  ): Rule[A, ReceiveCashPayments] =
    From[A] { __ =>
      (__ \ "receivePayments").read[Boolean].flatMap[Option[Set[PaymentMethod]]] {
        case true =>
          (__ \ "paymentMethods").read[Set[PaymentMethod]] fmap Some.apply
        case false =>
          Rule(_ => Success(None))
      } fmap ReceiveCashPayments.apply
    }

  private implicit def write[A]
  (implicit
   mon: Monoid[A],
   b: Path => WriteLike[Boolean, A],
   s: Path => WriteLike[Set[PaymentMethod], A]
  ): Write[ReceiveCashPayments, A] =
    To[A] { __ =>
      (
        (__ \ "receivePayments").write[Boolean].contramap[Option[_]] {
          case Some(_) => true
          case None => false
        } and
        (__ \ "paymentMethods").write[Set[PaymentMethod]].contramap[Option[Set[PaymentMethod]]] {
          case Some(set) => set
          case None => Set.empty
        }
      )(a => (a.paymentMethod, a.paymentMethod))
    }

  val formR: Rule[UrlFormEncoded, ReceiveCashPayments] = {
    import play.api.data.mapping.forms.Rules._
    implicitly[Rule[UrlFormEncoded, ReceiveCashPayments]]
  }

  val jsonR: Rule[JsValue, ReceiveCashPayments] = {
    import utils.JsonMapping._
    import play.api.data.mapping.json.Rules.{JsValue => _, pickInJson => _, _}
    implicitly[Rule[JsValue, ReceiveCashPayments]]
  }

  val formW: Write[ReceiveCashPayments, UrlFormEncoded] = {
    import play.api.data.mapping.forms.Writes._
    implicitly[Write[ReceiveCashPayments, UrlFormEncoded]]
  }

  val jsonW: Writes[ReceiveCashPayments] = {
    import utils.JsonMapping._
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
