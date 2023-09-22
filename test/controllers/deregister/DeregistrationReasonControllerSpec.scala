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

package controllers.deregister

import cats.implicits._
import connectors.{AmlsConnector, DataCacheConnector}
import controllers.actions.SuccessfulAuthAction
import forms.deregister.DeregistrationReasonFormProvider
import models.businessmatching.{BusinessActivities, BusinessMatching}
import models.businessmatching.BusinessActivity.{HighValueDealing, MoneyServiceBusiness}
import models.deregister.DeregistrationReason.{HVDPolicyOfNotAcceptingHighValueCashPayments, Other, OutOfScope}
import models.deregister.{DeRegisterSubscriptionRequest, DeRegisterSubscriptionResponse, DeregistrationReason}
import org.jsoup.Jsoup
import org.mockito.ArgumentCaptor
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Injecting}
import services.{AuthEnrolmentsService, StatusService}
import utils.{AmlsSpec, AuthorisedFixture}
import views.html.deregister.DeregistrationReasonView

import scala.concurrent.Future

class DeregistrationReasonControllerSpec extends AmlsSpec with Injecting {

  trait TestFixture extends AuthorisedFixture {
    self =>

    val request = addToken(authRequest)
    val amlsConnector = mock[AmlsConnector]
    val authService = mock[AuthEnrolmentsService]
    val dataCacheConnector = mock[DataCacheConnector]
    val statusService = mock[StatusService]
    lazy val view = inject[DeregistrationReasonView]
    lazy val controller = new DeregistrationReasonController(
      SuccessfulAuthAction,
      ds = commonDependencies,
      dataCacheConnector,
      amlsConnector,
      authService,
      mockMcc,
      formProvider = inject[DeregistrationReasonFormProvider],
      view = view
    )

    val amlsRegistrationNumber = "XA1234567890L"

    when {
      authService.amlsRegistrationNumber(Some(any()), Some(any()))(any(), any())
    } thenReturn Future.successful(amlsRegistrationNumber.some)

    when {
      amlsConnector.deregister(eqTo(amlsRegistrationNumber), any(), any())(any(), any())
    } thenReturn Future.successful(mock[DeRegisterSubscriptionResponse])

  }

  "DeregistrationReasonController" when {

    "get is called" must {
      "display deregistration_reasons view without data" which {
        "shows hvd option" when {
          "hvd is present in business activities" in new TestFixture {

            val businessMatching = BusinessMatching(
              activities = Some(BusinessActivities(Set(HighValueDealing)))
            )

            when(controller.dataCacheConnector.fetch[BusinessMatching](any(), eqTo(BusinessMatching.key))(any(),any()))
              .thenReturn(Future.successful(Some(businessMatching)))

            val result = controller.get()(request)
            status(result) must be(OK)
            contentAsString(result) must include(messages("deregistration.reason.heading"))

            val document = Jsoup.parse(contentAsString(result))

            DeregistrationReason.all foreach { reason =>
              document.getElementById(reason.toString).hasAttr("checked") must be(false)
            }

            document.getElementById("specifyOtherReason").`val`() must be("")
          }
        }

        "hides hvd option" when {
          "hvd is not present in business activities" in new TestFixture {

            val businessMatching = BusinessMatching(
              activities = Some(BusinessActivities(Set(MoneyServiceBusiness)))
            )

            when(controller.dataCacheConnector.fetch[BusinessMatching](any(), eqTo(BusinessMatching.key))(any(),any()))
              .thenReturn(Future.successful(Some(businessMatching)))

            val result = controller.get()(request)
            status(result) must be(OK)
            contentAsString(result) must include(messages("deregistration.reason.heading"))

            val document = Jsoup.parse(contentAsString(result))

            DeregistrationReason.all diff Seq(HVDPolicyOfNotAcceptingHighValueCashPayments) foreach { reason =>
              document.getElementById(reason.toString).hasAttr("checked") must be(false)
            }

            document.select("input[type=radio]").size() must be(DeregistrationReason.all.length - 1)
            document.getElementById("specifyOtherReason").`val`() must be("")
          }
        }
      }
    }

    "post is called" when {

      "given valid data" must {

        "go to landing controller" which {
          "follows sending a deregistration to amls" when {
            "deregistrationReason is selection without other reason" in new TestFixture {

              val newRequest = FakeRequest(POST, routes.DeregistrationReasonController.post().url)
              .withFormUrlEncodedBody(
                "deregistrationReason" -> OutOfScope.toString
              )

              val result = controller.post()(newRequest)
              status(result) must be(SEE_OTHER)

              val captor = ArgumentCaptor.forClass(classOf[DeRegisterSubscriptionRequest])
              verify(amlsConnector).deregister(eqTo(amlsRegistrationNumber), captor.capture(), any())(any(), any())

              captor.getValue.deregistrationReason mustBe DeregistrationReason.OutOfScope

              redirectLocation(result) must be(Some(controllers.routes.LandingController.get.url))
            }

            "DeregistrationReason is selection with other reason" in new TestFixture {

              val newRequest = FakeRequest(POST, routes.DeregistrationReasonController.post().url)
              .withFormUrlEncodedBody(
                "deregistrationReason" -> Other("").toString,
                "specifyOtherReason" -> "reason"
              )

              val result = controller.post()(newRequest)
              status(result) must be(SEE_OTHER)

              val captor = ArgumentCaptor.forClass(classOf[DeRegisterSubscriptionRequest])
              verify(amlsConnector).deregister(eqTo(amlsRegistrationNumber), captor.capture(), any())(any(), any())

              captor.getValue.deregistrationReason mustBe DeregistrationReason.Other("reason")
              captor.getValue.deregReasonOther mustBe "reason".some

              redirectLocation(result) must be(Some(controllers.routes.LandingController.get.url))
            }
          }
        }
      }

      "given invalid data" must {
        "return with BAD_REQUEST with HVD" in new TestFixture {

          val businessMatching = BusinessMatching(
            activities = Some(BusinessActivities(Set(HighValueDealing)))
          )

          when(controller.dataCacheConnector.fetch[BusinessMatching](any(), eqTo(BusinessMatching.key))(any(),any()))
            .thenReturn(Future.successful(Some(businessMatching)))

          val newRequest = FakeRequest(POST, routes.DeregistrationReasonController.post().url)
          .withFormUrlEncodedBody(
            "deregistrationReason" -> "foo"
          )

          val result = controller.post()(newRequest)
          status(result) must be(BAD_REQUEST)
        }

        "return with BAD_REQUEST no HVD" in new TestFixture {

          val businessMatching = BusinessMatching(
            activities = Some(BusinessActivities(Set(MoneyServiceBusiness)))
          )

          when(controller.dataCacheConnector.fetch[BusinessMatching](any(), eqTo(BusinessMatching.key))(any(),any()))
            .thenReturn(Future.successful(Some(businessMatching)))

          val newRequest = FakeRequest(POST, routes.DeregistrationReasonController.post().url)
          .withFormUrlEncodedBody(
            "deregistrationReason" -> "bar"
          )

          val result = controller.post()(newRequest)
          status(result) must be(BAD_REQUEST)
        }
      }

      "unable to withdraw" must {
        "return InternalServerError" in new TestFixture {

          when {
            authService.amlsRegistrationNumber(Some(any()), Some(any()))(any(), any())
          } thenReturn Future.successful(None)

          val newRequest = FakeRequest(POST, routes.DeregistrationReasonController.post().url)
          .withFormUrlEncodedBody(
            "deregistrationReason" -> OutOfScope.toString
          )

          val result = controller.post()(newRequest)
          status(result) must be(INTERNAL_SERVER_ERROR)
        }
      }
    }
  }
}
