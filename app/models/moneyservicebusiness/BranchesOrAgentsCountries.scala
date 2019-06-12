/*
 * Copyright 2019 HM Revenue & Customs
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

import jto.validation._
import jto.validation.forms.UrlFormEncoded
import models.Country
import utils.TraversableValidators

case class BranchesOrAgentsCountries(branches: Seq[Country])

object BranchesOrAgentsCountries {

  val minLength = 1
  val maxLength = 10

  import jto.validation.forms.Rules._

  implicit val formWrite: Write[BranchesOrAgentsCountries, UrlFormEncoded] = write

  implicit val formRule: Rule[UrlFormEncoded, BranchesOrAgentsCountries] = rule

  private implicit def rule
  (implicit
   a: Path => RuleLike[UrlFormEncoded, Seq[String]],
   cR: Rule[Seq[String], Seq[Country]]
  ): Rule[UrlFormEncoded, BranchesOrAgentsCountries] =
    From[UrlFormEncoded] { __ =>

      import TraversableValidators._
      import utils.MappingUtils.Implicits.RichRule

      implicit val emptyToNone: String => Option[String] = {
        case "" => None
        case s => Some(s)
      }

      val countrySeqR = {
        (seqToOptionSeq[String] andThen flattenR[String] andThen cR)
          .andThen(minLengthR[Seq[Country]](minLength) withMessage "error.invalid.countries.msb.branchesOrAgents")
          .andThen(maxLengthR[Seq[Country]](maxLength))
      }

      (__ \ "countries").read[Seq[String]].andThen(countrySeqR).map(countries => {
        BranchesOrAgentsCountries.apply(countries)
      })
    }


//  private implicit def write
//  (implicit
//   mon: cats.Monoid[UrlFormEncoded],
//  // a: Path => WriteLike[Boolean, UrlFormEncoded],
//   b: Path => WriteLike[Seq[Country], UrlFormEncoded]
//  ): Write[BranchesOrAgentsCountries, UrlFormEncoded] =
//    To[UrlFormEncoded] { __ =>
//      (
//
//          (__ \ "countries").write[Option[Seq[Country]]]
//        )(a => (Some(a.branches), Some(a.branches)))
//    }

  private  def write: Write[BranchesOrAgentsCountries, UrlFormEncoded] = Write {
    case BranchesOrAgentsCountries(countries) => countries.zipWithIndex.map(i => s"countries[${i._2}]" -> Seq(i._1.code)).toMap
    case _ => throw new IllegalArgumentException("Eep")
  }
}