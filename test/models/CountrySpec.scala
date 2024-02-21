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

package models

import org.scalacheck.Gen
import org.scalatest.{MustMatchers, WordSpec}
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import play.api.libs.json.JsString

class CountrySpec extends WordSpec with MustMatchers with ScalaCheckDrivenPropertyChecks {

  "Country" when {

    "reading json" must {

      "return an empty country" when {

        "Json String is empty" in {

          JsString("").as[Country] mustBe Country("", "")
        }
      }

      "return a country" when {

        "a valid code is given" in {

          forAll(Gen.oneOf(countries)) { country =>
            JsString(country.code).as[Country] mustBe country
          }
        }
      }

      "return an error when an invalid code is given" in {
        JsString("ZZ").validate[Country].isError mustBe true
      }
    }
  }
}
