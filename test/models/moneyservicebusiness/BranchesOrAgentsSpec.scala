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
import org.scalatest.MustMatchers
import org.scalatestplus.play.PlaySpec
import jto.validation._
import jto.validation.forms.UrlFormEncoded
import play.api.libs.json._

class BranchesOrAgentsSpec extends PlaySpec with MustMatchers{

  "MsbServices" must {

    val rule = implicitly[Rule[UrlFormEncoded, BranchesOrAgents]]
    val write = implicitly[Write[BranchesOrAgents, UrlFormEncoded]]

    "round trip through Json correctly" in {

      val model: BranchesOrAgents = BranchesOrAgents(Some(Seq(Country("United Kingdom", "GB"))))

      Json.fromJson[BranchesOrAgents](Json.toJson(model)) mustBe JsSuccess(model)
    }

    "round trip through forms correctly" in {

      val model: BranchesOrAgents = BranchesOrAgents(Some(Seq(Country("United Kingdom", "GB"))))
      rule.validate(write.writes(model)) mustBe Valid(model)
    }

    "successfully validate when hasCountries is false" in {

      val form: UrlFormEncoded = Map(
        "hasCountries" -> Seq("false")
      )

      val model: BranchesOrAgents = BranchesOrAgents(None)

      rule.validate(form) mustBe Valid(model)
    }

    "successfully validate when hasCountries is true and there is at least 1 country selected" in {

      val form: UrlFormEncoded = Map(
        "hasCountries" -> Seq("true"),
        "countries" -> Seq("GB")
      )

      val model: BranchesOrAgents =
        BranchesOrAgents(
          Some(Seq(Country("United Kingdom", "GB")))
        )

      rule.validate(form) mustBe Valid(model)
    }

    "fail to validate when hasCountries is true and there are no countries selected" in {

      val form: UrlFormEncoded = Map(
        "hasCountries" -> Seq("true"),
        "countries" -> Seq.empty
      )

      rule.validate(form) mustBe Invalid(
        Seq((Path \ "countries") -> Seq(ValidationError("error.invalid.countries.msb.branchesOrAgents")))
      )
    }

    "fail to validate when hasCountries is true and there are more than 10 countries" in {

      val form: UrlFormEncoded = Map(
        "hasCountries" -> Seq("true"),
        "countries[]" -> Seq.fill(11)("GB")
      )

      rule.validate(form) mustBe Invalid(
        Seq((Path \ "countries") -> Seq(ValidationError("error.maxLength", 10)))
      )
    }

    "fail to validate when hasCountries isn't selected" in {

      val form: UrlFormEncoded = Map.empty

      rule.validate(form) mustBe Invalid(
        Seq((Path \ "hasCountries") -> Seq(ValidationError("error.required.hasCountries.msb.branchesOrAgents")))
      )
    }

    "successfully validate when there are empty values in the seq" in {

      val form: UrlFormEncoded = Map(
        "hasCountries" -> Seq("true"),
        "countries[]" -> Seq("GB", "", "US", "")
      )

      rule.validate(form) mustBe Valid(BranchesOrAgents(Some(Seq(
        Country("United Kingdom", "GB"),
        Country("United States", "US")
      ))))
    }

    "test" in {

      val form: UrlFormEncoded = Map(
        "hasCountries" -> Seq("true"),
        "countries[0]" -> Seq("GB"),
        "countries[1]" -> Seq("")
      )

      rule.validate(form) mustBe Valid(BranchesOrAgents(Some(Seq(
        Country("United Kingdom", "GB")
      ))))
    }
  }

  "BranchesOrAgents form writes" when {
    "there is no list of countries" must {
      "set hasCountries to false" in {
        BranchesOrAgents.formW.writes(BranchesOrAgents(None)) must be (Map(
                    "hasCountries" -> Seq("false")
                    )
          )
      }
    }

    "the list of countries is empty" must {
      "set hasCountries to false" in {
        BranchesOrAgents.formW.writes(BranchesOrAgents(Some(Seq.empty[Country]))) must be (Map(
          "hasCountries" -> Seq("false")
        ))
      }
    }

    "the list of countries has entries" must {
      "set hasCountries to true and populate the countries list" in {
        BranchesOrAgents.formW.writes(BranchesOrAgents(Some(Seq(Country("TESTCOUNTRY1", "TC1"), Country("TESTCOUNTRY2", "TC2"))))) must be (Map(
        "hasCountries" -> Seq("true"),
        "countries[0]" -> Seq("TC1"),
        "countries[1]" -> Seq("TC2")
      ))}
    }
  }
}

