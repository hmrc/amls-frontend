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
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.play.PlaySpec
import play.api.libs.json._

class BranchesOrAgentsSpec extends PlaySpec with Matchers {

  "MsbServices" must {

    "round trip through Json correctly" in {
      val model: BranchesOrAgents = BranchesOrAgents(
        BranchesOrAgentsHasCountries(true),
        Some(BranchesOrAgentsWhichCountries(Seq(Country("United Kingdom", "GB"))))
      )
      Json.fromJson[BranchesOrAgents](Json.toJson(model)) mustBe JsSuccess(model)
    }

    "parse json correctly where no countries" in {
      val model: BranchesOrAgents = BranchesOrAgents(BranchesOrAgentsHasCountries(false), None)

      val json = Json.obj(
        "hasCountries" -> false
      )

      BranchesOrAgents.jsonWrites.writes(model) must be(json)
    }
  }

  "BranchesOrAgents" when {

    "the list of countries is empty" must {
      "set hasCountries to false" in {

        BranchesOrAgents.update(
          BranchesOrAgents(BranchesOrAgentsHasCountries(true), None),
          BranchesOrAgentsWhichCountries(Seq.empty)
        ) mustBe BranchesOrAgents(BranchesOrAgentsHasCountries(false), None)
      }
    }

    "the list of countries has entries" must {
      "set hasCountries to true and populate the countries list" in {

        BranchesOrAgents.update(
          BranchesOrAgents(BranchesOrAgentsHasCountries(false), None),
          BranchesOrAgentsWhichCountries(Seq(Country(name = "sadasd", code = "asdasd")))
        ) mustBe BranchesOrAgents(
          BranchesOrAgentsHasCountries(true),
          Some(BranchesOrAgentsWhichCountries(Seq(Country(name = "sadasd", code = "asdasd"))))
        )
      }
    }
  }
}
