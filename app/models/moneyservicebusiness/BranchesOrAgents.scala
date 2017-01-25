package models.moneyservicebusiness

import models.Country
import jto.validation._
import jto.validation.forms.UrlFormEncoded
import play.api.libs.functional.Monoid
import play.api.libs.json.{Reads, Writes}
import utils.{JsonMapping, TraversableValidators}

case class BranchesOrAgents(branches: Option[Seq[Country]])

sealed trait BranchesOrAgents0 {

  val minLength = 1
  val maxLength = 10

  import JsonMapping._

  private implicit def rule[A]
  (implicit
   bR: Path => Rule[A, Boolean],
   sR: Path => Rule[A, Seq[String]],
   cR: Rule[Seq[String], Seq[Country]]
  ): Rule[A, BranchesOrAgents] =
    From[A] { __ =>

      import utils.MappingUtils.Implicits.RichRule
      import TraversableValidators._

       implicit val emptyToNone: String => Option[String] = {
          case "" => None
          case s => Some(s)
       }

      val boolR =
        bR andThen {
          _ withMessage "error.required.hasCountries.msb.branchesOrAgents"
        }

      val countrySeqR = {
        (seqToOptionSeq[String] andThen flattenR[String] andThen cR)
          .andThen(minLengthR[Seq[Country]](minLength) withMessage "error.invalid.countries.msb.branchesOrAgents")
          .andThen(maxLengthR[Seq[Country]](maxLength))
      }

      (__ \ "hasCountries").read(boolR).flatMap[Option[Seq[Country]]] {
        case true =>
          (__ \ "countries").read(countrySeqR) fmap Some.apply
        case false =>
          Rule(_ => Success(None))
      } fmap BranchesOrAgents.apply
    }

  private implicit def write[A]
  (implicit
   mon: cats.Monoid[A],
   a: Path => WriteLike[Boolean, A],
   b: Path => WriteLike[Option[Seq[Country]], A]
  ): Write[BranchesOrAgents, A] =
    To[A] { __ =>
      (
        (__ \ "hasCountries").write[Boolean].contramap[Option[Seq[_]]] {
          case Some(x) if x.size == 0 => false
          case Some(_) => true
          case None => false
        } ~
        (__ \ "countries").write[Option[Seq[Country]]]
      )(a => (a.branches, a.branches))
    }

  val formR: Rule[UrlFormEncoded, BranchesOrAgents] = {
    import jto.validation.forms.Rules._
    implicitly
  }

  val jsonR: Reads[BranchesOrAgents] = {
    import jto.validation.playjson.Rules.{JsValue => _, pickInJson => _, _}
    implicitly
  }

  val formW: Write[BranchesOrAgents, UrlFormEncoded] = {
    import jto.validation.forms.Writes._
    implicitly
  }

  val jsonW: Writes[BranchesOrAgents] = {
    import jto.validation.playjson.Writes._
    implicitly
  }
}

object BranchesOrAgents {

  private object Cache extends BranchesOrAgents0

  val minLength = Cache.minLength
  val maxLength = Cache.maxLength

  implicit val formR: Rule[UrlFormEncoded, BranchesOrAgents] = Cache.formR
  implicit val jsonR: Reads[BranchesOrAgents] = Cache.jsonR
  implicit val formW: Write[BranchesOrAgents, UrlFormEncoded] = Cache.formW
  implicit val jsonW: Writes[BranchesOrAgents] = Cache.jsonW
}
