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

package controllers.aboutthebusiness

import java.util.UUID

import connectors.DataCacheConnector
import models.aboutthebusiness.{AboutTheBusiness, ContactingYou, RegisteredOfficeUK}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import utils.GenericTestHelper
import play.api.i18n.Messages
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.AuthorisedFixture

import scala.concurrent.Future

class ContactingYouControllerSpec extends GenericTestHelper with MockitoSugar with ScalaFutures with BeforeAndAfterEach {

  val userId = s"user-${UUID.randomUUID}"
  val contactingYou = Some(ContactingYou("+44 (0)123 456-7890", "test@test.com"))
  val ukAddress = RegisteredOfficeUK("305", "address line", Some("address line2"), Some("address line3"), "AA1 1AA")
  val aboutTheBusinessWithData = AboutTheBusiness(contactingYou = contactingYou, registeredOffice = Some(ukAddress))

  trait Fixture extends AuthorisedFixture {
    self => val request = addToken(authRequest)

    val controller = new ContactingYouController {
      override val dataCache = mock[DataCacheConnector]
      override val authConnector = self.authConnector
    }
  }

  val emptyCache = CacheMap("", Map.empty)

  "BusinessHasEmailController" must {

    "Get" must {

      "load the page" in new Fixture {

        val aboutTheBusinessWithAddress = AboutTheBusiness(registeredOffice = Some(ukAddress))

        when(controller.dataCache.fetch[AboutTheBusiness](any())
          (any(), any(), any())).thenReturn(Future.successful(Some(aboutTheBusinessWithAddress)))

        val result = controller.get()(request)
        status(result) must be(OK)
        contentAsString(result) must include(Messages("aboutthebusiness.contactingyou.title"))
      }

      "load the page with the pre populated data" in new Fixture {

        when(controller.dataCache.fetch[AboutTheBusiness](any())
          (any(), any(), any())).thenReturn(Future.successful(Some(aboutTheBusinessWithData)))

        val result = controller.get()(request)
        status(result) must be(OK)
        contentAsString(result) must include(Messages("aboutthebusiness.contactingyou.title"))
      }

      "load the page with no data" in new Fixture {
        when(controller.dataCache.fetch[AboutTheBusiness](any())
          (any(), any(), any())).thenReturn(Future.successful(None))
        val result = controller.get()(request)
        status(result) must be(SEE_OTHER)
        redirectLocation(result) mustBe Some(routes.ConfirmRegisteredOfficeController.get().url)
      }

    }

    "Post" must {

      "on post of valid data" in new Fixture {

        val newRequest = request.withFormUrlEncodedBody(
          "phoneNumber" -> "+44 (0)123 456-7890",
          "email" -> "test@test.com",
          "website" -> "website",
          "letterToThisAddress" -> "true"
        )

        when(controller.dataCache.fetch[AboutTheBusiness](any())
          (any(), any(), any())).thenReturn(Future.successful(Some(aboutTheBusinessWithData)))

        when(controller.dataCache.save[AboutTheBusiness](any(), any())
          (any(), any(), any())).thenReturn(Future.successful(emptyCache))

        val result = controller.post()(newRequest)
        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some(routes.SummaryController.get().url))
      }


      "on post of incomplete data" in new Fixture {

        val newRequest = request.withFormUrlEncodedBody(
          "phoneNumber" -> "+44 (0)123 456-7890"
        )

        when(controller.dataCache.fetch[AboutTheBusiness](any())
          (any(), any(), any())).thenReturn(Future.successful(Some(aboutTheBusinessWithData)))

        when(controller.dataCache.save[AboutTheBusiness](any(), any())
          (any(), any(), any())).thenReturn(Future.successful(emptyCache))

        val result = controller.post()(newRequest)
        status(result) must be(BAD_REQUEST)
      }

      "fail validation on invalid phone number" in new Fixture {

        val newRequest = request.withFormUrlEncodedBody(
          "phoneNumber" -> "+44 (0)123 456_7890",
          "email" -> "test@test.com",
          "website" -> "website",
          "letterToThisAddress" -> "true"
        )

        when(controller.dataCache.fetch[AboutTheBusiness](any())
          (any(), any(), any())).thenReturn(Future.successful(Some(aboutTheBusinessWithData)))

        when(controller.dataCache.save[AboutTheBusiness](any(), any())
          (any(), any(), any())).thenReturn(Future.successful(emptyCache))

        val result = controller.post()(newRequest)
        status(result) must be(BAD_REQUEST)

        val document: Document  = Jsoup.parse(contentAsString(result))
        document.getElementsByClass("error-notification").html() must include(Messages("err.invalid.phone.number"))
      }


      "on post of incomplete data with no response from data cache" in new Fixture {

        val newRequest = request.withFormUrlEncodedBody(
          "phoneNumber" -> "+44 (0)123 456-7890"
        )

        when(controller.dataCache.fetch[AboutTheBusiness](any())
          (any(), any(), any())).thenReturn(Future.successful(None))

        when(controller.dataCache.save[AboutTheBusiness](any(), any())
          (any(), any(), any())).thenReturn(Future.successful(emptyCache))

        val result = controller.post()(newRequest)
        status(result) must be(SEE_OTHER)
        redirectLocation(result) mustBe Some(routes.ContactingYouController.get().url)
      }

      "load the page with valid data and letterToThisAddress set to false" in new Fixture {

        val newRequest = request.withFormUrlEncodedBody(
          "phoneNumber" -> "+44 (0)123 456-7890",
          "email" -> "test@test.com",
          "website" -> "website",
          "letterToThisAddress" -> "false"
        )

        when(controller.dataCache.fetch[AboutTheBusiness](any())
          (any(), any(), any())).thenReturn(Future.successful(Some(aboutTheBusinessWithData)))

        when(controller.dataCache.save[AboutTheBusiness](any(), any())
          (any(), any(), any())).thenReturn(Future.successful(emptyCache))

        val result = controller.post()(newRequest)
        status(result) must be(SEE_OTHER)
        redirectLocation(result) mustBe Some(routes.CorrespondenceAddressController.get().url)
      }

    }
  }

}