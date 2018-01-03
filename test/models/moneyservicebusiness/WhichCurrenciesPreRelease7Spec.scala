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

package models.moneyservicebusiness

import cats.data.Validated.Valid
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.libs.json.Json
import play.api.test.FakeApplication

class WhichCurrenciesPreRelease7Spec extends PlaySpec with OneAppPerSuite {

  override lazy val app = FakeApplication(additionalConfiguration = Map("microservice.services.feature-toggle.release7" -> false))

  "WhichCurrencies" must {

    val model = WhichCurrencies(
      Seq("USD", "CHF", "EUR"),
      None,
      Some(BankMoneySource("Bank names")),
      Some(WholesalerMoneySource("wholesaler names")),
      customerMoneySource = Some(true))

    "pass form validation" when {

      "no foreign currency flag is set" in {

        val formData = Map(
          "currencies[0]" -> Seq("USD"),
          "currencies[1]" -> Seq("CHF"),
          "currencies[2]" -> Seq("EUR"),
          "bankMoneySource" -> Seq("Yes"),
          "bankNames" -> Seq("Bank names"),
          "wholesalerMoneySource" -> Seq("Yes"),
          "wholesalerNames" -> Seq("wholesaler names"),
          "customerMoneySource" -> Seq("Yes")
        )

        WhichCurrencies.formR.validate(formData) must be(Valid(model))

      }

    }

    "serialize the json properly" when {
      "no foreign currency flag is set" in {
        Json.toJson(model).as[WhichCurrencies] must be(model)
      }
    }

  }

}
