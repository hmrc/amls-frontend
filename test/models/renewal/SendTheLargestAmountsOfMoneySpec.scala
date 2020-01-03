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

package models.renewal

import jto.validation.forms.UrlFormEncoded
import jto.validation.{Invalid, Path, Rule, VA, Valid, ValidationError, Write}
import models.Country
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsSuccess, Json}

class SendTheLargestAmountsOfMoneySpec extends PlaySpec {


  "SendTheLargestAmountsOfMoney" must {

    val rule: Rule[UrlFormEncoded, SendTheLargestAmountsOfMoney] = implicitly
    val write: Write[SendTheLargestAmountsOfMoney, UrlFormEncoded] = implicitly

    "roundtrip through json" in {

      val model: SendTheLargestAmountsOfMoney =
        SendTheLargestAmountsOfMoney(Seq(Country("United Kingdom", "GB")))

      Json.fromJson[SendTheLargestAmountsOfMoney](Json.toJson(model)) mustEqual JsSuccess(model)
    }

    "correctly parse the json if country_1 and country_3 fields provided" in {
      val json = Json.obj("country_1" -> "GB", "country_3" -> "IN")
      val expected = SendTheLargestAmountsOfMoney(Seq(Country("United Kingdom", "GB"), Country("India", "IN")))

      Json.fromJson[SendTheLargestAmountsOfMoney](json) mustEqual JsSuccess(expected)
    }

    "roundtrip through forms" in {

      val model: SendTheLargestAmountsOfMoney =
        SendTheLargestAmountsOfMoney(Seq(Country("United Kingdom", "GB")))

      rule.validate(write.writes(model)) mustEqual Valid(model)
    }

    "fail to validate when there are no countries" in {

      val form: UrlFormEncoded = Map(
        "largestAmountsOfMoney" -> Seq.empty
      )

      rule.validate(form) mustEqual Invalid(
        Seq((Path \ "largestAmountsOfMoney") -> Seq(ValidationError("error.required.renewal.largest.amounts.country")))
      )
    }

    "fail to validate when there are more than 3 countries" in {

      // scalastyle:off magic.number
      val form: UrlFormEncoded = Map(
        "largestAmountsOfMoney[]" -> Seq.fill(4)("GB")
      )

      rule.validate(form) mustEqual Invalid(
        Seq((Path \ "largestAmountsOfMoney") -> Seq(ValidationError("error.maxLength", 3)))
      )
    }
  }

  "SendTheLargestAmountsOfMoney Form Writes" when {
    "an item is repeated" must {
      "serialise all items correctly" in {
        SendTheLargestAmountsOfMoney.formW.writes(SendTheLargestAmountsOfMoney(List(
          Country("Country2", "BB"),
          Country("Country1", "AA"),
          Country("Country1", "AA")
        ))) must be (
          Map(
            "largestAmountsOfMoney[0]" -> List("BB"),
            "largestAmountsOfMoney[1]" -> List("AA"),
            "largestAmountsOfMoney[2]" -> List("AA")
          )
        )
      }
    }
  }

  "SendTheLargestAmountsOfMoney Form Reads" when {
    "all countries are valid" must {
      "Successfully read from the form" in {

        SendTheLargestAmountsOfMoney.formR.validate(
          Map(
            "largestAmountsOfMoney[0]" -> Seq("GB"),
            "largestAmountsOfMoney[1]" -> Seq("MK"),
            "largestAmountsOfMoney[2]" -> Seq("JO")
          )
        ) must be(Valid(SendTheLargestAmountsOfMoney(Seq(
          Country("United Kingdom", "GB"),
          Country("Macedonia, the Former Yugoslav Republic of", "MK"),
          Country("Jordan", "JO")
        ))))
      }
    }

    "the second country is invalid" must {
      "fail validation" in {

        val x: VA[SendTheLargestAmountsOfMoney] = SendTheLargestAmountsOfMoney.formR.validate(
          Map(
            "largestAmountsOfMoney[0]" -> Seq("GB"),
            "largestAmountsOfMoney[1]" -> Seq("hjjkhjkjh"),
            "largestAmountsOfMoney[2]" -> Seq("MK")
          )
        )
        x must be (Invalid(Seq((Path \ "largestAmountsOfMoney" \ 1) -> Seq(ValidationError("error.invalid.country")))))
      }
    }
  }
}
