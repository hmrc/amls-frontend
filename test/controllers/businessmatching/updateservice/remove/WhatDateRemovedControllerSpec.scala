/*
 * Copyright 2019 HM Revenue & Customs
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

import controllers.actions.SuccessfulAuthAction
import models.DateOfChange
import models.flowmanagement.{RemoveBusinessTypeFlowModel, WhatDateRemovedPageId}
import org.mockito.Matchers._
import org.joda.time.LocalDate
import org.jsoup.Jsoup
import play.api.i18n.Messages
import play.api.test.Helpers._
import utils.{AmlsSpec, AuthorisedFixture, DependencyMocks, DependencyMocksNewAuth}

class WhatDateRemovedControllerSpec extends AmlsSpec {

  trait Fixture extends AuthorisedFixture with DependencyMocksNewAuth {
    self =>

    val request = addToken(authRequest)

    val controller = new WhatDateRemovedController(
      authAction = SuccessfulAuthAction,
      dataCacheConnector = mockCacheConnector,
      router = createRouter[RemoveBusinessTypeFlowModel]
    )
  }

  "WhatDateRemovedController" when {

    "get is called" must {
      "return OK with date_of_change view" in new Fixture {

        mockCacheFetch[RemoveBusinessTypeFlowModel](Some(RemoveBusinessTypeFlowModel()))

        val result = controller.get()(request)
        status(result) must be(OK)
        Jsoup.parse(contentAsString(result)).title() must include(Messages("dateofchange.title"))
      }

      "Not display the return link" in new Fixture {
        mockCacheFetch[RemoveBusinessTypeFlowModel](Some(RemoveBusinessTypeFlowModel()))

        val result = controller.get()(request)
        status(result) must be(OK)
        Jsoup.parse(contentAsString(result)).title() mustNot include(Messages("link.return.registration.progress"))
      }

      "display the date when it is already in the data cache" in new Fixture {
        val today = LocalDate.now

        mockCacheFetch[RemoveBusinessTypeFlowModel](
          Some(RemoveBusinessTypeFlowModel(dateOfChange = Some(DateOfChange(today)))),
          Some(RemoveBusinessTypeFlowModel.key))

        val result = controller.get()(request)
        status(result) must be(OK)
        Jsoup.parse(contentAsString(result)).getElementById("dateOfChange-day").attr("value") mustBe today.getDayOfMonth.toString
        Jsoup.parse(contentAsString(result)).getElementById("dateOfChange-month").attr("value") mustBe today.getMonthOfYear.toString
        Jsoup.parse(contentAsString(result)).getElementById("dateOfChange-year").attr("value") mustBe today.getYear.toString
      }
    }


    "post is called" must {
      "redirect to next page" in new Fixture {
        val today = LocalDate.now
        mockCacheUpdate(Some(RemoveBusinessTypeFlowModel.key), RemoveBusinessTypeFlowModel(dateOfChange = Some(DateOfChange(today))))

        val result = controller.post()(request.withFormUrlEncodedBody(
          "dateOfChange.day" -> today.getDayOfMonth.toString,
          "dateOfChange.month" -> today.getMonthOfYear.toString,
          "dateOfChange.year" -> today.getYear.toString
        ))

        status(result) mustBe SEE_OTHER
     }

      "save the data to the flow model" in new Fixture {
        val today = LocalDate.now
        mockCacheUpdate(Some(RemoveBusinessTypeFlowModel.key), RemoveBusinessTypeFlowModel(dateOfChange = Some(DateOfChange(today.minusDays(10)))))


        val result = await {
          controller.post()(request.withFormUrlEncodedBody(
            "dateOfChange.day" -> today.getDayOfMonth.toString,
            "dateOfChange.month" -> today.getMonthOfYear.toString,
            "dateOfChange.year" -> today.getYear.toString
          ))
        }

        controller.router.verify(any(), WhatDateRemovedPageId, RemoveBusinessTypeFlowModel(dateOfChange = Some(DateOfChange(today))))
      }


      "return badRequest" must {
        "on invalid request" in new Fixture {
          val result = controller.post()(request)
          status(result) mustBe BAD_REQUEST
        }
      }
    }
  }
}
