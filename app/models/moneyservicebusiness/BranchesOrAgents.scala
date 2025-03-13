/*
 * Copyright 2024 HM Revenue & Customs
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
import play.api.libs.json.{Json, Reads, Writes, __}

case class BranchesOrAgents(
  hasCountries: BranchesOrAgentsHasCountries,
  branches: Option[BranchesOrAgentsWhichCountries]
)

object BranchesOrAgents {

  implicit val jsonReads: Reads[BranchesOrAgents] = {
    import play.api.libs.functional.syntax._
    ((__ \ "hasCountries").read[Boolean] map BranchesOrAgentsHasCountries.apply and
      (__ \ "countries").readNullable[Seq[Country]].map {
        case Some(countries) if countries.isEmpty => None
        case Some(countries)                      => Some(BranchesOrAgentsWhichCountries apply countries)
        case None                                 => None
      })((hasCountries, countries) => BranchesOrAgents.apply(hasCountries, countries))
  }

  implicit val jsonWrites: Writes[BranchesOrAgents] =
    Writes[BranchesOrAgents] {
      case BranchesOrAgents(BranchesOrAgentsHasCountries(hasCountries), None) =>
        Json.obj(
          "hasCountries" -> hasCountries
        )
      case BranchesOrAgents(
            BranchesOrAgentsHasCountries(hasCountries),
            Some(BranchesOrAgentsWhichCountries(countries))
          ) =>
        Json.obj(
          "hasCountries" -> hasCountries,
          "countries"    -> countries
        )
    }

  def update(branchesOrAgents: BranchesOrAgents, hasCountries: BranchesOrAgentsHasCountries): BranchesOrAgents =
    hasCountries match {
      case BranchesOrAgentsHasCountries(false) => BranchesOrAgents(hasCountries, None)
      case BranchesOrAgentsHasCountries(true)  => BranchesOrAgents(hasCountries, branchesOrAgents.branches)
    }

  def update(branchesOrAgents: BranchesOrAgents, countries: BranchesOrAgentsWhichCountries): BranchesOrAgents =
    countries match {
      case BranchesOrAgentsWhichCountries(list) if list.isEmpty =>
        BranchesOrAgents(BranchesOrAgentsHasCountries(false), None)
      case BranchesOrAgentsWhichCountries(_)                    => BranchesOrAgents(BranchesOrAgentsHasCountries(true), Some(countries))
    }
}
