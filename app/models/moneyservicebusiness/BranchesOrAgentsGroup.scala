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

import models.Country
import play.api.libs.json.{Json, Reads, Writes, __}

case class BranchesOrAgentsGroup(hasCountries: BranchesOrAgents, branches: Option[BranchesOrAgentsCountries])

object BranchesOrAgentsGroup {

  implicit val jsonReads: Reads[BranchesOrAgentsGroup] = {
    import play.api.libs.functional.syntax._
    ((__ \ "hasCountries").read[Boolean] map BranchesOrAgents.apply and
      (__ \ "countries").read[Seq[Country]].map {
        case countries if countries.isEmpty => None
        case countries => Some(BranchesOrAgentsCountries.apply(countries))
      })((hasCountries, countries) => BranchesOrAgentsGroup apply(hasCountries, countries))
  }

  implicit val jsonWrites:Writes[BranchesOrAgentsGroup] = {
    Writes[BranchesOrAgentsGroup] {
      case BranchesOrAgentsGroup(BranchesOrAgents(hasCountries), None) =>
        Json.obj(
          "hasCountries" -> hasCountries,
          "countries" -> Seq[String]()
        )
      case BranchesOrAgentsGroup(BranchesOrAgents(hasCountries), Some(BranchesOrAgentsCountries(countries))) =>
        Json.obj(
          "hasCountries" -> hasCountries,
          "countries" -> countries
        )
    }
  }

  def update(branchesOrAgents: BranchesOrAgentsGroup, hasCountries: BranchesOrAgents): BranchesOrAgentsGroup = {
    hasCountries match {
      case BranchesOrAgents(false) => BranchesOrAgentsGroup(hasCountries, None)
      case BranchesOrAgents(true) => BranchesOrAgentsGroup(hasCountries, branchesOrAgents.branches)
    }
  }

  def update(branchesOrAgents: BranchesOrAgentsGroup, countries: BranchesOrAgentsCountries): BranchesOrAgentsGroup = {
    countries match {
      case BranchesOrAgentsCountries(list) if list.isEmpty => BranchesOrAgentsGroup(BranchesOrAgents(false), None)
      case BranchesOrAgentsCountries(_) =>BranchesOrAgentsGroup(BranchesOrAgents(true), Some(countries))
    }
  }
}