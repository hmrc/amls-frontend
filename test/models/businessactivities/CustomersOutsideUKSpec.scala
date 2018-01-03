/*
 * Copyright 2018 HM Revenue & Customs
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

package models.businessactivities

import models.Country
import org.scalatestplus.play.PlaySpec
import jto.validation.forms._
import jto.validation._
import jto.validation.ValidationError
import play.api.libs.json.{JsPath, JsSuccess, Json}

class CustomersOutsideUKSpec extends PlaySpec {

  "CustomersOutsideUK" must {
    val rule = implicitly[Rule[UrlFormEncoded, CustomersOutsideUK]]
    val write = implicitly[Write[CustomersOutsideUK, UrlFormEncoded]]

    "round trip through Json correctly" in {

      val model: CustomersOutsideUK = CustomersOutsideUK(Some(Seq(Country("United Kingdom", "GB"))))
      Json.fromJson[CustomersOutsideUK](Json.toJson(model)) mustBe JsSuccess(model, JsPath)
    }

    "round trip through forms correctly" in {

      val model: CustomersOutsideUK = CustomersOutsideUK(Some(Seq(Country("United Kingdom", "GB"), Country("India", "IN"))))
      rule.validate(write.writes(model)) mustBe Valid(model)
    }

    "successfully validate when isOutside is false" in {

      val form: UrlFormEncoded = Map(
        "isOutside" -> Seq("false")
      )

      val model: CustomersOutsideUK = CustomersOutsideUK(None)

      rule.validate(form) mustBe Valid(model)
    }

    "successfully validate when isOutside is true and there is at least 1 country selected" in {

      val form: UrlFormEncoded = Map(
        "isOutside" -> Seq("true"),
        "countries" -> Seq("GB")
      )

      val model: CustomersOutsideUK =
        CustomersOutsideUK(
          Some(Seq(Country("United Kingdom", "GB")))
        )

      rule.validate(form) mustBe Valid(model)
    }

    "fail to validate when isOutside is true and there are no countries selected" in {

      val form: UrlFormEncoded = Map(
        "isOutside" -> Seq("true"),
        "countries" -> Seq.empty
      )

      rule.validate(form) mustBe Invalid(
        Seq((Path \ "countries") -> Seq(ValidationError("error.invalid.ba.select.country")))
      )
    }

    "fail to validate when isOutside is true and there are more than 10 countries" in {

      val form: UrlFormEncoded = Map(
        "isOutside" -> Seq("true"),
        "countries[]" -> Seq.fill(11)("GB")
      )

      rule.validate(form) mustBe Invalid(
        Seq((Path \ "countries") -> Seq(ValidationError("error.maxLength", 10)))
      )
    }

    "fail to validate when isOutside isn't selected" in {

      val form: UrlFormEncoded = Map.empty

      rule.validate(form) mustBe Invalid(
        Seq((Path \ "isOutside") -> Seq(ValidationError("error.required.ba.select.country")))
      )
    }

    "successfully validate when there are empty values in the seq" in {

      val form: UrlFormEncoded = Map(
        "isOutside" -> Seq("true"),
        "countries[]" -> Seq("GB", "", "US", "")
      )

      rule.validate(form) mustBe Valid(CustomersOutsideUK(Some(Seq(
        Country("United Kingdom", "GB"),
        Country("United States", "US")
      ))))
    }

    "test" in {

      val form: UrlFormEncoded = Map(
        "isOutside" -> Seq("true"),
        "countries[0]" -> Seq("GB"),
        "countries[1]" -> Seq("")
      )

      rule.validate(form) mustBe Valid(CustomersOutsideUK(Some(Seq(
        Country("United Kingdom", "GB")
      ))))
    }
  }
}
