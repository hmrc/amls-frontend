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

package models.renewal

import jto.validation.{Invalid, Path, Valid, ValidationError}
import models.Country
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.JsSuccess

class SendTheLargestAmountsOfMoneySpec extends PlaySpec {


  "SendTheLargestAmountsOfMoney" must {

    "successfully validate the form Rule with option Yes" in {
      SendTheLargestAmountsOfMoney.formRule.validate(
        Map(
          "country_1" -> Seq("AL"),
          "country_2" -> Seq("DZ"),
          "country_3" -> Seq("AS")
        )
      ) mustBe {
        Valid(SendTheLargestAmountsOfMoney(
          
            Country("Albania", "AL"),
            Some(Country("Algeria", "DZ")),
            Some(Country("American Samoa", "AS"))
          )
        )
      }
    }


    "validate mandatory field when isOutside is  selected as Yes and country with invalid data" in {
      val json = Map("country_1" -> Seq("ABC"))

      SendTheLargestAmountsOfMoney.formRule.validate(json) must
        be(Invalid(Seq(
          (Path \ "country_1") -> Seq(ValidationError("error.required.renewal.country.name"))
        )))
    }

    "validate mandatory field for min length when isOutside is  selected as Yes and country with invalid data" in {
      val json = Map("country_1" -> Seq("A"))

      SendTheLargestAmountsOfMoney.formRule.validate(json) must
        be(Invalid(Seq(
          (Path \ "country_1") -> Seq(ValidationError("error.required.renewal.country.name"))
        )))
    }

    "validate mandatory country field" in {
      SendTheLargestAmountsOfMoney.formRule.validate(Map("country_1" -> Seq(""))) must
        be(Invalid(Seq(
          (Path \ "country_1") -> Seq(ValidationError("error.required.renewal.country.name"))
        )))
    }

    "successfully write model with formWrite" in {

      val model = SendTheLargestAmountsOfMoney(Country("United Kingdom", "GB"), Some(Country("United Kingdom", "GB")))
      SendTheLargestAmountsOfMoney.formWrites.writes(model) must
        contain allOf (
        "country_1" -> Seq("GB"),
        "country_2" -> Seq("GB")
        )
    }

    "JSON validation" must {
      "successfully validate givcen values" in {
        val data = SendTheLargestAmountsOfMoney(Country("United Kingdom", "GB"))

        SendTheLargestAmountsOfMoney.format.reads(SendTheLargestAmountsOfMoney.format.writes(data)) must
          be(JsSuccess(data))
      }
     }
  }
}
