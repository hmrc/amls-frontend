package models.renewal

import models.Country
import jto.validation._
import jto.validation.forms.UrlFormEncoded
import play.api.libs.json.{Reads, Writes}
import utils.TraversableValidators

case class MostTransactions(countries: Seq[Country])

private sealed trait MostTransactions0 {

  private implicit def rule[A]
  (implicit
   a: Path => RuleLike[A, Seq[String]],
   cR: Rule[Seq[String], Seq[Country]]
  ): Rule[A, MostTransactions] =
    From[A] { __ =>

      import utils.MappingUtils.Implicits.RichRule
      import TraversableValidators._

      implicit val emptyToNone: String => Option[String] = {
        case "" => None
        case s => Some(s)
      }

      val seqR = {
        (seqToOptionSeq[String] andThen flattenR[String] andThen cR)
          .andThen(minLengthR[Seq[Country]](1) withMessage "error.required.renewal.country.name")
          .andThen(maxLengthR[Seq[Country]](3))
      }

      (__ \ "mostTransactionsCountries").read(seqR) map MostTransactions.apply
    }

  private implicit def write[A]
  (implicit
   a: Path => WriteLike[Seq[Country], A]
  ): Write[MostTransactions, A] =
    To[A] { __ =>
      import play.api.libs.functional.syntax.unlift
      (__ \ "mostTransactionsCountries").write[Seq[Country]] contramap unlift(MostTransactions.unapply)
    }

  val formR: Rule[UrlFormEncoded, MostTransactions] = {
    import jto.validation.forms.Rules._
    implicitly
  }

  val jsonR: Reads[MostTransactions] = {
    import utils.JsonMapping._
    import jto.validation.playjson.Rules.{JsValue => _, pickInJson => _, _}
    implicitly
  }

  val formW: Write[MostTransactions, UrlFormEncoded] = {
    import jto.validation.forms.Writes._
    import utils.MappingUtils.spm
    implicitly
  }

  val jsonW: Writes[MostTransactions] = {
    import jto.validation.playjson.Writes._
    import utils.JsonMapping._
    implicitly
  }
}

object MostTransactions {

  private object Cache extends MostTransactions0

  implicit val formR: Rule[UrlFormEncoded, MostTransactions] = Cache.formR
  implicit val formW: Write[MostTransactions, UrlFormEncoded] = Cache.formW
  implicit val jsonR: Reads[MostTransactions] = Cache.jsonR
  implicit val jsonW: Writes[MostTransactions] = Cache.jsonW

  implicit def convert(model: MostTransactions): models.moneyservicebusiness.MostTransactions = {
    models.moneyservicebusiness.MostTransactions(model.countries)
  }
}
