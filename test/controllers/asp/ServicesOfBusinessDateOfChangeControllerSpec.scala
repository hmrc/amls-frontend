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

package controllers.asp

import controllers.actions.SuccessfulAuthAction
import forms.DateOfChangeFormProvider
import models.asp._
import models.businessdetails.ActivityStartDate
import org.joda.time.LocalDate
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Injecting}
import services.asp.ServicesOfBusinessDateOfChangeService
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.{AmlsSpec, DateHelper, DependencyMocks}
import views.html.DateOfChangeView

import scala.concurrent.Future

class ServicesOfBusinessDateOfChangeControllerSpec extends AmlsSpec with MockitoSugar with Injecting {

  val emptyCache = CacheMap("", Map.empty)

  val mockService = mock[ServicesOfBusinessDateOfChangeService]

  trait Fixture extends DependencyMocks {
    self =>
    val request = addToken(authRequest)

    lazy val dateOfChange = inject[DateOfChangeView]

    val controller = new ServicesOfBusinessDateOfChangeController(
      SuccessfulAuthAction,
      ds = commonDependencies,
      cc = mockMcc,
      mockService,
      inject[DateOfChangeFormProvider],
      dateOfChange
    )
  }

  "ServicesDateOfChangeController" must {

    "on get display date of change view" in new Fixture {
      val result = controller.get()(request)
      status(result) must be(OK)
      contentAsString(result) must include(messages("summary.asp"))
    }

    "submit with valid data" in new Fixture {

      val newRequest = FakeRequest(POST, routes.ServicesOfBusinessDateOfChangeController.post.url)
      .withFormUrlEncodedBody(
        "dateOfChange.day" -> "24",
        "dateOfChange.month" -> "2",
        "dateOfChange.year" -> "1990"
      )

      when(mockService.getModelWithDate(any())(any()))
        .thenReturn(Future.successful((Asp(), Some(ActivityStartDate(new LocalDate(1990, 2, 24))))))

      when(mockService.updateAsp(any(), any(), any())(any()))
        .thenReturn(Future.successful(Some(Asp())))

      val result = controller.post()(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(controllers.asp.routes.SummaryController.get.url))
    }

    "fail submission when invalid date is supplied" in new Fixture {

      val newRequest = FakeRequest(POST, routes.ServicesOfBusinessDateOfChangeController.post.url)
      .withFormUrlEncodedBody(
        "dateOfChange.day" -> "24",
        "dateOfChange.month" -> "2",
        "dateOfChange.year" -> "foo"
      )

      val result = controller.post()(newRequest)
      status(result) must be(BAD_REQUEST)
      contentAsString(result) must include(messages("error.invalid.year"))
    }

    "fail submission when input date is before activity start date" in new Fixture {

      val newRequest = FakeRequest(POST, routes.ServicesOfBusinessDateOfChangeController.post.url)
      .withFormUrlEncodedBody(
        "dateOfChange.day" -> "24",
        "dateOfChange.month" -> "2",
        "dateOfChange.year" -> "1980"
      )

      val startDate = new LocalDate(1990, 2, 24)

      val result = controller.post()(newRequest)
      status(result) must be(BAD_REQUEST)
      contentAsString(result) must include(messages("error.expected.dateofchange.date.after.activitystartdate", DateHelper.formatDate(startDate)))
    }
  }
}
