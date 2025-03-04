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

package controllers.hvd

import connectors.DataCacheConnector
import controllers.actions.SuccessfulAuthAction
import forms.DateOfChangeFormProvider
import models.DateOfChange
import models.businessdetails.{ActivityStartDate, BusinessDetails}
import models.hvd.Hvd
import org.mockito.ArgumentMatchers.{eq => meq, _}
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Injecting}
import services.cache.Cache
import utils.{AmlsSpec, DateHelper, DateOfChangeHelper}
import views.html.DateOfChangeView

import java.time.LocalDate
import scala.concurrent.Future

class HvdDateOfChangeControllerSpec extends AmlsSpec with MockitoSugar with Injecting {

  trait Fixture {
    self =>
    val request = addToken(authRequest)

    lazy val view          = inject[DateOfChangeView]
    val dataCacheConnector = mock[DataCacheConnector]
    val controller         = new HvdDateOfChangeController(
      dataCacheConnector,
      authAction = SuccessfulAuthAction,
      ds = commonDependencies,
      cc = mockMcc,
      formProvider = inject[DateOfChangeFormProvider],
      view = view
    )

  }

  val emptyCache = Cache.empty

  "HvdDateOfChangeController" must {

    "on get display date of change view" in new Fixture with DateOfChangeHelper {
      val result = controller.get(DateOfChangeRedirect.checkYourAnswers)(request)
      status(result)          must be(OK)
      contentAsString(result) must include(messages("summary.hvd"))
    }

    "submit with valid data" in new Fixture with DateOfChangeHelper {

      val newRequest   =
        FakeRequest(POST, routes.HvdDateOfChangeController.post(DateOfChangeRedirect.checkYourAnswers).url)
          .withFormUrlEncodedBody(
            "dateOfChange.day"   -> "24",
            "dateOfChange.month" -> "2",
            "dateOfChange.year"  -> "1990"
          )
      val hvd          = Hvd(dateOfChange = Some(DateOfChange(LocalDate.of(1990, 2, 24))))
      val mockCacheMap = mock[Cache]
      when(mockCacheMap.getEntry[BusinessDetails](BusinessDetails.key))
        .thenReturn(Some(BusinessDetails(activityStartDate = Some(ActivityStartDate(LocalDate.of(1990, 2, 24))))))

      when(mockCacheMap.getEntry[Hvd](Hvd.key))
        .thenReturn(Some(Hvd()))

      when(controller.dataCacheConnector.fetchAll(any()))
        .thenReturn(Future.successful(Some(mockCacheMap)))

      when(controller.dataCacheConnector.save[Hvd](any(), any(), any())(any()))
        .thenReturn(Future.successful(emptyCache))

      val result = controller.post(DateOfChangeRedirect.checkYourAnswers)(newRequest)
      status(result)           must be(SEE_OTHER)
      redirectLocation(result) must be(Some(controllers.hvd.routes.SummaryController.get.url))

      verify(controller.dataCacheConnector).save[Hvd](any(), any(), meq(hvd))(any())
    }

    "submit request" when {

      "dateOfChange is earlier than that in S4L" in new Fixture with DateOfChangeHelper {

        val newRequest =
          FakeRequest(POST, routes.HvdDateOfChangeController.post(DateOfChangeRedirect.checkYourAnswers).url)
            .withFormUrlEncodedBody(
              "dateOfChange.day"   -> "24",
              "dateOfChange.month" -> "1",
              "dateOfChange.year"  -> "1990"
            )

        val mockCacheMap = mock[Cache]
        val hvd          = Hvd(dateOfChange = Some(DateOfChange(LocalDate.of(1999, 1, 28))))

        when(mockCacheMap.getEntry[BusinessDetails](BusinessDetails.key))
          .thenReturn(Some(BusinessDetails(activityStartDate = Some(ActivityStartDate(LocalDate.of(1988, 2, 24))))))

        when(mockCacheMap.getEntry[Hvd](Hvd.key))
          .thenReturn(Some(hvd))

        when(controller.dataCacheConnector.fetchAll(any()))
          .thenReturn(Future.successful(Some(mockCacheMap)))

        when(controller.dataCacheConnector.save[Hvd](any(), any(), any())(any()))
          .thenReturn(Future.successful(emptyCache))

        val result = controller.post(DateOfChangeRedirect.checkYourAnswers)(newRequest)
        status(result)           must be(SEE_OTHER)
        redirectLocation(result) must be(Some(controllers.hvd.routes.SummaryController.get.url))

        verify(controller.dataCacheConnector)
          .save[Hvd](any(), any(), meq(hvd.copy(dateOfChange = Some(DateOfChange(LocalDate.of(1990, 1, 24))))))(any())
      }

      "dateOfChange is later than that in S4L" in new Fixture with DateOfChangeHelper {

        val newRequest =
          FakeRequest(POST, routes.HvdDateOfChangeController.post(DateOfChangeRedirect.checkYourAnswers).url)
            .withFormUrlEncodedBody(
              "dateOfChange.day"   -> "24",
              "dateOfChange.month" -> "1",
              "dateOfChange.year"  -> "2001"
            )

        val mockCacheMap = mock[Cache]
        val hvd          = Hvd(dateOfChange = Some(DateOfChange(LocalDate.of(1990, 1, 20))))

        when(mockCacheMap.getEntry[BusinessDetails](BusinessDetails.key))
          .thenReturn(Some(BusinessDetails(activityStartDate = Some(ActivityStartDate(LocalDate.of(1988, 2, 24))))))

        when(mockCacheMap.getEntry[Hvd](Hvd.key))
          .thenReturn(Some(hvd))

        when(controller.dataCacheConnector.fetchAll(any()))
          .thenReturn(Future.successful(Some(mockCacheMap)))

        when(controller.dataCacheConnector.save[Hvd](any(), any(), any())(any()))
          .thenReturn(Future.successful(emptyCache))

        val result = controller.post(DateOfChangeRedirect.checkYourAnswers)(newRequest)
        status(result)           must be(SEE_OTHER)
        redirectLocation(result) must be(Some(controllers.hvd.routes.SummaryController.get.url))

        verify(controller.dataCacheConnector).save[Hvd](any(), any(), meq(hvd))(any())

      }

    }

    "fail submission" when {

      "invalid date is supplied" in new Fixture with DateOfChangeHelper {

        val newRequest =
          FakeRequest(POST, routes.HvdDateOfChangeController.post(DateOfChangeRedirect.checkYourAnswers).url)
            .withFormUrlEncodedBody(
              "dateOfChange.day"   -> "24",
              "dateOfChange.month" -> "2",
              "dateOfChange.year"  -> "199000"
            )

        val mockCacheMap = mock[Cache]
        when(mockCacheMap.getEntry[BusinessDetails](BusinessDetails.key))
          .thenReturn(Some(BusinessDetails(activityStartDate = Some(ActivityStartDate(LocalDate.of(1990, 2, 24))))))

        when(mockCacheMap.getEntry[Hvd](Hvd.key))
          .thenReturn(None)

        when(controller.dataCacheConnector.fetchAll(any()))
          .thenReturn(Future.successful(Some(mockCacheMap)))

        when(controller.dataCacheConnector.save[Hvd](any(), any(), any())(any()))
          .thenReturn(Future.successful(emptyCache))

        val result = controller.post(DateOfChangeRedirect.checkYourAnswers)(newRequest)
        status(result) must be(BAD_REQUEST)
      }

      "input date is before activity start date" in new Fixture with DateOfChangeHelper {

        val newRequest =
          FakeRequest(POST, routes.HvdDateOfChangeController.post(DateOfChangeRedirect.checkYourAnswers).url)
            .withFormUrlEncodedBody(
              "dateOfChange.day"   -> "24",
              "dateOfChange.month" -> "2",
              "dateOfChange.year"  -> "1980"
            )

        val date = LocalDate.of(1990, 2, 24)

        val mockCacheMap = mock[Cache]
        when(mockCacheMap.getEntry[BusinessDetails](BusinessDetails.key))
          .thenReturn(Some(BusinessDetails(activityStartDate = Some(ActivityStartDate(date)))))

        when(mockCacheMap.getEntry[Hvd](Hvd.key))
          .thenReturn(Some(Hvd()))

        when(controller.dataCacheConnector.fetchAll(any()))
          .thenReturn(Future.successful(Some(mockCacheMap)))

        when(controller.dataCacheConnector.save[Hvd](any(), any(), any())(any()))
          .thenReturn(Future.successful(emptyCache))

        val result = controller.post(DateOfChangeRedirect.checkYourAnswers)(newRequest)
        status(result)          must be(BAD_REQUEST)
        contentAsString(result) must include(
          messages("error.expected.dateofchange.date.after.activitystartdate", DateHelper.formatDate(date))
        )
      }
    }
  }
}
