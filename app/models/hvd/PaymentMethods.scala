package models.hvd

import jto.validation._
import jto.validation.forms._
import jto.validation.ValidationError
import play.api.libs.json.{Reads, Writes}

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
    r.map(Some.apply).orElse(Rule(_ => Success(None)))

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
        (minLengthR(minLength) withMessage "error.required.hvd.describe") andThen
          (maxLengthR(maxLength) withMessage "error.invalid.maxlength.255")

      val booleanR = b andThen { _ map { case Some(b) => b; case None => false } }

      (
        (__ \ "courier").read(booleanR) ~
        (__ \ "direct").read(booleanR) ~
        (__ \ "other").read(booleanR).flatMap[Option[String]] {
          case true =>
            (__ \ "details").read(detailsR) map Some.apply
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
   mon: cats.Monoid[A],
   s: Path => WriteLike[Option[String], A],
   b: Path => WriteLike[Boolean, A]
  ): Write[PaymentMethods, A] =
    To[A] { __ =>
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
   // import jto.validation.playjson.Rules.{pickInJson => _, _}
    implicitly
  }

  val formW: Write[PaymentMethods, UrlFormEncoded] = {
    implicitly[Write[PaymentMethods, UrlFormEncoded]]
  }

  val jsonW: Writes[PaymentMethods] = {
    implicitly
  }
}

object PaymentMethods {

  object Cache extends PaymentMethods0

  implicit val formR: Rule[UrlFormEncoded, PaymentMethods] = Cache.formR
  implicit val jsonR: Reads[PaymentMethods] = Cache.jsonR
  implicit val formW: Write[PaymentMethods, UrlFormEncoded] = Cache.formW
  implicit val jsonW: Writes[PaymentMethods] = Cache.jsonW
}
