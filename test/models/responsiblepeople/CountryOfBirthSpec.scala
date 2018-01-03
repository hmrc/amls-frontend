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

package models.responsiblepeople


import cats.data.Validated.{Invalid, Valid}
import jto.validation.{Path, ValidationError}
import models.Country
import org.scalatestplus.play.PlaySpec

class CountryOfBirthSpec extends PlaySpec {

  "CountryOfBirth" must {

    "Form" must {
      "read successfully for valid input type 'Yes'" in {
        val urlFormEncoded = Map(
          "countryOfBirth" -> Seq("false"),
          "country" -> Seq("GB")
        )
        CountryOfBirth.formRule.validate(urlFormEncoded) must be(Valid(CountryOfBirth(false, Some(Country("United Kingdom", "GB")))))
      }

      "read successfully for valid input type 'No'" in {
        val urlFormEncoded = Map(
          "countryOfBirth" -> Seq("true")
        )
        CountryOfBirth.formRule.validate(urlFormEncoded) must be(Valid(CountryOfBirth(true, None)))
      }

      "throw validation error when mandatory field not selected" in {
        val urlFormEncoded = Map(
          "countryOfBirth" -> Seq("")
        )
        CountryOfBirth.formRule.validate(urlFormEncoded) must be(Invalid(Seq((Path \ "countryOfBirth") ->
          Seq(ValidationError("error.required.rp.select.country.of.birth")))))
      }

      "throw validation error when mandatory field country od birth is selected as 'yes' and not selected country" in {
        val urlFormEncoded = Map(
          "countryOfBirth" -> Seq("false"),
          "country" -> Seq("")
        )

        CountryOfBirth.formRule.validate(urlFormEncoded) must be(Invalid(Seq((Path \ "country") ->
          Seq(ValidationError("error.required.country")))))
      }
    }

  }

}
