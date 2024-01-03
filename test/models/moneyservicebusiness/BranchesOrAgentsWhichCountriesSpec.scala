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

import jto.validation._
import jto.validation.forms.UrlFormEncoded
import models.Country
import org.scalatest.MustMatchers
import org.scalatestplus.play.PlaySpec

class BranchesOrAgentsWhichCountriesSpec extends PlaySpec with MustMatchers{

  "MsbServices" must {

    val rule = implicitly[Rule[UrlFormEncoded, BranchesOrAgentsWhichCountries]]
    val write = implicitly[Write[BranchesOrAgentsWhichCountries, UrlFormEncoded]]


    "round trip through forms correctly" in {
      val model: BranchesOrAgentsWhichCountries = BranchesOrAgentsWhichCountries(Seq(Country("United Kingdom", "GB")))
      rule.validate(write.writes(model)) mustBe Valid(model)
    }

    "successfully validate when  there is at least 1 country selected" in {
      val form: UrlFormEncoded = Map( "countries" -> Seq("GB") )
      val model: BranchesOrAgentsWhichCountries = BranchesOrAgentsWhichCountries( Seq(Country("United Kingdom", "GB")))
      rule.validate(form) mustBe Valid(model)
    }

    "fail to validate when hasCountries is true and there are no countries selected" in {

      val form: UrlFormEncoded = Map(
        "hasCountries" -> Seq("true"),
        "countries" -> Seq.empty
      )

      rule.validate(form) mustBe Invalid(
        Seq((Path \ "countries") -> Seq(ValidationError("error.invalid.countries.msb.branchesOrAgents.country")))
      )
    }

    "successfully validate when there are empty values in the seq" in {

      val form: UrlFormEncoded = Map(
        "countries[]" -> Seq("GB", "", "FR", "")
      )

      rule.validate(form) mustBe Valid(BranchesOrAgentsWhichCountries(Seq(
        Country("United Kingdom", "GB"),
        Country("France", "FR")
      )))
    }

    "test" in {

      val form: UrlFormEncoded = Map(
        "countries[0]" -> Seq("GB"),
        "countries[1]" -> Seq("")
      )

      rule.validate(form) mustBe Valid(BranchesOrAgentsWhichCountries(Seq(
        Country("United Kingdom", "GB")
      )))
    }
  }

  "BranchesOrAgents form writes" when {


    "the list of countries has entries" must {
      "populate the countries list" in {
        BranchesOrAgentsWhichCountries.formWrite.writes(BranchesOrAgentsWhichCountries(Seq(Country("TESTCOUNTRY1", "TC1"), Country("TESTCOUNTRY2", "TC2")))) must be (Map(
        "countries[0]" -> Seq("TC1"),
        "countries[1]" -> Seq("TC2")
      ))}
    }
  }
}

