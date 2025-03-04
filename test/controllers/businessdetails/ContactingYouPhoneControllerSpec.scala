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
import forms.businessdetails.BusinessTelephoneFormProvider
import models.businessdetails.{BusinessDetails, ContactingYou}
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.cache.Cache
import utils.AmlsSpec
import views.html.businessdetails.BusinessTelephoneView

import java.util.UUID
import scala.concurrent.Future

class ContactingYouPhoneControllerSpec extends AmlsSpec with MockitoSugar with ScalaFutures with BeforeAndAfterEach {

  val userId                  = s"user-${UUID.randomUUID}"
  val contactingYou           = Some(ContactingYou(Some("+44 (0)123 456-7890"), Some("test@test.com")))
  val businessDetailsWithData = BusinessDetails(contactingYou = contactingYou)

  trait Fixture {
    self =>
    val request    = addToken(authRequest)
    lazy val view  = app.injector.instanceOf[BusinessTelephoneView]
    val controller = new ContactingYouPhoneController(
      dataCache = mock[DataCacheConnector],
      authAction = SuccessfulAuthAction,
      ds = commonDependencies,
      cc = mockMcc,
      formProvider = app.injector.instanceOf[BusinessTelephoneFormProvider],
      view = view
    )
  }

  val emptyCache = Cache.empty

  "ContactingYouPhoneController" must {

    "Get" must {

      "load the page" in new Fixture {

        when(controller.dataCache.fetch[BusinessDetails](any(), any())(any()))
          .thenReturn(Future.successful(Some(businessDetailsWithData)))

        val result = controller.get()(request)
        status(result)          must be(OK)
        contentAsString(result) must include(messages("businessdetails.contactingyou.phone.title"))
      }

      "load the page with the pre populated data" in new Fixture {

        when(controller.dataCache.fetch[BusinessDetails](any(), any())(any()))
          .thenReturn(Future.successful(Some(businessDetailsWithData)))

        val result = controller.get()(request)
        status(result)          must be(OK)
        contentAsString(result) must include(messages("businessdetails.contactingyou.phone.title"))
      }

      "load the page when business details are unavailable" in new Fixture {

        when(controller.dataCache.fetch[BusinessDetails](any(), any())(any())).thenReturn(Future.successful(None))

        val result = controller.get()(request)
        status(result)          must be(OK)
        contentAsString(result) must include(messages("businessdetails.contactingyou.phone.title"))
      }
    }

    "Post" must {

      "on post of valid data" in new Fixture {

        val newRequest = FakeRequest(POST, routes.ContactingYouPhoneController.post(false).url)
          .withFormUrlEncodedBody("phoneNumber" -> "+44 (0)123 456-7890")

        when(controller.dataCache.fetch[BusinessDetails](any(), any())(any()))
          .thenReturn(Future.successful(Some(businessDetailsWithData)))

        when(controller.dataCache.save[BusinessDetails](any(), any(), any())(any()))
          .thenReturn(Future.successful(emptyCache))

        val result = controller.post()(newRequest)
        status(result)           must be(SEE_OTHER)
        redirectLocation(result) must be(Some(routes.LettersAddressController.get().url))
      }

      "on post of valid data with edit" in new Fixture {

        val newRequest = FakeRequest(POST, routes.ContactingYouPhoneController.post(true).url)
          .withFormUrlEncodedBody("phoneNumber" -> "+44 (0)123 456-7890")

        when(controller.dataCache.fetch[BusinessDetails](any(), any())(any()))
          .thenReturn(Future.successful(Some(businessDetailsWithData)))

        when(controller.dataCache.save[BusinessDetails](any(), any(), any())(any()))
          .thenReturn(Future.successful(emptyCache))

        val result = controller.post(true)(newRequest)
        status(result)           must be(SEE_OTHER)
        redirectLocation(result) must be(Some(routes.SummaryController.get.url))
      }

      "on post of valid data with empty contact details" in new Fixture {

        val newRequest = FakeRequest(POST, routes.ContactingYouPhoneController.post(false).url)
          .withFormUrlEncodedBody("phoneNumber" -> "+44 (0)123 456-7890")

        when(controller.dataCache.fetch[BusinessDetails](any(), any())(any())).thenReturn(Future.successful(None))

        when(controller.dataCache.save[BusinessDetails](any(), any(), any())(any()))
          .thenReturn(Future.successful(emptyCache))

        val result = controller.post()(newRequest)
        status(result)           must be(SEE_OTHER)
        redirectLocation(result) must be(Some(routes.LettersAddressController.get().url))
      }

      "on post of incomplete data" in new Fixture {

        val newRequest = FakeRequest(POST, routes.ContactingYouPhoneController.post(false).url)
          .withFormUrlEncodedBody("phoneNumber" -> "")

        when(controller.dataCache.fetch[BusinessDetails](any(), any())(any()))
          .thenReturn(Future.successful(Some(businessDetailsWithData)))

        when(controller.dataCache.save[BusinessDetails](any(), any(), any())(any()))
          .thenReturn(Future.successful(emptyCache))

        val result = controller.post()(newRequest)
        status(result) must be(BAD_REQUEST)
      }
    }
  }
}
