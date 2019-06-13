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
import org.scalatest.MustMatchers
import org.scalatestplus.play.PlaySpec

class BranchesOrAgentsSpec extends PlaySpec with MustMatchers{

  "MsbServices" must {

    val rule = implicitly[Rule[UrlFormEncoded, BranchesOrAgents]]
    val write = implicitly[Write[BranchesOrAgents, UrlFormEncoded]]

    "round trip through forms correctly" in {

      val model: BranchesOrAgents = BranchesOrAgents(true)
      rule.validate(write.writes(model)) mustBe Valid(model)
    }

    "successfully validate when hasCountries is false" in {

      val form: UrlFormEncoded = Map(
        "hasCountries" -> Seq("false")
      )

      val model: BranchesOrAgents = BranchesOrAgents(false)

      rule.validate(form) mustBe Valid(model)
    }


    "fail to validate when hasCountries isn't selected" in {

      val form: UrlFormEncoded = Map.empty

      rule.validate(form) mustBe Invalid(
        Seq((Path \ "hasCountries") -> Seq(ValidationError("error.required.hasCountries.msb.branchesOrAgents")))
      )
    }

    "test" in {
      val form: UrlFormEncoded = Map("hasCountries" -> Seq("true"))
      rule.validate(form) mustBe Valid(BranchesOrAgents(true))
    }
  }

  "BranchesOrAgents form writes" when {
    "there is no list of countries" must {
      "set hasCountries to false" in {
        BranchesOrAgents.formWrites.writes(BranchesOrAgents(false)) must be (Map("hasCountries" -> Seq("false")))
      }
    }
  }
}

