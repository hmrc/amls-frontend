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

package controllers.businessdetails

import connectors.DataCacheConnector
import controllers.actions.SuccessfulAuthAction
import forms.businessdetails.ActivityStartDateFormProvider
import models.Country
import models.businesscustomer.{Address, ReviewDetails}
import models.businessdetails._
import models.businessmatching.{BusinessMatching, BusinessType}
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.cache.Cache
import utils.{AmlsSpec, DependencyMocks}
import views.html.businessdetails.ActivityStartDateView

import java.time.LocalDate
import scala.concurrent.Future

class ActivityStartDateControllerSpec extends AmlsSpec with MockitoSugar {

  trait Fixture extends DependencyMocks {
    self =>
    val request = addToken(authRequest)

    lazy val view  = app.injector.instanceOf[ActivityStartDateView]
    val controller = new ActivityStartDateController(
      dataCache = mock[DataCacheConnector],
      authAction = SuccessfulAuthAction,
      ds = commonDependencies,
      cc = mockMcc,
      activity_start_date = view,
      formProvider = app.injector.instanceOf[ActivityStartDateFormProvider],
      error = errorView
    )
  }

  // scalastyle:off
  private val startDate       = ActivityStartDate(LocalDate.of(2010, 2, 22))
  private val businessDetails = BusinessDetails(None, Some(startDate), None, None)

  val emptyCache = Cache.empty

  "ActivityStartDateController" must {

    "Get Option:" must {

      "load ActivityStartDate page" in new Fixture {

        when(controller.dataCache.fetch[BusinessDetails](any(), any())(any()))
          .thenReturn(Future.successful(None))
        val result = controller.get()(request)
        status(result)          must be(OK)
        contentAsString(result) must include(messages("businessdetails.activity.start.date.title"))
      }

      "load ActivityStartDate with pre-populated data" in new Fixture {

        when(controller.dataCache.fetch[BusinessDetails](any(), any())(any()))
          .thenReturn(Future.successful(Some(businessDetails)))
        val result   = controller.get()(request)
        val document = Jsoup.parse(contentAsString(result))
        document.select("input[name=value.day]").`val`   must include("22")
        document.select("input[name=value.month]").`val` must include("2")

      }
    }

    "Post" must {

      "successfully redirect to ConfirmRegisteredOfficeController if not org or partnership" in new Fixture {

        val newRequest = FakeRequest(POST, routes.ActivityStartDateController.post().url).withFormUrlEncodedBody(
          "value.day"   -> "12",
          "value.month" -> "5",
          "value.year"  -> "1999"
        )

        val reviewDtls = ReviewDetails(
          "BusinessName",
          Some(BusinessType.SoleProprietor),
          Address(
            "line1",
            Some("line2"),
            Some("line3"),
            Some("line4"),
            Some("AA11 1AA"),
            Country("United Kingdom", "GB")
          ),
          "ghghg"
        )

        override val mockCacheMap = mock[Cache]

        when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
          .thenReturn(Some(BusinessMatching(Some(reviewDtls))))
        when(mockCacheMap.getEntry[BusinessDetails](BusinessDetails.key))
          .thenReturn(Some(BusinessDetails(Some(PreviouslyRegisteredNo))))

        when(controller.dataCache.fetchAll(any()))
          .thenReturn(Future.successful(Some(mockCacheMap)))
        when(controller.dataCache.save(any(), any(), any())(any())).thenReturn(Future.successful(emptyCache))

        val result = controller.post()(newRequest)
        status(result)           must be(SEE_OTHER)
        redirectLocation(result) must be(
          Some(controllers.businessdetails.routes.ConfirmRegisteredOfficeController.get().url)
        )
      }

      "successfully redirect to VATRegisteredController org or partnership" in new Fixture {

        val newRequest = FakeRequest(POST, routes.ActivityStartDateController.post().url).withFormUrlEncodedBody(
          "value.day"   -> "12",
          "value.month" -> "5",
          "value.year"  -> "1999"
        )

        val reviewDtls = ReviewDetails(
          "BusinessName",
          Some(BusinessType.LimitedCompany),
          Address(
            "line1",
            Some("line2"),
            Some("line3"),
            Some("line4"),
            Some("AA11 1AA"),
            Country("United Kingdom", "GB")
          ),
          "ghghg"
        )

        when(controller.dataCache.save(any(), any(), any())(any())).thenReturn(Future.successful(emptyCache))

        override val mockCacheMap = mock[Cache]

        when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
          .thenReturn(Some(BusinessMatching(Some(reviewDtls))))
        when(mockCacheMap.getEntry[BusinessDetails](BusinessDetails.key))
          .thenReturn(Some(BusinessDetails(Some(PreviouslyRegisteredNo))))

        when(controller.dataCache.fetchAll(any[String]))
          .thenReturn(Future.successful(Some(mockCacheMap)))

        val result = controller.post()(newRequest)
        status(result)           must be(SEE_OTHER)
        redirectLocation(result) must be(Some(controllers.businessdetails.routes.VATRegisteredController.get().url))
      }

      "show error with empty form" in new Fixture {

        val newRequest = FakeRequest(POST, routes.ActivityStartDateController.post().url).withFormUrlEncodedBody(
          "value.day"   -> "",
          "value.month" -> "",
          "value.year"  -> ""
        )
        when(controller.dataCache.fetch[BusinessDetails](any(), any())(any()))
          .thenReturn(Future.successful(Some(businessDetails)))

        val result = controller.post()(newRequest)
        status(result)          must be(BAD_REQUEST)
        contentAsString(result) must include(messages("error.required.date.required.all"))

      }

      "show error with one empty field" in new Fixture {

        val newRequest = FakeRequest(POST, routes.ActivityStartDateController.post().url).withFormUrlEncodedBody(
          "value.day"   -> "",
          "value.month" -> "12",
          "value.year"  -> "1990"
        )
        when(controller.dataCache.fetch[BusinessDetails](any(), any())(any()))
          .thenReturn(Future.successful(Some(businessDetails)))

        val result = controller.post()(newRequest)
        status(result)          must be(BAD_REQUEST)
        contentAsString(result) must include(messages("error.required.date.required.one", "day"))

      }

      "show error with two empty fields" in new Fixture {

        val newRequest = FakeRequest(POST, routes.ActivityStartDateController.post().url).withFormUrlEncodedBody(
          "value.day"   -> "",
          "value.month" -> "11",
          "value.year"  -> ""
        )
        when(controller.dataCache.fetch[BusinessDetails](any(), any())(any()))
          .thenReturn(Future.successful(Some(businessDetails)))

        val result = controller.post()(newRequest)
        status(result)          must be(BAD_REQUEST)
        contentAsString(result) must include(messages("error.required.date.required.two", "day", "year"))

      }

      "show error with year field too short" in new Fixture {
        val newRequest = FakeRequest(POST, routes.ActivityStartDateController.post().url).withFormUrlEncodedBody(
          "value.day"   -> "1",
          "value.month" -> "3",
          "value.year"  -> "1666"
        )
        when(controller.dataCache.fetch[BusinessDetails](any(), any())(any()))
          .thenReturn(Future.successful(Some(businessDetails)))

        val result = controller.post()(newRequest)
        status(result)          must be(BAD_REQUEST)
        contentAsString(result) must include(messages("error.invalid.date.after.1900"))
      }

      "show error with year field too long" in new Fixture {
        val newRequest = FakeRequest(POST, routes.ActivityStartDateController.post().url).withFormUrlEncodedBody(
          "value.day"   -> "1",
          "value.month" -> "3",
          "value.year"  -> "9782"
        )
        when(controller.dataCache.fetch[BusinessDetails](any(), any())(any()))
          .thenReturn(Future.successful(Some(businessDetails)))

        val result = controller.post()(newRequest)
        status(result)          must be(BAD_REQUEST)
        contentAsString(result) must include(messages("error.invalid.date.before.2100"))
      }

      "return NOT_FOUND when IndexOutOfBoundsException is thrown" in new Fixture {

        val newRequest = FakeRequest(POST, routes.ActivityStartDateController.post(true).url)
          .withFormUrlEncodedBody(
            "value.day"   -> "12",
            "value.month" -> "5",
            "value.year"  -> "1999"
          )

        val reviewDtls = ReviewDetails(
          "BusinessName",
          Some(BusinessType.SoleProprietor),
          Address(
            "line1",
            Some("line2"),
            Some("line3"),
            Some("line4"),
            Some("AA11 1AA"),
            Country("United Kingdom", "GB")
          ),
          "ghghg"
        )

        override val mockCacheMap = mock[Cache]

        when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
          .thenReturn(Some(BusinessMatching(Some(reviewDtls))))
        when(mockCacheMap.getEntry[BusinessDetails](BusinessDetails.key))
          .thenReturn(Some(BusinessDetails(Some(PreviouslyRegisteredNo))))

        when(controller.dataCache.fetchAll(any()))
          .thenReturn(Future.successful(Some(mockCacheMap)))
        when(controller.dataCache.save(any(), any(), any())(any()))
          .thenThrow(new IndexOutOfBoundsException("error"))

        val result = controller.post()(newRequest)
        status(result) must be(NOT_FOUND)
      }

      "redirect to Summary page when in edit mode" in new Fixture {

        val newRequest = FakeRequest(POST, routes.ActivityStartDateController.post(true).url)
          .withFormUrlEncodedBody(
            "value.day"   -> "12",
            "value.month" -> "5",
            "value.year"  -> "1999"
          )

        val reviewDtls = ReviewDetails(
          "BusinessName",
          Some(BusinessType.SoleProprietor),
          Address(
            "line1",
            Some("line2"),
            Some("line3"),
            Some("line4"),
            Some("AA11 1AA"),
            Country("United Kingdom", "GB")
          ),
          "ghghg"
        )

        override val mockCacheMap = mock[Cache]

        when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
          .thenReturn(Some(BusinessMatching(Some(reviewDtls))))
        when(mockCacheMap.getEntry[BusinessDetails](BusinessDetails.key))
          .thenReturn(Some(BusinessDetails(Some(PreviouslyRegisteredNo))))

        when(controller.dataCache.fetchAll(any()))
          .thenReturn(Future.successful(Some(mockCacheMap)))
        when(controller.dataCache.save(any(), any(), any())(any())).thenReturn(Future.successful(emptyCache))

        val result = controller.post(true)(newRequest)
        status(result)           must be(SEE_OTHER)
        redirectLocation(result) must be(Some(controllers.businessdetails.routes.SummaryController.get.url))
      }
    }
  }
}
