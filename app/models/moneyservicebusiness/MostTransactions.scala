/*
 * Copyright 2017 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package models.moneyservicebusiness

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
          .andThen(minLengthR[Seq[Country]](1) withMessage "error.required.countries.msb.most.transactions")
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
}
