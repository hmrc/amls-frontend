/*
 * Copyright 2017 HM Revenue & Customs
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

package controllers.hvd

import connectors.DataCacheConnector
import models.DateOfChange
import models.aboutthebusiness.{AboutTheBusiness, ActivityStartDate}
import models.hvd.Hvd
import org.joda.time.LocalDate
import org.mockito.Matchers._
import org.mockito.Matchers.{eq => meq, _}
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import  utils.GenericTestHelper
import play.api.i18n.Messages
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http.HeaderCarrier
import utils.AuthorisedFixture

import scala.concurrent.Future

class HvdDateOfChangeControllerSpec extends GenericTestHelper with MockitoSugar {

  trait Fixture extends AuthorisedFixture {
    self => val request = addToken(authRequest)

    val controller = new HvdDateOfChangeController {
      override val dataCacheConnector = mock[DataCacheConnector]
      override val authConnector = self.authConnector
    }
  }

  val emptyCache = CacheMap("", Map.empty)

  "HvdDateOfChangeController" must {

    "on get display date of change view" in new Fixture {
      val result = controller.get()(request)
      status(result) must be(OK)
      contentAsString(result) must include(Messages("summary.hvd"))
    }

    "submit with valid data" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "dateOfChange.day" -> "24",
        "dateOfChange.month" -> "2",
        "dateOfChange.year" -> "1990"
      )
      val hvd = Hvd(dateOfChange = Some(DateOfChange(new LocalDate(1990,2,24))))
      val mockCacheMap = mock[CacheMap]
      when(mockCacheMap.getEntry[AboutTheBusiness](AboutTheBusiness.key))
        .thenReturn(Some(AboutTheBusiness(activityStartDate = Some(ActivityStartDate(new LocalDate(1990, 2, 24))))))

      when(mockCacheMap.getEntry[Hvd](Hvd.key))
        .thenReturn(None)

      when(controller.dataCacheConnector.fetchAll(any[HeaderCarrier], any[AuthContext]))
        .thenReturn(Future.successful(Some(mockCacheMap)))

      when(controller.dataCacheConnector.save[Hvd](any(), any())
        (any(), any(), any())).thenReturn(Future.successful(emptyCache))

      val result = controller.post()(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(controllers.hvd.routes.SummaryController.get().url))

      verify(controller.dataCacheConnector).save[Hvd](any(),
        meq(hvd))(any(), any(), any())
    }

    "submit request" when {

      "dateOfChange is earlier than that in S4L" in new Fixture {

        val newRequest = request.withFormUrlEncodedBody(
          "dateOfChange.day" -> "24",
          "dateOfChange.month" -> "1",
          "dateOfChange.year" -> "1990"
        )

        val mockCacheMap = mock[CacheMap]
        val hvd = Hvd(dateOfChange = Some(DateOfChange(new LocalDate(1999,1,28))))

        when(mockCacheMap.getEntry[AboutTheBusiness](AboutTheBusiness.key))
          .thenReturn(Some(AboutTheBusiness(activityStartDate = Some(ActivityStartDate(new LocalDate(1988, 2, 24))))))

        when(mockCacheMap.getEntry[Hvd](Hvd.key))
          .thenReturn(Some(hvd))

        when(controller.dataCacheConnector.fetchAll(any[HeaderCarrier], any[AuthContext]))
          .thenReturn(Future.successful(Some(mockCacheMap)))

        when(controller.dataCacheConnector.save[Hvd](any(), any())
          (any(), any(), any())).thenReturn(Future.successful(emptyCache))

        val result = controller.post()(newRequest)
        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some(controllers.hvd.routes.SummaryController.get().url))

        verify(controller.dataCacheConnector).save[Hvd](any(),
          meq(hvd.copy(dateOfChange = Some(DateOfChange(new LocalDate(1990,1,24))))))(any(), any(), any())
      }

      "dateOfChange is later than that in S4L" in new Fixture {

        val newRequest = request.withFormUrlEncodedBody(
          "dateOfChange.day" -> "24",
          "dateOfChange.month" -> "1",
          "dateOfChange.year" -> "2001"
        )

        val mockCacheMap = mock[CacheMap]
        val hvd = Hvd(dateOfChange = Some(DateOfChange(new LocalDate(1990,1,20))))

        when(mockCacheMap.getEntry[AboutTheBusiness](AboutTheBusiness.key))
          .thenReturn(Some(AboutTheBusiness(activityStartDate = Some(ActivityStartDate(new LocalDate(1988, 2, 24))))))

        when(mockCacheMap.getEntry[Hvd](Hvd.key))
          .thenReturn(Some(hvd))

        when(controller.dataCacheConnector.fetchAll(any[HeaderCarrier], any[AuthContext]))
          .thenReturn(Future.successful(Some(mockCacheMap)))

        when(controller.dataCacheConnector.save[Hvd](any(), any())
          (any(), any(), any())).thenReturn(Future.successful(emptyCache))

        val result = controller.post()(newRequest)
        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some(controllers.hvd.routes.SummaryController.get().url))

        verify(controller.dataCacheConnector).save[Hvd](any(),
          meq(hvd))(any(), any(), any())

      }

    }

    "fail submission when invalid date is supplied" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "dateOfChange.day" -> "24",
        "dateOfChange.month" -> "2",
        "dateOfChange.year" -> "199000"
      )

      val mockCacheMap = mock[CacheMap]
      when(mockCacheMap.getEntry[AboutTheBusiness](AboutTheBusiness.key))
        .thenReturn(Some(AboutTheBusiness(activityStartDate = Some(ActivityStartDate(new LocalDate(1990, 2, 24))))))

      when(mockCacheMap.getEntry[Hvd](Hvd.key))
        .thenReturn(None)

      when(controller.dataCacheConnector.fetchAll(any[HeaderCarrier], any[AuthContext]))
        .thenReturn(Future.successful(Some(mockCacheMap)))

      when(controller.dataCacheConnector.save[Hvd](any(), any())
        (any(), any(), any())).thenReturn(Future.successful(emptyCache))

      val result = controller.post()(newRequest)
      status(result) must be(BAD_REQUEST)
      contentAsString(result) must include(Messages("error.expected.jodadate.format"))
    }

    "fail submission when input date is before activity start date" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "dateOfChange.day" -> "24",
        "dateOfChange.month" -> "2",
        "dateOfChange.year" -> "1980"
      )

      val mockCacheMap = mock[CacheMap]
      when(mockCacheMap.getEntry[AboutTheBusiness](AboutTheBusiness.key))
        .thenReturn(Some(AboutTheBusiness(activityStartDate = Some(ActivityStartDate(new LocalDate(1990, 2, 24))))))

      when(mockCacheMap.getEntry[Hvd](Hvd.key))
        .thenReturn(Some(Hvd()))

      when(controller.dataCacheConnector.fetchAll(any[HeaderCarrier], any[AuthContext]))
        .thenReturn(Future.successful(Some(mockCacheMap)))

      when(controller.dataCacheConnector.save[Hvd](any(), any())
        (any(), any(), any())).thenReturn(Future.successful(emptyCache))

      val result = controller.post()(newRequest)
      status(result) must be(BAD_REQUEST)
      contentAsString(result) must include(Messages("error.expected.dateofchange.date.after.activitystartdate", "24-02-1990"))
    }

  }
}
