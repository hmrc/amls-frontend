package models.businessactivities

import models.Country
import play.api.data.mapping.forms.UrlFormEncoded
import play.api.data.mapping.{From, Rule, Success, Write}
import play.api.data.mapping._
import play.api.libs.functional.Monoid
import play.api.libs.json.{Reads, Writes}
import utils.{TraversableValidators, JsonMapping}

case class CustomersOutsideUK(countries: Option[Seq[Country]])

sealed trait CustomersOutsideUK0 {

  val minLength = 1
  val maxLength = 10

  import JsonMapping._

  private implicit def rule[A]
  (implicit
   bR: Path => Rule[A, Boolean],
   sR: Path => Rule[A, Seq[String]],
   cR: Rule[Seq[String], Seq[Country]]
  ): Rule[A, CustomersOutsideUK] =
    From[A] { __ =>

      import utils.MappingUtils.Implicits.RichRule
      import TraversableValidators._

      implicit val emptyToNone: String => Option[String] = {
        case "" => None
        case s => Some(s)
      }

      val boolR =
        bR andThen {
          _ withMessage "error.required.ba.select.country"
        }

      val countrySeqR = {
        (seqToOptionSeq[String] compose flattenR[String] compose cR)
          .compose(minLengthR[Seq[Country]](minLength) withMessage "error.invalid.ba.select.country")
          .compose(maxLengthR[Seq[Country]](maxLength))
      }

      (__ \ "isOutside").read(boolR).flatMap[Option[Seq[Country]]] {
        case true =>
          (__ \ "countries").read(countrySeqR) fmap Some.apply
        case false =>
          Rule(_ => Success(None))
      } fmap CustomersOutsideUK.apply
    }

  private implicit def write[A]
  (implicit
   mon: Monoid[A],
   a: Path => WriteLike[Boolean, A],
   b: Path => WriteLike[Option[Seq[Country]], A]
  ): Write[CustomersOutsideUK, A] =
    To[A] { __ =>
      (
        (__ \ "isOutside").write[Boolean].contramap[Option[_]] {
          case Some(_) => true
          case None => false
        } and
          (__ \ "countries").write[Option[Seq[Country]]]
        )(a => (a.countries, a.countries))
    }

  val formR: Rule[UrlFormEncoded, CustomersOutsideUK] = {
    import play.api.data.mapping.forms.Rules._
    implicitly
  }

  val jsonR: Reads[CustomersOutsideUK] = {
    import play.api.data.mapping.json.Rules.{JsValue => _, pickInJson => _, _}
    implicitly
  }

  val formW: Write[CustomersOutsideUK, UrlFormEncoded] = {
    import play.api.data.mapping.forms.Writes._
    implicitly
  }

  val jsonW: Writes[CustomersOutsideUK] = {
    import play.api.data.mapping.json.Writes._
    implicitly
  }
}

object CustomersOutsideUK {

  private object Cache extends CustomersOutsideUK0

  val minLength = Cache.minLength
  val maxLength = Cache.maxLength

  implicit val formR: Rule[UrlFormEncoded, CustomersOutsideUK] = Cache.formR
  implicit val jsonR: Reads[CustomersOutsideUK] = Cache.jsonR
  implicit val formW: Write[CustomersOutsideUK, UrlFormEncoded] = Cache.formW
  implicit val jsonW: Writes[CustomersOutsideUK] = Cache.jsonW
}
