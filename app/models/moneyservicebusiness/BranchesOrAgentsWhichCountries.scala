/*
 * Copyright 2020 HM Revenue & Customs
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

case class BranchesOrAgentsWhichCountries(branches: Seq[Country])

object BranchesOrAgentsWhichCountries {

  val minLength = 1
  val maxLength = 10

  import jto.validation.forms.Rules._

  implicit val formWrite: Write[BranchesOrAgentsWhichCountries, UrlFormEncoded] = write

  implicit val formRule: Rule[UrlFormEncoded, BranchesOrAgentsWhichCountries] = rule

  private implicit def rule
  (implicit
   a: Path => RuleLike[UrlFormEncoded, Seq[String]]
  ): Rule[UrlFormEncoded, BranchesOrAgentsWhichCountries] =
    From[UrlFormEncoded] { __ =>

      import TraversableValidators._
      import utils.MappingUtils.Implicits.RichRule

      implicit val emptyToNone: String => Option[String] = {
        case "" => None
        case s => Some(s)
      }

      val countrySeqR = {
        (seqToOptionSeq[String] andThen flattenR[String] andThen cR)
          .andThen(minLengthR[Seq[Country]](minLength) withMessage "error.invalid.countries.msb.branchesOrAgents.country")
      }

      (__ \ "countries").read(countrySeqR) map BranchesOrAgentsWhichCountries.apply
    }

  private  def write: Write[BranchesOrAgentsWhichCountries, UrlFormEncoded] = Write {
    case BranchesOrAgentsWhichCountries(countries) => countries.zipWithIndex.map(i => s"countries[${i._2}]" -> Seq(i._1.code)).toMap
    case _ => throw new IllegalArgumentException("Eep")
  }
}