package models.hvd

import play.api.data.mapping._
import play.api.data.mapping.forms._
import play.api.data.validation.ValidationError
import play.api.libs.functional.Monoid
import play.api.libs.json.{JsString, JsValue, Reads, Writes}

sealed trait PaymentMethod

sealed trait PaymentMethod0 {

  private implicit def rule[A]
  (implicit
   s: Path => Rule[A, String]
  ): Rule[A, PaymentMethod] =
    From[A] { __ =>
      (__ \ "method").read[String].flatMap[PaymentMethod] {
        case "01" =>
          Rule(_ => Success(PaymentMethod.Courier))
        case "02" =>
          Rule(_ => Success(PaymentMethod.Direct))
        case "03" =>
          (__ \ "details").read[String] fmap PaymentMethod.Other.apply
        case _ =>
          Rule(_ => Failure(Seq(Path -> Seq(ValidationError("error.invalid")))))
      }
    }

  private implicit def write[A]
  (implicit
   mon: Monoid[A],
   s: Path => WriteLike[String, A]
  ): Write[PaymentMethod, A] =
    To[A] { __ =>
      (
        (__ \ "method").write[String].contramap[PaymentMethod] {
          case PaymentMethod.Courier => "01"
          case PaymentMethod.Direct => "02"
          case PaymentMethod.Other(_) => "03"
        } and
        (__ \ "details").write[String].contramap[PaymentMethod] {
          case PaymentMethod.Other(details) => details
          case _ => ""
        }
      )(a => (a, a))
    }

  val formR: Rule[UrlFormEncoded, PaymentMethod] = {
    import play.api.data.mapping.forms.Rules._
    implicitly[Rule[UrlFormEncoded, PaymentMethod]]
  }

  val jsonR: Rule[JsValue, PaymentMethod] = {
    import utils.JsonMapping._
    import play.api.data.mapping.json.Rules.{JsValue => _, pickInJson => _, _}
    implicitly[Rule[JsValue, PaymentMethod]]
  }

  val formW: Write[PaymentMethod, UrlFormEncoded] = {
    import play.api.data.mapping.forms.Writes._
    implicitly[Write[PaymentMethod, UrlFormEncoded]]
  }

  val jsonW: Write[PaymentMethod, JsValue] = {
    import utils.JsonMapping._
    import play.api.data.mapping.json.Writes._
    implicitly[Write[PaymentMethod, JsValue]]
  }
}

object PaymentMethod {

  object Courier extends PaymentMethod
  object Direct extends PaymentMethod
  case class Other(details: String) extends PaymentMethod

  object Cache extends PaymentMethod0

  implicit val formR = Cache.formR
  implicit val formW = Cache.formW
  implicit val jsonR = Cache.jsonR
  implicit val jsonW = Cache.jsonW
}
