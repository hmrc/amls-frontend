package models.moneyservicebusiness

import models.Country
import org.scalatestplus.play.PlaySpec
import play.api.data.mapping._
import play.api.data.mapping.forms.UrlFormEncoded
import play.api.libs.json._

class BranchesOrAgentsSpec extends PlaySpec {

  "MsbServices" must {

    val rule = implicitly[Rule[UrlFormEncoded, BranchesOrAgents]]
    val write = implicitly[Write[BranchesOrAgents, UrlFormEncoded]]

    "round trip through Json correctly" in {

      val model: BranchesOrAgents = BranchesOrAgents(Some(Seq(Country("United Kingdom", "GB"))))

      Json.fromJson[BranchesOrAgents](Json.toJson(model)) mustBe JsSuccess(model)
    }

    "round trip through forms correctly" in {

      val model: BranchesOrAgents = BranchesOrAgents(Some(Seq(Country("United Kingdom", "GB"))))

      rule.validate(write.writes(model)) mustBe Success(model)
    }

    "successfully validate when hasCountries is false" in {

      val form: UrlFormEncoded = Map(
        "hasCountries" -> Seq("false")
      )

      val model: BranchesOrAgents = BranchesOrAgents(None)

      rule.validate(form) mustBe Success(model)
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

      rule.validate(form) mustBe Success(model)
    }

    "fail to validate when hasCountries is true and there are no countries selected" in {

      val form: UrlFormEncoded = Map(
        "hasCountries" -> Seq("true"),
        "countries" -> Seq.empty
      )

      rule.validate(form) mustBe Failure(
        Seq((Path \ "countries") -> Seq(ValidationError("error.invalid.countries.msb.branchesOrAgents")))
      )
    }

    "fail to validate when hasCountries is true and there are more than 10 countries" in {

      val form: UrlFormEncoded = Map(
        "hasCountries" -> Seq("true"),
        "countries[]" -> Seq.fill(11)("GB")
      )

      rule.validate(form) mustBe Failure(
        Seq((Path \ "countries") -> Seq(ValidationError("error.maxLength", 10)))
      )
    }

    "fail to validate when hasCountries isn't selected" in {

      val form: UrlFormEncoded = Map.empty

      rule.validate(form) mustBe Failure(
        Seq((Path \ "hasCountries") -> Seq(ValidationError("error.required.hasCountries.msb.branchesOrAgents")))
      )
    }

    "successfully validate when there are empty values in the seq" in {

      val form: UrlFormEncoded = Map(
        "hasCountries" -> Seq("true"),
        "countries[]" -> Seq("GB", "", "US", "")
      )

      rule.validate(form) mustBe Success(BranchesOrAgents(Some(Seq(
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

      rule.validate(form) mustBe Success(BranchesOrAgents(Some(Seq(
        Country("United Kingdom", "GB")
      ))))
    }
  }
}

