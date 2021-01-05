/*
 * Copyright 2021 HM Revenue & Customs
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
import models.Country
import models.businesscustomer.{Address, ReviewDetails}
import models.businessdetails._
import models.businessmatching.{BusinessMatching, BusinessType}
import org.joda.time.LocalDate
import org.jsoup.Jsoup
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import play.api.i18n.Messages
import play.api.test.Helpers._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.{AmlsSpec, DependencyMocks}
import views.html.businessdetails.activity_start_date

import scala.concurrent.Future

class ActivityStartDateControllerSpec extends AmlsSpec with MockitoSugar {

  trait Fixture extends DependencyMocks {
    self => val request = addToken(authRequest)

    lazy val view = app.injector.instanceOf[activity_start_date]
    val controller = new ActivityStartDateController (
      dataCache = mock[DataCacheConnector],
      authAction = SuccessfulAuthAction,
      ds = commonDependencies,
      cc = mockMcc,
      activity_start_date = view,
      errorView)
  }

  // scalastyle:off
  private val startDate = ActivityStartDate(new LocalDate(2010, 2, 22))
  private val businessDetails = BusinessDetails(None, Some(startDate), None, None)

  val emptyCache = CacheMap("", Map.empty)

  "ActivityStartDateController" must {

    "Get Option:" must {

      "load ActivityStartDate page" in new Fixture {

        when(controller.dataCache.fetch[BusinessDetails](any(), any())(any(), any()))
          .thenReturn(Future.successful(Some(businessDetails)))
        val result = controller.get()(request)
        status(result) must be(OK)
        contentAsString(result) must include(Messages("businessdetails.activity.start.date.title"))
      }

      "load ActivityStartDate with pre-populated data" in new Fixture {

        when(controller.dataCache.fetch[BusinessDetails](any(), any())(any(), any()))
          .thenReturn(Future.successful(Some(businessDetails)))
        val result = controller.get()(request)
        val document = Jsoup.parse(contentAsString(result))
        document.select("input[name=startDate.day]").`val` must include("22")
        document.select("input[name=startDate.month]").`val` must include("2")

      }
    }

    "Post" must {

      "successfully redirect to ConfirmRegisteredOfficeController if not org or partnership" in new Fixture {

        val newRequest = requestWithUrlEncodedBody(
          "startDate.day" -> "12",
          "startDate.month" -> "5",
          "startDate.year" -> "1999"
        )

        val reviewDtls = ReviewDetails("BusinessName", Some(BusinessType.SoleProprietor),
          Address("line1", "line2", Some("line3"), Some("line4"), Some("AA11 1AA"), Country("United Kingdom", "GB")), "ghghg")

        override val mockCacheMap = mock[CacheMap]

        when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
          .thenReturn(Some(BusinessMatching(Some(reviewDtls))))
        when(mockCacheMap.getEntry[BusinessDetails](BusinessDetails.key))
          .thenReturn(Some(BusinessDetails(Some(PreviouslyRegisteredNo))))

        when(controller.dataCache.fetchAll(any())(any[HeaderCarrier]))
          .thenReturn(Future.successful(Some(mockCacheMap)))
        when(controller.dataCache.save(any(), any(), any())(any(), any())).thenReturn(Future.successful(emptyCache))

        val result = controller.post()(newRequest)
        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some(controllers.businessdetails.routes.ConfirmRegisteredOfficeController.get().url))
      }

      "successfully redirect to VATRegisteredController org or partnership" in new Fixture {

        val newRequest = requestWithUrlEncodedBody(
          "startDate.day" -> "12",
          "startDate.month" -> "5",
          "startDate.year" -> "1999"
        )

        val reviewDtls = ReviewDetails("BusinessName", Some(BusinessType.LimitedCompany),
          Address("line1", "line2", Some("line3"), Some("line4"), Some("AA11 1AA"), Country("United Kingdom", "GB")), "ghghg")

        when(controller.dataCache.save(any(), any(), any())(any(), any())).thenReturn(Future.successful(emptyCache))

        override val mockCacheMap = mock[CacheMap]

        when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
          .thenReturn(Some(BusinessMatching(Some(reviewDtls))))
        when(mockCacheMap.getEntry[BusinessDetails](BusinessDetails.key))
          .thenReturn(Some(BusinessDetails(Some(PreviouslyRegisteredNo))))

        when(controller.dataCache.fetchAll(any[String])(any[HeaderCarrier]))
          .thenReturn(Future.successful(Some(mockCacheMap)))

        val result = controller.post()(newRequest)
        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some(controllers.businessdetails.routes.VATRegisteredController.get().url))
      }

      "show error with invalid" in new Fixture {
        val newRequest = requestWithUrlEncodedBody(
          "startDate.day" -> "",
          "startDate.month" -> "",
          "startDate.year" -> ""
        )
        when(controller.dataCache.fetch[BusinessDetails](any(), any())(any(), any()))
          .thenReturn(Future.successful(Some(businessDetails)))

        val result = controller.post()(newRequest)
        status(result) must be(BAD_REQUEST)
        contentAsString(result) must include(Messages("error.required.date.year.month.day"))

      }

      "show error with year field too short" in new Fixture {
        val newRequest = requestWithUrlEncodedBody(
          "startDate.day" -> "1",
          "startDate.month" -> "3",
          "startDate.year" -> "16"
        )
        when(controller.dataCache.fetch[BusinessDetails](any(), any())(any(), any()))
          .thenReturn(Future.successful(Some(businessDetails)))

        val result = controller.post()(newRequest)
        status(result) must be(BAD_REQUEST)
        contentAsString(result) must include(Messages("error.invalid.date.after.1900"))
      }

      "show error with year field too long" in new Fixture {
        val newRequest = requestWithUrlEncodedBody(
          "startDate.day" -> "1",
          "startDate.month" -> "3",
          "startDate.year" -> "19782"
        )
        when(controller.dataCache.fetch[BusinessDetails](any(), any())(any(), any()))
          .thenReturn(Future.successful(Some(businessDetails)))

        val result = controller.post()(newRequest)
        status(result) must be(BAD_REQUEST)
        contentAsString(result) must include(Messages("error.invalid.date.before.2100"))
      }
    }
  }
}
