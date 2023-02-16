/*
 * Copyright 2023 HM Revenue & Customs
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
import models.status.ReadyForRenewal
import org.joda.time.LocalDate
import org.jsoup.Jsoup
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import play.api.i18n.Messages
import play.api.test.Helpers._
import services.{ProgressService, RenewalService, StatusService}
import utils.{AmlsSpec, AuthorisedFixture, DependencyMocks}
import views.html.declaration.renew_registration

import scala.concurrent.Future

class RenewRegistrationControllerSpec extends AmlsSpec with MockitoSugar with ScalaFutures {

  trait Fixture extends AuthorisedFixture with DependencyMocks { self =>
    val request = addToken(authRequest)
    lazy val view = app.injector.instanceOf[renew_registration]
    val controller = new RenewRegistrationController(
      dataCacheConnector = mockCacheConnector,
      authAction = SuccessfulAuthAction,
      progressService = mock[ProgressService],
      statusService = mock[StatusService],
      renewalService = mock[RenewalService],
      ds = commonDependencies,
      cc = mockMcc,
      renew_registration = view
    )
  }

  "RenewRegistrationController" when {
    "get is called" must {
      "display the  renew registration question where not previously answered" in new Fixture {
        val date = new LocalDate()

        when {
          controller.statusService.getStatus(any(),any(), any())(any(),any())
        } thenReturn Future.successful(ReadyForRenewal(Some(date)))

        when{
          controller.dataCacheConnector.fetch[RenewRegistration](any(), any())(any(), any())
        } thenReturn Future.successful(None)

        val result = controller.get()(request)

        status(result) must be(OK)
        contentAsString(result) must include(Messages("declaration.renew.registration.title"))
      }

      "display the renew registration question with pre populated data" in new Fixture {
        val date = new LocalDate()

        when {
          controller.statusService.getStatus(any(),any(), any())(any(),any())
        } thenReturn Future.successful(ReadyForRenewal(Some(date)))

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
            val date = new LocalDate()

            when {
              controller.statusService.getStatus(any(),any(), any())(any(),any())
            } thenReturn Future.successful(ReadyForRenewal(Some(date)))

            mockCacheSave[RenewRegistration](RenewRegistrationYes, Some(RenewRegistration.key))

            val newRequest = requestWithUrlEncodedBody("renewRegistration" -> "true")

            val result = controller.post()(newRequest)
            status(result) must be(SEE_OTHER)
            redirectLocation(result) must be(Some(controllers.renewal.routes.WhatYouNeedController.get.url))
          }
        }

        "redirect to the url provided by progressService" in new Fixture {
          val call = controllers.routes.RegistrationProgressController.get
          val newRequest = requestWithUrlEncodedBody("renewRegistration" -> "false")
          val date = new LocalDate()

          when {
            controller.statusService.getStatus(any(),any(), any())(any(),any())
          } thenReturn Future.successful(ReadyForRenewal(Some(date)))

          mockCacheSave[RenewRegistration](RenewRegistrationNo, Some(RenewRegistration.key))

          when {
            controller.progressService.getSubmitRedirect(any[Option[String]](), any(), any())(any(), any())
          } thenReturn Future.successful(Some(call))

          val result = controller.post()(newRequest)
          status(result) must be(SEE_OTHER)

          redirectLocation(result) must be(Some(call.url))
        }
      }

      "with invalid data" must {
        "respond with BAD_REQUEST" in new Fixture {
          val newRequest = requestWithUrlEncodedBody(
            "renewRegistration" -> "1234567890"
          )
          val date = new LocalDate()

          when {
            controller.statusService.getStatus(any(),any(), any())(any(),any())
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


