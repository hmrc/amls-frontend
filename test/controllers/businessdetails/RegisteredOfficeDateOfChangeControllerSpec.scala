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
import forms.DateOfChangeFormProvider
import models.businessdetails._
import models.{Country, DateOfChange}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Injecting}
import services.StatusService
import services.cache.Cache
import utils.AmlsSpec
import views.html.DateOfChangeView

import java.time.LocalDate
import scala.concurrent.Future

class RegisteredOfficeDateOfChangeControllerSpec extends AmlsSpec with MockitoSugar with Injecting {

  trait Fixture {
    self =>
    val request    = addToken(authRequest)
    lazy val view  = inject[DateOfChangeView]
    val controller = new RegisteredOfficeDateOfChangeController(
      dataCacheConnector = mock[DataCacheConnector],
      statusService = mock[StatusService],
      authAction = SuccessfulAuthAction,
      ds = commonDependencies,
      cc = mockMcc,
      formProvider = inject[DateOfChangeFormProvider],
      view = view
    )
  }

  val emptyCache = Cache.empty

  "return view for Date of Change" in new Fixture {
    val result = controller.get()(request)
    status(result) must be(OK)
  }

  "handle the date of change form post" when {
    "given valid data for a UK address" in new Fixture {

      val postRequest = FakeRequest(POST, routes.RegisteredOfficeDateOfChangeController.post().url)
        .withFormUrlEncodedBody(
          "dateOfChange.year"  -> "2010",
          "dateOfChange.month" -> "10",
          "dateOfChange.day"   -> "01"
        )

      val date          = LocalDate.of(2010, 10, 1)
      val office        =
        RegisteredOfficeUK("305", Some("address line"), Some("address line2"), Some("address line3"), "AA1 1AA")
      val updatedOffice = office.copy(dateOfChange = Some(DateOfChange(date)))

      val business = BusinessDetails(registeredOffice = Some(office), activityStartDate = Some(ActivityStartDate(date)))

      when(controller.dataCacheConnector.fetch[BusinessDetails](any(), eqTo(BusinessDetails.key))(any()))
        .thenReturn(Future.successful(Some(business)))

      when(
        controller.dataCacheConnector.save[BusinessDetails](any(), eqTo(BusinessDetails.key), any[BusinessDetails])(
          any()
        )
      ).thenReturn(Future.successful(mock[Cache]))

      val result = controller.post()(postRequest)

      status(result)           must be(SEE_OTHER)
      redirectLocation(result) must be(Some(routes.SummaryController.get.url))

      val captor = ArgumentCaptor.forClass(classOf[BusinessDetails])
      verify(controller.dataCacheConnector)
        .save[BusinessDetails](any(), eqTo(BusinessDetails.key), captor.capture())(any())

      captor.getValue.registeredOffice match {
        case Some(savedOffice: RegisteredOfficeUK) => savedOffice must be(updatedOffice)
      }

    }

    "given valid data for a Non-UK address" in new Fixture {

      val postRequest = FakeRequest(POST, routes.RegisteredOfficeDateOfChangeController.post().url)
        .withFormUrlEncodedBody(
          "dateOfChange.year"  -> "2005",
          "dateOfChange.month" -> "04",
          "dateOfChange.day"   -> "26"
        )

      val date = LocalDate.of(2005, 4, 26)

      val office = RegisteredOfficeNonUK(
        "305",
        Some("address line"),
        Some("address line2"),
        Some("address line3"),
        Country("Finland", "FIN")
      )

      val updatedOffice = office.copy(dateOfChange = Some(DateOfChange(date)))

      val business = BusinessDetails(registeredOffice = Some(office), activityStartDate = Some(ActivityStartDate(date)))

      when(controller.dataCacheConnector.fetch[BusinessDetails](any(), eqTo(BusinessDetails.key))(any()))
        .thenReturn(Future.successful(Some(business)))

      when(
        controller.dataCacheConnector.save[BusinessDetails](any(), eqTo(BusinessDetails.key), any[BusinessDetails])(
          any()
        )
      ).thenReturn(Future.successful(mock[Cache]))

      val result = controller.post()(postRequest)

      status(result)           must be(SEE_OTHER)
      redirectLocation(result) must be(Some(routes.SummaryController.get.url))

      val captor = ArgumentCaptor.forClass(classOf[BusinessDetails])
      verify(controller.dataCacheConnector)
        .save[BusinessDetails](any(), eqTo(BusinessDetails.key), captor.capture())(any())

      captor.getValue.registeredOffice match {
        case Some(savedOffice: RegisteredOfficeNonUK) => savedOffice must be(updatedOffice)
      }
    }
  }

  "show the data of change form once again" when {
    "posted with invalid data" in new Fixture {

      when(controller.dataCacheConnector.fetch[BusinessDetails](any(), eqTo(BusinessDetails.key))(any()))
        .thenReturn(Future.successful(Some(BusinessDetails())))

      val postRequest = FakeRequest(POST, routes.RegisteredOfficeDateOfChangeController.post().url)
        .withFormUrlEncodedBody("invalid" -> "data")

      val result = controller.post(postRequest)

      status(result) must be(BAD_REQUEST)
      verify(controller.dataCacheConnector, never()).save[BusinessDetails](any(), any(), any())(any())

    }
    "dateOfChange is earlier than Business Activities Start Date" in new Fixture {
      val postRequest = FakeRequest(POST, routes.RegisteredOfficeDateOfChangeController.post().url)
        .withFormUrlEncodedBody(
          "dateOfChange.year"  -> "2010",
          "dateOfChange.month" -> "10",
          "dateOfChange.day"   -> "01"
        )

      val office =
        RegisteredOfficeUK("305", Some("address line"), Some("address line2"), Some("address line3"), "AA1 1AA")

      val business = BusinessDetails(
        activityStartDate = Some(ActivityStartDate(LocalDate.of(2015, 10, 1))),
        registeredOffice = Some(office)
      )

      when(controller.dataCacheConnector.fetch[BusinessDetails](any(), eqTo(BusinessDetails.key))(any()))
        .thenReturn(Future.successful(Some(business)))

      when(
        controller.dataCacheConnector.save[BusinessDetails](any(), eqTo(BusinessDetails.key), any[BusinessDetails])(
          any()
        )
      ).thenReturn(Future.successful(mock[Cache]))

      val result = controller.post()(postRequest)
      status(result) must be(BAD_REQUEST)
    }
  }
}
