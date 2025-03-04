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

import controllers.actions.SuccessfulAuthAction
import forms.businessdetails.BusinessEmailAddressFormProvider
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.businessdetails.BusinessEmailAddressService
import services.cache.Cache
import utils.AmlsSpec
import views.html.businessdetails.BusinessEmailAddressView

import java.util.UUID
import scala.concurrent.Future

class BusinessEmailAddressControllerSpec extends AmlsSpec with MockitoSugar with ScalaFutures with BeforeAndAfterEach {

  val userId = s"user-${UUID.randomUUID}"
  val email  = "test@test.com"

  val mockService = mock[BusinessEmailAddressService]

  trait Fixture {
    self =>
    val request    = addToken(authRequest)
    lazy val view  = app.injector.instanceOf[BusinessEmailAddressView]
    val controller = new BusinessEmailAddressController(
      authAction = SuccessfulAuthAction,
      ds = commonDependencies,
      cc = mockMcc,
      service = mockService,
      formProvider = app.injector.instanceOf[BusinessEmailAddressFormProvider],
      view = view
    )
  }

  val emptyCache = Cache.empty

  "BusinessHasEmailController" must {

    "Get" must {

      "load the page with the pre populated data" in new Fixture {

        when(mockService.getEmailAddress(any())).thenReturn(Future.successful(Some(email)))

        val result = controller.get()(request)
        status(result)          must be(OK)
        contentAsString(result) must include(messages("businessdetails.contactingyou.email.title"))
      }

      "load the page with no data" in new Fixture {

        when(mockService.getEmailAddress(any())).thenReturn(Future.successful(None))

        val result = controller.get()(request)
        status(result)          must be(OK)
        contentAsString(result) must include(messages("businessdetails.contactingyou.email.title"))
      }

    }

    "Post" must {

      "on post of valid data" in new Fixture {
        val newRequest = FakeRequest(POST, routes.BusinessEmailAddressController.post().url).withFormUrlEncodedBody(
          "email" -> "test@test.com"
        )

        when(mockService.updateEmailAddress(any(), any())).thenReturn(Future.successful(Some(emptyCache)))

        val result = controller.post()(newRequest)
        status(result)           must be(SEE_OTHER)
        redirectLocation(result) must be(Some(routes.ContactingYouPhoneController.get().url))
      }

      "on post of incomplete data" in new Fixture {

        val newRequest = FakeRequest(POST, routes.BusinessEmailAddressController.post().url).withFormUrlEncodedBody(
          "email" -> ""
        )

        val result = controller.post()(newRequest)
        status(result) must be(BAD_REQUEST)
      }

    }
  }
}
