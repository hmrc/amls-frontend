package models.renewal

import models.Country
import jto.validation._
import jto.validation.forms.UrlFormEncoded
import play.api.libs.json.{Reads, Writes}
import utils.TraversableValidators

case class MsbMostTransactions(countries: Seq[Country])

private sealed trait MostTransactions0 {

  private implicit def rule[A]
  (implicit
   a: Path => RuleLike[A, Seq[String]],
   cR: Rule[Seq[String], Seq[Country]]
  ): Rule[A, MsbMostTransactions] =
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

      (__ \ "mostTransactionsCountries").read(seqR) map MsbMostTransactions.apply
    }

  private implicit def write[A]
  (implicit
   a: Path => WriteLike[Seq[Country], A]
  ): Write[MsbMostTransactions, A] =
    To[A] { __ =>
      import play.api.libs.functional.syntax.unlift
      (__ \ "mostTransactionsCountries").write[Seq[Country]] contramap unlift(MsbMostTransactions.unapply)
    }

  val formR: Rule[UrlFormEncoded, MsbMostTransactions] = {
    import jto.validation.forms.Rules._
    implicitly
  }

  val jsonR: Reads[MsbMostTransactions] = {
    import utils.JsonMapping._
    import jto.validation.playjson.Rules.{JsValue => _, pickInJson => _, _}
    implicitly
  }

  val formW: Write[MsbMostTransactions, UrlFormEncoded] = {
    import jto.validation.forms.Writes._
    import utils.MappingUtils.spm
    implicitly
  }

  val jsonW: Writes[MsbMostTransactions] = {
    import jto.validation.playjson.Writes._
    import utils.JsonMapping._
    implicitly
  }
}

object MsbMostTransactions {

  private object Cache extends MostTransactions0

  implicit val formR: Rule[UrlFormEncoded, MsbMostTransactions] = Cache.formR
  implicit val formW: Write[MsbMostTransactions, UrlFormEncoded] = Cache.formW
  implicit val jsonR: Reads[MsbMostTransactions] = Cache.jsonR
  implicit val jsonW: Writes[MsbMostTransactions] = Cache.jsonW

  implicit def convert(model: MsbMostTransactions): models.moneyservicebusiness.MostTransactions = {
    models.moneyservicebusiness.MostTransactions(model.countries)
  }
}
