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

package controllers.aboutthebusiness

import connectors.DataCacheConnector
import models.{Country, DateOfChange}
import models.aboutthebusiness._
import org.joda.time.LocalDate
import org.mockito.ArgumentCaptor
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import  utils.GenericTestHelper
import play.api.test.FakeApplication
import play.api.test.Helpers._
import services.StatusService
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.AuthorisedFixture

import scala.concurrent.Future

class RegisteredOfficeDateOfChangeControllerSpec extends GenericTestHelper with  MockitoSugar{

  override lazy val app = FakeApplication(additionalConfiguration = Map("microservice.services.feature-toggle.release7" -> true) )

  trait Fixture extends AuthorisedFixture {
    self => val request = addToken(authRequest)

    val controller = new RegisteredOfficeDateOfChangeController () {
      override val dataCacheConnector = mock[DataCacheConnector]
      override val authConnector = self.authConnector
      override val statusService = mock[StatusService]
    }
  }

  val emptyCache = CacheMap("", Map.empty)

  "return view for Date of Change" in new Fixture {
    val result = controller.get()(request)
    status(result) must be(OK)
  }

  "handle the date of change form post" when {
    "given valid data for a UK address" in new Fixture {

      val postRequest = request.withFormUrlEncodedBody(
        "dateOfChange.year" -> "2010",
        "dateOfChange.month" -> "10",
        "dateOfChange.day" -> "01"
      )

      val office = RegisteredOfficeUK("305", "address line", Some("address line2"), Some("address line3"), "AA1 1AA")
      val updatedOffice = office.copy(dateOfChange = Some(DateOfChange(new LocalDate(2010, 10, 1))))

      val business = AboutTheBusiness(registeredOffice = Some(office))

      when(controller.dataCacheConnector.fetch[AboutTheBusiness](eqTo(AboutTheBusiness.key))(any(), any(), any())).
        thenReturn(Future.successful(Some(business)))

      when(controller.dataCacheConnector.save[AboutTheBusiness](eqTo(AboutTheBusiness.key), any[AboutTheBusiness])(any(), any(), any())).
        thenReturn(Future.successful(mock[CacheMap]))

      val result = controller.post()(postRequest)

      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(routes.SummaryController.get().url))

      val captor = ArgumentCaptor.forClass(classOf[AboutTheBusiness])
      verify(controller.dataCacheConnector).save[AboutTheBusiness](eqTo(AboutTheBusiness.key), captor.capture())(any(), any(), any())

      captor.getValue.registeredOffice match {
        case Some(savedOffice: RegisteredOfficeUK) => savedOffice must be(updatedOffice)
      }

    }

    "given valid data for a Non-UK address" in new Fixture {

      val postRequest = request.withFormUrlEncodedBody(
        "dateOfChange.year" -> "2005",
        "dateOfChange.month" -> "04",
        "dateOfChange.day" -> "26"
      )

      val office = RegisteredOfficeNonUK("305", "address line", Some("address line2"), Some("address line3"), Country("Finland", "FIN"))
      val updatedOffice = office.copy(dateOfChange = Some(DateOfChange(new LocalDate(2005, 4, 26))))

      val business = AboutTheBusiness(registeredOffice = Some(office))

      when(controller.dataCacheConnector.fetch[AboutTheBusiness](eqTo(AboutTheBusiness.key))(any(), any(), any())).
        thenReturn(Future.successful(Some(business)))

      when(controller.dataCacheConnector.save[AboutTheBusiness](eqTo(AboutTheBusiness.key), any[AboutTheBusiness])(any(), any(), any())).
        thenReturn(Future.successful(mock[CacheMap]))

      val result = controller.post()(postRequest)

      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(routes.SummaryController.get().url))

      val captor = ArgumentCaptor.forClass(classOf[AboutTheBusiness])
      verify(controller.dataCacheConnector).save[AboutTheBusiness](eqTo(AboutTheBusiness.key), captor.capture())(any(), any(), any())

      captor.getValue.registeredOffice match {
        case Some(savedOffice: RegisteredOfficeNonUK) => savedOffice must be(updatedOffice)
      }
    }
  }

  "show the data of change form once again" when {
    "posted with invalid data" in new Fixture {

      when(controller.dataCacheConnector.fetch[AboutTheBusiness](eqTo(AboutTheBusiness.key))(any(), any(), any())).
        thenReturn(Future.successful(Some(AboutTheBusiness())))

      val postRequest = request.withFormUrlEncodedBody()

      val result = controller.post(postRequest)

      status(result) must be(BAD_REQUEST)
      verify(controller.dataCacheConnector, never()).save[AboutTheBusiness](any(), any())(any(), any(), any())

    }
    "dateOfChange is earlier than Business Activities Start Date" in new Fixture {
      val postRequest = request.withFormUrlEncodedBody(
        "dateOfChange.year" -> "2010",
        "dateOfChange.month" -> "10",
        "dateOfChange.day" -> "01"
      )

      val office = RegisteredOfficeUK("305", "address line", Some("address line2"), Some("address line3"), "AA1 1AA")
      val updatedOffice = office.copy(dateOfChange = Some(DateOfChange(new LocalDate(2010, 10, 1))))

      val business = AboutTheBusiness(
        activityStartDate = Some(ActivityStartDate(new LocalDate(2015, 10, 1))),
        registeredOffice = Some(office)
      )

      when(controller.dataCacheConnector.fetch[AboutTheBusiness](eqTo(AboutTheBusiness.key))(any(), any(), any())).
        thenReturn(Future.successful(Some(business)))

      when(controller.dataCacheConnector.save[AboutTheBusiness](eqTo(AboutTheBusiness.key), any[AboutTheBusiness])(any(), any(), any())).
        thenReturn(Future.successful(mock[CacheMap]))

      val result = controller.post()(postRequest)
      status(result) must be(BAD_REQUEST)
    }
  }
}
