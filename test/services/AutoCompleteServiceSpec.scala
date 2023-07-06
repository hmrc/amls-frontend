/*
 * Copyright 2023 HM Revenue & Customs
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

package services

import models.autocomplete.{CountryDataProvider, NameValuePair}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import uk.gov.hmrc.govukfrontend.views.viewmodels.select.SelectItem

class AutoCompleteServiceSpec extends PlaySpec with MockitoSugar {

  trait Fixture {

    val locations = Seq(
      NameValuePair("United Kingdom", "GB"),
      NameValuePair("United States", "US"),
      NameValuePair("Spain", "ES")
    )

    val service = new AutoCompleteService(new CountryDataProvider {
      override def fetch: Option[Seq[NameValuePair]] = Some(locations)
    })
  }

  "getLocations" must {
    "return a list of locations loaded from a resource file" in new Fixture {
      service.getCountries mustBe Some(locations.sortBy(_.name))
    }
  }

  "formOptions" must {
    "transform the result of .getLocations to a list of SelectItems" in new Fixture {
      service.formOptions mustBe Seq(
        SelectItem(),
        SelectItem(Some("ES"), "Spain"),
        SelectItem(Some("GB"), "United Kingdom"),
        SelectItem(Some("US"), "United States")
      )
    }
  }

  "formOptionsExcludeUK" must {
    "product the result of .formOptions without United Kingdom" in new Fixture {

      val result: Seq[SelectItem] = service.formOptionsExcludeUK

      result mustBe Seq(
        SelectItem(),
        SelectItem(Some("ES"), "Spain"),
        SelectItem(Some("US"), "United States")
      )

      result mustNot contain(SelectItem(Some("GB"), "United Kingdom"))
    }
  }
}
