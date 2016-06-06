package models.moneyservicebusiness

import models.Country
import play.api.data.mapping._
import play.api.data.mapping.forms.UrlFormEncoded
import play.api.libs.json.{Reads, Writes}
import utils.TraversableValidators

case class MostTransactions(countries: Seq[Country])

private sealed trait MostTransactions0 {

  private implicit def rule[A]
  (implicit
    a: Path => RuleLike[A, Seq[Country]]
  ): Rule[A, MostTransactions] =
    From[A] { __ =>

      import utils.MappingUtils.Implicits.RichRule
      import TraversableValidators._

      val seqR =
        (minLength[Seq[Country]](1) withMessage "foo") compose (maxLength[Seq[Country]](1) withMessage "bar")

      (__ \ "countries").read(seqR) fmap MostTransactions.apply
    }

  private implicit def write[A]
  (implicit
   a: Path => WriteLike[Seq[Country], A]
  ): Write[MostTransactions, A] =
    To[A] { __ =>

      import play.api.libs.functional.syntax.unlift
      (__ \ "countries").write[Seq[Country]] contramap unlift(MostTransactions.unapply)
    }

  val formR: Rule[UrlFormEncoded, MostTransactions] = {
    import play.api.data.mapping.forms.Rules._
    implicitly
  }

  val jsonR: Reads[MostTransactions] = {
    import play.api.data.mapping.json.Rules.{JsValue => _, pickInJson => _, _}
    import utils.JsonMapping._
    implicitly
  }

  val formW: Write[MostTransactions, UrlFormEncoded] = {
    import play.api.data.mapping.forms.Writes._
    implicitly
  }

  val jsonW: Writes[MostTransactions] = {
    import play.api.data.mapping.json.Writes._
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
}
