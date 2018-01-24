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

package services

import models.autocomplete.{AutoCompleteData, NameValuePair}
import org.mockito.Matchers.{eq => eqTo, any}
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec

class AutoCompleteServiceSpec extends PlaySpec with MockitoSugar {

  trait Fixture {

    val locations = Seq(
      NameValuePair("Location 1", "location:1"),
      NameValuePair("Location 2", "location:2")
    )

    val service = new AutoCompleteService(new AutoCompleteData {
      override def fetch: Option[Seq[NameValuePair]] = Some(locations)
    })
  }

  "getLocations" must {
    "return a list of locations loaded from a resource file" in new Fixture {
      service.getLocations mustBe locations
    }
  }

}
