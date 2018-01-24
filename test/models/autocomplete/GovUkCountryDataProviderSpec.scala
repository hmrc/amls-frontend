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

package models.autocomplete

import java.io.StringBufferInputStream

import org.mockito.Matchers.{any, eq => eqTo}
import org.mockito.Mockito.when
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.Environment
import play.api.libs.json.Json

class GovUkCountryDataProviderSpec extends PlaySpec with MockitoSugar {

  trait Fixture {

    val env = mock[Environment]

    lazy val model = new GovUkCountryDataProvider(env)

    def setupEnvironment(countries: Option[Seq[NameValuePair]]) = when {
      env.resourceAsStream(any())
    } thenReturn Some(new StringBufferInputStream(
      Json.toJson(countries).toString
    ))
  }

  "fetch" must {
    "retain only the country code data" in new Fixture {
      setupEnvironment(Some(Seq(
        NameValuePair("Great Britain", "country:GB"),
        NameValuePair("Bermuda", "country:BM"),
        NameValuePair("Ghana", "country:GH"),
        NameValuePair("Kenya", "KE")
      )))

      model.fetch mustBe Some(Seq(
        NameValuePair("Great Britain", "GB"),
        NameValuePair("Bermuda", "BM"),
        NameValuePair("Ghana", "GH"),
        NameValuePair("Kenya", "KE")
      ))
    }

    "strip out countries that don't appear in the whitelist" in new Fixture {
      setupEnvironment(Some(Seq(
        NameValuePair("Haiti", "HT"),
        NameValuePair("Made up country", "MUC")
      )))

      model.fetch mustBe Some(Seq(
        NameValuePair("Haiti", "HT")
      ))
    }
  }
}
