package models.renewal

import jto.validation._
import jto.validation.forms._
import jto.validation.ValidationError
import play.api.libs.json.{Json, Reads, Writes}
import utils.JsonMapping
import cats.data.Validated.{Invalid, Valid}


case class PaymentMethods(
                         courier: Boolean,
                         direct: Boolean,
                         other: Option[String]
                         )

sealed trait PaymentMethods0 {

  import JsonMapping._
  import utils.MappingUtils.MonoidImplicits._
  import models.FormTypes._

  private implicit def rule[A]
  (implicit
   s: Path => Rule[A, String],
   b: Path => Rule[A, Option[Boolean]]
  ): Rule[A, PaymentMethods] =
    From[A] { __ =>

      import utils.MappingUtils.Implicits.RichRule

      val minLength = 1
      val maxLength = 255

      def minLengthR(l: Int) = Rule.zero[String].flatMap[String] {
        case s if s.length >= l =>
          Rule(_ => Valid(s))
        case _ =>
          Rule(_ => Invalid(Seq(Path -> Seq(ValidationError("error.minLength", l)))))
      }

      def maxLengthR(l: Int) = Rule.zero[String].flatMap[String] {
        case s if s.length <= l =>
          Rule(_ => Valid(s))
        case _ =>
          Rule(_ => Invalid(Seq(Path -> Seq(ValidationError("error.maxLength", l)))))
      }

      val detailsR: Rule[String, String] =
        (minLengthR(minLength) withMessage "error.required.hvd.describe") andThen
        (maxLengthR(maxLength) withMessage "error.invalid.maxlength.255") andThen
        basicPunctuationPattern()

      val booleanR = b andThen { _ map { case Some(b) => b; case None => false } }

      (
        (__ \ "courier").read(booleanR) ~
        (__ \ "direct").read(booleanR) ~
        (__ \ "other").read(booleanR).flatMap[Option[String]] {
          case true =>
            (__ \ "details").read(detailsR) map Some.apply
          case false =>
            Rule(_ => Valid(None))
        }
      )(PaymentMethods.apply _).validateWith("error.required.hvd.choose.option"){
        methods =>
          methods.courier || methods.direct || methods.other.isDefined
      }
    }

  private implicit def write
  (implicit
   mon: cats.Monoid[UrlFormEncoded],
   s: Path => WriteLike[Option[String], UrlFormEncoded],
   b: Path => WriteLike[Boolean, UrlFormEncoded]
  ): Write[PaymentMethods, UrlFormEncoded] =
    To[UrlFormEncoded] { __ =>
      (
        (__ \ "courier").write[Boolean] ~
        (__ \ "direct").write[Boolean] ~
        (__ \ "other").write[Boolean].contramap[Option[_]] {
          case Some(_) => true
          case None => false
        } ~
        (__ \ "details").write[Option[String]]
      )(a => (a.courier, a.direct, a.other, a.other))
    }

  val formR: Rule[UrlFormEncoded, PaymentMethods] = {
    import jto.validation.forms.Rules._
    implicitly[Rule[UrlFormEncoded, PaymentMethods]]
  }

  val jsonR: Reads[PaymentMethods] = {
    import utils.JsonMapping._
    import jto.validation.playjson.Rules.{pickInJson => _, _}
    implicitly
  }

  val formW: Write[PaymentMethods, UrlFormEncoded] = {
    import cats.implicits._
    import jto.validation.forms.Writes._
    implicitly[Write[PaymentMethods, UrlFormEncoded]]
  }

  val jsonW = Writes[PaymentMethods] {x =>
    val jsMethods = Json.obj("courier" -> x.courier,
      "direct" -> x.direct,
      "other" -> x.other.isDefined)
    val jsDetails = Json.obj("details" -> x.other)
    x.other.isDefined match {
      case true => jsMethods ++ jsDetails
      case false => jsMethods
    }

  }
}

object PaymentMethods {

  object Cache extends PaymentMethods0

  implicit val formR: Rule[UrlFormEncoded, PaymentMethods] = Cache.formR
  implicit val jsonR: Reads[PaymentMethods] = Cache.jsonR
  implicit val formW: Write[PaymentMethods, UrlFormEncoded] = Cache.formW
  implicit val jsonW: Writes[PaymentMethods] = Cache.jsonW
}
