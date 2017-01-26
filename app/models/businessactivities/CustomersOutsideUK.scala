package models.businessactivities

import models.Country
import jto.validation.forms.UrlFormEncoded
import jto.validation.{From, Rule, Success, Write}
import jto.validation._
import play.api.libs.json.{Json, Reads, Writes}
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
        (seqToOptionSeq[String] andThen flattenR[String] andThen cR)
          .andThen(minLengthR[Seq[Country]](minLength) withMessage "error.invalid.ba.select.country")
          .andThen(maxLengthR[Seq[Country]](maxLength))
      }

      (__ \ "isOutside").read(boolR).flatMap[Option[Seq[Country]]] {
        case true =>
          (__ \ "countries").read(countrySeqR) fmap Some.apply
        case false =>
          Rule(_ => Success(None))
      } fmap CustomersOutsideUK.apply
    }

  implicit def formW = Write[CustomersOutsideUK, UrlFormEncoded] {x =>
    val countries = x.countries.fold[Seq[String]](Seq.empty)(x => x.map(m => m.code))
    Map(
        "isOutside" -> Seq("true"),
        "countries" -> countries
      )
  }

  val formR: Rule[UrlFormEncoded, CustomersOutsideUK] = {
    import jto.validation.forms.Rules._
    implicitly
  }

  implicit val jsonR: Reads[CustomersOutsideUK] = {
    import play.api.libs.json.Reads._
    import play.api.libs.json._
    (__ \ "isOutside").read[Boolean].flatMap[Option[Seq[Country]]]  {
      case true => (__ \ "countries").readNullable[Seq[Country]]
      case false => Reads[Option[Seq[Country]]](_ => JsSuccess(None))
    }.map(CustomersOutsideUK(_))
  }

  val jsonW: Writes[CustomersOutsideUK] = Writes {x =>
    val countries = x.countries.fold[Seq[String]](Seq.empty)(x => x.map(m => m.code))
    Json.obj(
      "isOutside" -> true,
      "countries" -> countries
    )
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
