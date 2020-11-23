/*
 * Copyright 2020 HM Revenue & Customs
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

package controllers.responsiblepeople.address

import connectors.DataCacheConnector
import controllers.actions.SuccessfulAuthAction
import controllers.responsiblepeople.address
import models.responsiblepeople._
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import play.api.i18n.Messages
import play.api.test.Helpers._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.AmlsSpec
import views.html.responsiblepeople.address.moved_address

import scala.concurrent.Future

class MovedAddressControllerSpec extends AmlsSpec with MockitoSugar {

  trait Fixture {
    self =>
    val request = addToken(authRequest)
    val dataCache: DataCacheConnector = mock[DataCacheConnector]
    lazy val view = app.injector.instanceOf[moved_address]
    val controller = new MovedAddressController(messagesApi, self.dataCache, SuccessfulAuthAction, ds = commonDependencies, cc = mockMcc,
      moved_address = view)

  }

  "MovedAddress" when {


    val personName = PersonName("firstName", Some("middleName"), "lastName")
    val UKAddress = PersonAddressUK("line1", "line2", Some("line3"), Some("line4"), "AA1 1AA")
    val currentAddress = ResponsiblePersonCurrentAddress(UKAddress, None)
    val history = ResponsiblePersonAddressHistory(currentAddress = Some(currentAddress))

    val rp = ResponsiblePerson (
      personName = Some(personName),
      addressHistory = Some(history)
    )

    val responsiblePerson = Seq(rp)


    "Get is called" must {

      "Load Moved address page successfully when address is supplied" in new Fixture {
        val mockCacheMap = mock[CacheMap]

        when(mockCacheMap.getEntry[Seq[ResponsiblePerson]](any())(any()))
          .thenReturn(Some(responsiblePerson))

        when(controller.dataCacheConnector.fetchAll(any())(any[HeaderCarrier]))
          .thenReturn(Future.successful(Some(mockCacheMap)))

        val result = controller.get(1)(request)
        status(result) must be(OK)
        contentAsString(result) must include(Messages("responsiblepeople.movedaddress.heading", personName.titleName))
      }

      "Load registration progress page successfully when no ResponsiblePeople model is supplied" in new Fixture {
        val mockCacheMap = mock[CacheMap]

        when(mockCacheMap.getEntry[Seq[ResponsiblePerson]](any())(any()))
          .thenReturn(None)

        when(controller.dataCacheConnector.fetchAll(any())(any[HeaderCarrier]))
          .thenReturn(Future.successful(Some(mockCacheMap)))

        val result = controller.get(1)(request)
        redirectLocation(result) must be(Some(controllers.routes.RegistrationProgressController.get.url))
        status(result) must be(SEE_OTHER)

      }

      "Load current address page successfully when no address is supplied" in new Fixture {
        val personName = PersonName("firstName", Some("middleName"), "lastName")
        val history = ResponsiblePersonAddressHistory(currentAddress = None)

        val rp = ResponsiblePerson (
          personName = Some(personName),
          addressHistory = Some(history)
        )

        val responsiblePerson = Seq(rp)
        val mockCacheMap = mock[CacheMap]

        when(mockCacheMap.getEntry[Seq[ResponsiblePerson]](any())(any()))
          .thenReturn(Some(responsiblePerson))

        when(controller.dataCacheConnector.fetchAll(any())(any[HeaderCarrier]))
          .thenReturn(Future.successful(Some(mockCacheMap)))

        val result = controller.get(1)(request)
        redirectLocation(result) must be(Some(address.routes.CurrentAddressController.get(1,true).url))
        status(result) must be(SEE_OTHER)

      }

    }

    "Post is called" must {

      val UKAddress = PersonAddressUK("line1", "line2", Some("line3"), Some("line4"), "AA1 1AA")
      val currentAddress = ResponsiblePersonCurrentAddress(UKAddress, None)
      val history = ResponsiblePersonAddressHistory(currentAddress = Some(currentAddress))

      "successfully redirect to next page" when {

        "option is 'Yes' is selected confirming the mentioned has moved from the shown address" in new Fixture {

          val newRequest = requestWithUrlEncodedBody(
            "movedAddress" -> "true"
          )

          val mockCacheMap = mock[CacheMap]

          when(mockCacheMap.getEntry[Seq[ResponsiblePerson]](any())(any()))
            .thenReturn(Some(Seq(ResponsiblePerson())))


          when(controller.dataCacheConnector.fetchAll(any())(any[HeaderCarrier]))
            .thenReturn(Future.successful(Some(mockCacheMap)))

          val result = controller.post(1)(newRequest)
          status(result) must be (SEE_OTHER)
          redirectLocation(result) must be(Some(routes.NewHomeAddressDateOfChangeController.get(1).url))
        }

        "option is 'No' is selected confirming the mentioned has not moved from the shown address" in new Fixture {
          val newRequest = requestWithUrlEncodedBody(
            "movedAddress" -> "false"
          )

          val result = controller.post(1)(newRequest)
          status(result) must be (SEE_OTHER)
          redirectLocation(result) must be(Some(address.routes.CurrentAddressController.get(1, true).url))
        }
      }

      "redirect to current address controller when no address is supplied" in new Fixture {

        val newRequest = requestWithUrlEncodedBody(
        )

        val personName = PersonName("firstName", Some("middleName"), "lastName")
        val history = ResponsiblePersonAddressHistory(currentAddress = None)

        val rp = ResponsiblePerson (
          personName = Some(personName),
          addressHistory = Some(history)
        )

        val responsiblePerson = Seq(rp)
        val mockCacheMap = mock[CacheMap]

        when(mockCacheMap.getEntry[Seq[ResponsiblePerson]](any())(any()))
          .thenReturn(Some(responsiblePerson))

        when(controller.dataCacheConnector.fetchAll(any())(any[HeaderCarrier]))
          .thenReturn(Future.successful(Some(mockCacheMap)))

        val result = controller.post(1)(newRequest)
        redirectLocation(result) must be(Some(address.routes.CurrentAddressController.get(1,true).url))
        status(result) must be(SEE_OTHER)
      }

      "redirect to registration progress when no responsible person model is supplied" in new Fixture {

        val newRequest = requestWithUrlEncodedBody(
        )

        val mockCacheMap = mock[CacheMap]

        when(mockCacheMap.getEntry[Seq[ResponsiblePerson]](any())(any()))
          .thenReturn(None)

        when(controller.dataCacheConnector.fetchAll(any())(any[HeaderCarrier]))
          .thenReturn(Future.successful(Some(mockCacheMap)))

        val result = controller.post(1)(newRequest)
        redirectLocation(result) must be(Some(controllers.routes.RegistrationProgressController.get.url))
        status(result) must be(SEE_OTHER)
      }

      "throw error message on not selecting the option" in new Fixture {

        val newRequest = requestWithUrlEncodedBody(
        )

        val mockCacheMap = mock[CacheMap]

        when(mockCacheMap.getEntry[Seq[ResponsiblePerson]](any())(any()))
          .thenReturn(Some(responsiblePerson))

        when(controller.dataCacheConnector.fetchAll(any())(any[HeaderCarrier]))
          .thenReturn(Future.successful(Some(mockCacheMap)))

        val result = controller.post(1)(newRequest)
        status(result) must be(BAD_REQUEST)
        contentAsString(result) must include(Messages("error.required.rp.moved.address", personName.titleName))
      }

    }
  }
}
