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

package controllers.deregister

import cats.implicits._
import connectors.{AmlsConnector, DataCacheConnector}
import models.businessmatching.{BusinessActivities, BusinessMatching, HighValueDealing, MoneyServiceBusiness}
import models.deregister.{DeRegisterSubscriptionRequest, DeRegisterSubscriptionResponse, DeregistrationReason}
import models.withdrawal.WithdrawSubscriptionRequest
import org.joda.time.LocalDate
import org.jsoup.Jsoup
import org.mockito.ArgumentCaptor
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatestplus.play.OneAppPerSuite
import play.api.i18n.Messages
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers._
import services.{AuthEnrolmentsService, StatusService}
import utils.{AuthorisedFixture, GenericTestHelper}

import scala.concurrent.Future

class DeregistrationReasonControllerSpec extends GenericTestHelper with OneAppPerSuite {

  override lazy val app = new GuiceApplicationBuilder()
    .configure("microservice.services.feature-toggle.allow-deregister" -> true)
    .build()

  trait TestFixture extends AuthorisedFixture {
    self =>

    val request = addToken(authRequest)
    val amlsConnector = mock[AmlsConnector]
    val authService = mock[AuthEnrolmentsService]
    val dataCacheConnector = mock[DataCacheConnector]
    val statusService = mock[StatusService]

    lazy val controller = new DeregistrationReasonController(authConnector, dataCacheConnector, amlsConnector, authService, statusService)

    val amlsRegistrationNumber = "XA1234567890L"

    when {
      authService.amlsRegistrationNumber(any(), any(), any())
    } thenReturn Future.successful(amlsRegistrationNumber.some)

    when {
      amlsConnector.deregister(eqTo(amlsRegistrationNumber), any())(any(), any(), any())
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

            when(controller.dataCacheConnector.fetch[BusinessMatching](eqTo(BusinessMatching.key))(any(),any(),any()))
              .thenReturn(Future.successful(Some(businessMatching)))

            val result = controller.get()(request)
            status(result) must be(OK)
            contentAsString(result) must include(Messages("deregistration.reason.heading"))

            val document = Jsoup.parse(contentAsString(result))
            document.getElementById("deregistrationReason-01").hasAttr("checked") must be(false)
            document.getElementById("deregistrationReason-02").hasAttr("checked") must be(false)
            document.getElementById("deregistrationReason-03").hasAttr("checked") must be(false)
            document.getElementById("deregistrationReason-04").hasAttr("checked") must be(false)
            document.getElementById("deregistrationReason-05").hasAttr("checked") must be(false)
            document.getElementById("deregistrationReason-06").hasAttr("checked") must be(false)
            document.getElementById("specifyOtherReason").`val`() must be("")

          }
        }
        "hides hvd option" when {
          "hvd is not present in business activities" in new TestFixture {

            val businessMatching = BusinessMatching(
              activities = Some(BusinessActivities(Set(MoneyServiceBusiness)))
            )

            when(controller.dataCacheConnector.fetch[BusinessMatching](eqTo(BusinessMatching.key))(any(),any(),any()))
              .thenReturn(Future.successful(Some(businessMatching)))

            val result = controller.get()(request)
            status(result) must be(OK)
            contentAsString(result) must include(Messages("deregistration.reason.heading"))

            val document = Jsoup.parse(contentAsString(result))
            document.getElementById("deregistrationReason-01").hasAttr("checked") must be(false)
            document.getElementById("deregistrationReason-02").hasAttr("checked") must be(false)
            document.getElementById("deregistrationReason-03").hasAttr("checked") must be(false)
            document.getElementById("deregistrationReason-04").hasAttr("checked") must be(false)
            document.getElementById("deregistrationReason-05") must be(null)
            document.getElementById("deregistrationReason-06").hasAttr("checked") must be(false)
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

              val newRequest = request.withFormUrlEncodedBody(
                "deregistrationReason" -> "01"
              )

              val result = controller.post()(newRequest)
              status(result) must be(SEE_OTHER)

              val captor = ArgumentCaptor.forClass(classOf[DeRegisterSubscriptionRequest])
              verify(amlsConnector).deregister(eqTo(amlsRegistrationNumber), captor.capture())(any(), any(), any())

              captor.getValue.deregistrationReason mustBe DeregistrationReason.OutOfScope

              redirectLocation(result) must be(Some(controllers.routes.LandingController.get().url))

            }
            "DeregistrationReason is selection with other reason" in new TestFixture {

              val newRequest = request.withFormUrlEncodedBody(
                "deregistrationReason" -> "06",
                "specifyOtherReason" -> "reason"
              )

              val result = controller.post()(newRequest)
              status(result) must be(SEE_OTHER)

              val captor = ArgumentCaptor.forClass(classOf[DeRegisterSubscriptionRequest])
              verify(amlsConnector).deregister(eqTo(amlsRegistrationNumber), captor.capture())(any(), any(), any())

              captor.getValue.deregistrationReason mustBe DeregistrationReason.Other("reason")
              captor.getValue.deregReasonOther mustBe "reason".some

              redirectLocation(result) must be(Some(controllers.routes.LandingController.get().url))
            }
          }
        }

      }

      "given invalid data" must {
        "return with BAD_REQUEST" in new TestFixture {

          val newRequest = request.withFormUrlEncodedBody(
            "deregistrationReason" -> "20"
          )

          val result = controller.post()(newRequest)
          status(result) must be(BAD_REQUEST)

        }
      }

      "unable to withdraw" must {
        "return InternalServerError" in new TestFixture {

          when {
            authService.amlsRegistrationNumber(any(), any(), any())
          } thenReturn Future.successful(None)

          val newRequest = request.withFormUrlEncodedBody(
            "deregistrationReason" -> "01"
          )

          val result = controller.post()(newRequest)
          status(result) must be(INTERNAL_SERVER_ERROR)

        }
      }

    }

  }
}
