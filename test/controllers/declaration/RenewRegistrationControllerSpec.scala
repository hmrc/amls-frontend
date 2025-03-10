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

package controllers.declaration

import controllers.actions.SuccessfulAuthAction
import forms.declaration.RenewRegistrationFormProvider
import models.declaration.{RenewRegistration, RenewRegistrationNo, RenewRegistrationYes}
import models.status.ReadyForRenewal
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import play.api.i18n.Messages
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Injecting}
import services.{ProgressService, RenewalService, StatusService}
import utils.{AmlsSpec, AuthorisedFixture, DependencyMocks}
import views.html.declaration.RenewRegistrationView

import java.time.LocalDate
import scala.concurrent.Future

class RenewRegistrationControllerSpec extends AmlsSpec with MockitoSugar with ScalaFutures with Injecting {

  trait Fixture extends AuthorisedFixture with DependencyMocks { self =>
    val request    = addToken(authRequest)
    lazy val view  = app.injector.instanceOf[RenewRegistrationView]
    val controller = new RenewRegistrationController(
      dataCacheConnector = mockCacheConnector,
      authAction = SuccessfulAuthAction,
      progressService = mock[ProgressService],
      statusService = mock[StatusService],
      renewalService = mock[RenewalService],
      ds = commonDependencies,
      cc = mockMcc,
      formProvider = inject[RenewRegistrationFormProvider],
      view = view
    )
  }

  "RenewRegistrationController" when {
    "get is called" must {
      "display the  renew registration question where not previously answered" in new Fixture {
        val date = LocalDate.now()

        when {
          controller.statusService.getStatus(any(), any(), any())(any(), any(), any())
        } thenReturn Future.successful(ReadyForRenewal(Some(date)))

        when {
          controller.dataCacheConnector.fetch[RenewRegistration](any(), any())(any())
        } thenReturn Future.successful(None)

        val result = controller.get()(request)

        status(result)          must be(OK)
        contentAsString(result) must include(Messages("declaration.renew.registration.title"))
      }

      "display the renew registration question with pre populated data" in new Fixture {
        val date = LocalDate.now()

        when {
          controller.statusService.getStatus(any(), any(), any())(any(), any(), any())
        } thenReturn Future.successful(ReadyForRenewal(Some(date)))

        when {
          controller.dataCacheConnector.fetch[RenewRegistration](any(), any())(any())
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
            val date = LocalDate.now()

            when {
              controller.statusService.getStatus(any(), any(), any())(any(), any(), any())
            } thenReturn Future.successful(ReadyForRenewal(Some(date)))

            mockCacheSave[RenewRegistration](RenewRegistrationYes, Some(RenewRegistration.key))

            val newRequest = FakeRequest(POST, routes.RenewRegistrationController.post().url)
              .withFormUrlEncodedBody("renewRegistration" -> "true")

            val result = controller.post()(newRequest)
            status(result)           must be(SEE_OTHER)
            redirectLocation(result) must be(Some(controllers.renewal.routes.WhatYouNeedController.get.url))
          }
        }

        "redirect to the url provided by progressService" in new Fixture {
          val call       = controllers.routes.RegistrationProgressController.get()
          val newRequest = FakeRequest(POST, routes.RenewRegistrationController.post().url)
            .withFormUrlEncodedBody("renewRegistration" -> "false")
          val date       = LocalDate.now()

          when {
            controller.statusService.getStatus(any(), any(), any())(any(), any(), any())
          } thenReturn Future.successful(ReadyForRenewal(Some(date)))

          mockCacheSave[RenewRegistration](RenewRegistrationNo, Some(RenewRegistration.key))

          when {
            controller.progressService.getSubmitRedirect(any[Option[String]](), any(), any())(any(), any(), any())
          } thenReturn Future.successful(Some(call))

          val result = controller.post()(newRequest)
          status(result) must be(SEE_OTHER)

          redirectLocation(result) must be(Some(call.url))
        }
      }

      "with invalid data" must {
        "respond with BAD_REQUEST" in new Fixture {
          val newRequest = FakeRequest(POST, routes.RenewRegistrationController.post().url)
            .withFormUrlEncodedBody(
              "renewRegistration" -> "1234567890"
            )
          val date       = LocalDate.now()

          when {
            controller.statusService.getStatus(any(), any(), any())(any(), any(), any())
          } thenReturn Future.successful(ReadyForRenewal(Some(date)))

          mockCacheSave[RenewRegistration](RenewRegistrationNo, Some(RenewRegistration.key))

          val result = controller.post()(newRequest)
          status(result) must be(BAD_REQUEST)

          contentAsString(result) must include(Messages("error.required.declaration.renew.registration"))
        }
      }
    }
  }
}
