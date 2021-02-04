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

package controllers.asp

import controllers.actions.SuccessfulAuthAction
import models.asp._
import models.businessdetails.{ActivityStartDate, BusinessDetails}
import org.joda.time.LocalDate
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import play.api.i18n.Messages
import play.api.test.Helpers._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.{AmlsSpec, DependencyMocks}
import views.html.date_of_change

import scala.concurrent.Future

class ServicesOfBusinessDateOfChangeControllerSpec extends AmlsSpec with MockitoSugar {

  val emptyCache = CacheMap("", Map.empty)

  trait Fixture extends DependencyMocks {
    self =>
    val request = addToken(authRequest)

    lazy val dateOfChange = app.injector.instanceOf[date_of_change]

    val controller = new ServicesOfBusinessDateOfChangeController(mockCacheConnector, SuccessfulAuthAction, ds = commonDependencies, cc = mockMcc, dateOfChange)
  }

  "ServicesDateOfChangeController" must {

    "on get display date of change view" in new Fixture {
      val result = controller.get()(request)
      status(result) must be(OK)
      contentAsString(result) must include(Messages("summary.asp"))
    }

    "submit with valid data" in new Fixture {

      val newRequest = requestWithUrlEncodedBody(
        "dateOfChange.day" -> "24",
        "dateOfChange.month" -> "2",
        "dateOfChange.year" -> "1990"
      )

      when(mockCacheMap.getEntry[BusinessDetails](BusinessDetails.key))
        .thenReturn(Some(BusinessDetails(activityStartDate = Some(ActivityStartDate(new LocalDate(1990, 2, 24))))))

      when(mockCacheMap.getEntry[Asp](Asp.key))
        .thenReturn(None)

      when(controller.dataCacheConnector.fetchAll(any())(any[HeaderCarrier]))
        .thenReturn(Future.successful(Some(mockCacheMap)))

      when(controller.dataCacheConnector.save[Asp](any(), any(), any())
        (any(), any())).thenReturn(Future.successful(emptyCache))

      val result = controller.post()(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(controllers.asp.routes.SummaryController.get().url))
    }

    "fail submission when invalid date is supplied" in new Fixture {

      val newRequest = requestWithUrlEncodedBody(
        "dateOfChange.day" -> "24",
        "dateOfChange.month" -> "2",
        "dateOfChange.year" -> "199000"
      )

      when(mockCacheMap.getEntry[BusinessDetails](BusinessDetails.key))
        .thenReturn(Some(BusinessDetails(activityStartDate = Some(ActivityStartDate(new LocalDate(1990, 2, 24))))))

      when(mockCacheMap.getEntry[Asp](Asp.key))
        .thenReturn(None)

      when(controller.dataCacheConnector.fetchAll(any())(any[HeaderCarrier]))
        .thenReturn(Future.successful(Some(mockCacheMap)))

      when(controller.dataCacheConnector.save[Asp](any(), any(), any())
        (any(), any())).thenReturn(Future.successful(emptyCache))

      val result = controller.post()(newRequest)
      status(result) must be(BAD_REQUEST)
      contentAsString(result) must include(Messages("error.expected.jodadate.format"))
    }

    "fail submission when input date is before activity start date" in new Fixture {

      val newRequest = requestWithUrlEncodedBody(
        "dateOfChange.day" -> "24",
        "dateOfChange.month" -> "2",
        "dateOfChange.year" -> "1980"
      )

      when(mockCacheMap.getEntry[BusinessDetails](BusinessDetails.key))
        .thenReturn(Some(BusinessDetails(activityStartDate = Some(ActivityStartDate(new LocalDate(1990, 2, 24))))))

      when(mockCacheMap.getEntry[Asp](Asp.key))
        .thenReturn(Some(Asp()))

      when(controller.dataCacheConnector.fetchAll(any())(any[HeaderCarrier]))
        .thenReturn(Future.successful(Some(mockCacheMap)))

      when(controller.dataCacheConnector.save[Asp](any(), any(), any())
        (any(), any())).thenReturn(Future.successful(emptyCache))

      val result = controller.post()(newRequest)
      status(result) must be(BAD_REQUEST)
      contentAsString(result) must include(Messages("error.expected.dateofchange.date.after.activitystartdate", "24-02-1990"))
    }

  }
}
