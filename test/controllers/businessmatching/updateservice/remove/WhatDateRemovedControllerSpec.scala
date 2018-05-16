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

package controllers.businessmatching.updateservice.remove



import models.DateOfChange
import models.flowmanagement.RemoveBusinessTypeFlowModel
import org.joda.time.LocalDate
import org.jsoup.Jsoup
import play.api.i18n.Messages
import play.api.test.Helpers._
import utils.{AmlsSpec, AuthorisedFixture, DependencyMocks}

class WhatDateRemovedControllerSpec extends AmlsSpec {

  trait Fixture extends AuthorisedFixture with DependencyMocks {
    self =>

    val request = addToken(authRequest)

    val controller = new WhatDateRemovedController(
      authConnector = self.authConnector,
      dataCacheConnector = mockCacheConnector
    )
  }

  "UpdateServiceDateOfChangeController" when {

    "get is called" must {
      "return OK with date_of_change view" in new Fixture {

        mockCacheFetch[RemoveBusinessTypeFlowModel](Some(RemoveBusinessTypeFlowModel()))

        val result = controller.get()(request)
        status(result) must be(OK)
        Jsoup.parse(contentAsString(result)).title() must include(Messages("dateofchange.title"))
      }

      "display the date when it is already in the data cache" in new Fixture {
        val today = LocalDate.now
        mockCacheFetch[RemoveBusinessTypeFlowModel](Some(RemoveBusinessTypeFlowModel(dateOfChange = Some(DateOfChange(today)))), Some(RemoveBusinessTypeFlowModel.key))

        val result = controller.get()(request)
        status(result) must be(OK)
        Jsoup.parse(contentAsString(result)).getElementById("dateOfChange-day").attr("value") mustBe today.getDayOfMonth.toString
        Jsoup.parse(contentAsString(result)).getElementById("dateOfChange-month").attr("value") mustBe today.getMonthOfYear.toString
        Jsoup.parse(contentAsString(result)).getElementById("dateOfChange-year").attr("value") mustBe today.getYear.toString
      }
    }

  }


}