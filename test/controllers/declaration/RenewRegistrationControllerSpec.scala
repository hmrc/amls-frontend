/*
 * Copyright 2019 HM Revenue & Customs
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

package controllers.declaration

import controllers.actions.SuccessfulAuthAction
import models.declaration.{RenewRegistration, RenewRegistrationNo, RenewRegistrationYes}
import org.jsoup.Jsoup
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import play.api.i18n.Messages
import play.api.test.Helpers._
import utils.{AmlsSpec, AuthorisedFixture, DependencyMocks}

import scala.concurrent.Future

class RenewRegistrationControllerSpec extends AmlsSpec with MockitoSugar with ScalaFutures {

  trait Fixture extends AuthorisedFixture with DependencyMocks { self =>
    val request = addToken(authRequest)

    val controller = new RenewRegistrationController(
      dataCacheConnector = mockCacheConnector,
      authAction = SuccessfulAuthAction
    )
  }

  "RenewRegistrationController" when {
    "get is called" must {
      "display the  renew registration question where not previously answered" in new Fixture {
        when{
          controller.dataCacheConnector.fetch[RenewRegistration](any(), any())(any(), any())
        } thenReturn Future.successful(None)

        val result = controller.get()(request)

        status(result) must be(OK)
        contentAsString(result) must include(Messages("declaration.renew.registration.title"))
      }

      "display the renew registration question with pre populated data" in new Fixture {
        when {
          controller.dataCacheConnector.fetch[RenewRegistration](any(), any())(any(), any())
        } thenReturn Future.successful(Some(RenewRegistrationYes))

        val result = controller.get()(request)
        status(result) must be(OK)

        val document = Jsoup.parse(contentAsString(result))
        document.select("input[value=true]").hasAttr("checked") must be(true)
      }
    }

    "post is called" when {
      "with valid data" must {
        "redirect to renewal what you need" when {
          "yes is selected" in new Fixture {
            mockCacheSave[RenewRegistration](RenewRegistrationYes, Some(RenewRegistration.key))

            val newRequest = request.withFormUrlEncodedBody("renewRegistration" -> "true")

            val result = controller.post()(newRequest)
            status(result) must be(SEE_OTHER)
            redirectLocation(result) must be(Some(controllers.renewal.routes.WhatYouNeedController.get().url))
          }
        }

        "redirect to WhoIsRegistering" when {
          "No is selected" in new Fixture {
            mockCacheSave[RenewRegistration](RenewRegistrationNo, Some(RenewRegistration.key))

            val newRequest = request.withFormUrlEncodedBody("renewRegistration" -> "false")

            val result = controller.post()(newRequest)
            status(result) must be(SEE_OTHER)
            redirectLocation(result) must be(Some(controllers.declaration.routes.WhoIsRegisteringController.get().url))
          }
        }
      }

      "with invalid data" must {
        "respond with BAD_REQUEST" in new Fixture {
          val newRequest = request.withFormUrlEncodedBody(
            "renewRegistration" -> "1234567890"
          )

          mockCacheSave[RenewRegistration](RenewRegistrationNo, Some(RenewRegistration.key))

          val result = controller.post()(newRequest)
          status(result) must be(BAD_REQUEST)

          contentAsString(result) must include(Messages("error.required.declaration.renew.registration"))
        }
      }
    }
  }
}


