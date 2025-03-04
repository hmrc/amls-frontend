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

package controllers.businessmatching.updateservice.remove

import controllers.actions.SuccessfulAuthAction
import forms.DateOfChangeFormProvider
import models.DateOfChange
import models.flowmanagement.{RemoveBusinessTypeFlowModel, WhatDateRemovedPageId}
import org.jsoup.Jsoup
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Injecting}
import utils.{AmlsSpec, DependencyMocks}
import views.html.DateOfChangeView

import java.time.LocalDate

class WhatDateRemovedControllerSpec extends AmlsSpec with Injecting {

  trait Fixture extends DependencyMocks {
    self =>

    val request    = addToken(authRequest)
    lazy val view  = inject[DateOfChangeView]
    val controller = new WhatDateRemovedController(
      authAction = SuccessfulAuthAction,
      ds = commonDependencies,
      dataCacheConnector = mockCacheConnector,
      router = createRouter[RemoveBusinessTypeFlowModel],
      cc = mockMcc,
      formProvider = inject[DateOfChangeFormProvider],
      view = view
    )
  }

  "WhatDateRemovedController" when {

    "get is called" must {
      "return OK with DateOfChangeView" in new Fixture {

        mockCacheFetch[RemoveBusinessTypeFlowModel](Some(RemoveBusinessTypeFlowModel()))

        val result = controller.get()(request)
        status(result)                               must be(OK)
        Jsoup.parse(contentAsString(result)).title() must include(messages("dateofchange.title"))
      }

      "Not display the return link" in new Fixture {
        mockCacheFetch[RemoveBusinessTypeFlowModel](Some(RemoveBusinessTypeFlowModel()))

        val result = controller.get()(request)
        status(result) must be(OK)
        Jsoup.parse(contentAsString(result)).title() mustNot include(messages("link.return.registration.progress"))
      }

      "display the date when it is already in the data cache" in new Fixture {
        val today = LocalDate.now

        mockCacheFetch[RemoveBusinessTypeFlowModel](
          Some(RemoveBusinessTypeFlowModel(dateOfChange = Some(DateOfChange(today)))),
          Some(RemoveBusinessTypeFlowModel.key)
        )

        val result = controller.get()(request)
        status(result) must be(OK)
        Jsoup
          .parse(contentAsString(result))
          .getElementById("dateOfChange.day")
          .attr("value") mustBe today.getDayOfMonth.toString
        Jsoup
          .parse(contentAsString(result))
          .getElementById("dateOfChange.month")
          .attr("value") mustBe today.getMonthValue.toString
        Jsoup
          .parse(contentAsString(result))
          .getElementById("dateOfChange.year")
          .attr("value") mustBe today.getYear.toString
      }
    }

    "post is called" must {
      "redirect to next page" in new Fixture {
        val today = LocalDate.now
        mockCacheUpdate(
          Some(RemoveBusinessTypeFlowModel.key),
          RemoveBusinessTypeFlowModel(dateOfChange = Some(DateOfChange(today)))
        )

        val result = controller.post()(
          FakeRequest(POST, routes.WhatDateRemovedController.post().url).withFormUrlEncodedBody(
            "dateOfChange.day"   -> today.getDayOfMonth.toString,
            "dateOfChange.month" -> today.getMonthValue.toString,
            "dateOfChange.year"  -> today.getYear.toString
          )
        )

        status(result) mustBe SEE_OTHER
      }

      "save the data to the flow model" in new Fixture {
        val today = LocalDate.now
        mockCacheUpdate(
          Some(RemoveBusinessTypeFlowModel.key),
          RemoveBusinessTypeFlowModel(dateOfChange = Some(DateOfChange(today.minusDays(10))))
        )

        await {
          controller.post()(
            FakeRequest(POST, routes.WhatDateRemovedController.post().url).withFormUrlEncodedBody(
              "dateOfChange.day"   -> today.getDayOfMonth.toString,
              "dateOfChange.month" -> today.getMonthValue.toString,
              "dateOfChange.year"  -> today.getYear.toString
            )
          )
        }

        controller.router.verify(
          "internalId",
          WhatDateRemovedPageId,
          RemoveBusinessTypeFlowModel(dateOfChange = Some(DateOfChange(today)))
        )
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
