package models.hvd

import play.api.data.mapping._
import play.api.data.mapping.forms._
import play.api.data.validation.ValidationError
import play.api.libs.functional.Monoid
import play.api.libs.json.{JsObject, JsValue}
import utils.TraversableValidators

case class PaymentMethods(
                         courier: Boolean,
                         direct: Boolean,
                         other: Option[String]
                         )

sealed trait PaymentMethods0 {

  // This could be made more generic...
  private implicit def r[I, O]
  (implicit
    r: Rule[I, O]
  ): Rule[I, Option[O]] =
    r.fmap(Some.apply).orElse(Rule(_ => Success(None)))

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
          Rule(_ => Success(s))
        case _ =>
          Rule(_ => Failure(Seq(Path -> Seq(ValidationError("error.minLength", l)))))
      }

      def maxLengthR(l: Int) = Rule.zero[String].flatMap[String] {
        case s if s.length <= l =>
          Rule(_ => Success(s))
        case _ =>
          Rule(_ => Failure(Seq(Path -> Seq(ValidationError("error.maxLength", l)))))
      }

      val detailsR: Rule[String, String] =
        (minLengthR(minLength) withMessage "error.required.hvd.describe") compose
          (maxLengthR(maxLength) withMessage "error.maxlength.hvd.describe")

      val booleanR = b andThen { _ fmap { case Some(b) => b; case None => false } }

      (
        (__ \ "courier").read(booleanR) and
        (__ \ "direct").read(booleanR) and
        (__ \ "other").read(booleanR).flatMap[Option[String]] {
          case true =>
            (__ \ "details").read(detailsR) fmap Some.apply
          case false =>
            Rule(_ => Success(None))
        }
      )(PaymentMethods.apply _).validateWith("error.required.hvd.choose.option"){
        methods =>
          methods.courier || methods.direct || methods.other.isDefined
      }
    }

  private implicit def write[A]
  (implicit
   mon: Monoid[A],
   s: Path => WriteLike[Option[String], A],
   b: Path => WriteLike[Boolean, A]
  ): Write[PaymentMethods, A] =
    To[A] { __ =>
      (
        (__ \ "courier").write[Boolean] and
        (__ \ "direct").write[Boolean] and
        (__ \ "other").write[Boolean].contramap[Option[_]] {
          case Some(_) => true
          case None => false
        } and
        (__ \ "details").write[Option[String]]
      )(a => (a.courier, a.direct, a.other, a.other))
    }

  val formR: Rule[UrlFormEncoded, PaymentMethods] = {
    import play.api.data.mapping.forms.Rules._
    implicitly[Rule[UrlFormEncoded, PaymentMethods]]
  }

  val jsonR: Rule[JsValue, PaymentMethods] = {
    import play.api.data.mapping.json.Rules.{pickInJson => _, _}
    import utils.JsonMapping.{genericJsonR, pickInJson}
    implicitly[Rule[JsValue, PaymentMethods]]
  }

  val formW: Write[PaymentMethods, UrlFormEncoded] = {
    import play.api.data.mapping.forms.Writes._
    implicitly[Write[PaymentMethods, UrlFormEncoded]]
  }

  val jsonW: Write[PaymentMethods, JsObject] = {
    import play.api.data.mapping.json.Writes._
    implicitly[Write[PaymentMethods, JsObject]]
  }
}

object PaymentMethods {

  object Cache extends PaymentMethods0

  implicit val formR = Cache.formR
  implicit val formW = Cache.formW
  implicit val jsonR = Cache.jsonR
  implicit val jsonW = Cache.jsonW
}
