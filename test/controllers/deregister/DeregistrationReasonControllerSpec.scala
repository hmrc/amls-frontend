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

package controllers.deregister

import cats.implicits._
import connectors.{AmlsConnector, DataCacheConnector}
import models.deregister.DeRegisterSubscriptionResponse
import models.withdrawal.{WithdrawSubscriptionRequest, WithdrawSubscriptionResponse, WithdrawalReason}
import org.joda.time.LocalDate
import org.jsoup.Jsoup
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalacheck.Prop.Exception
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

    lazy val controller = new DeregistrationReasonController(authConnector, amlsConnector, authService, dataCacheConnector, statusService)

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

      "display deregistration_reasons view without data" in new TestFixture {

        val result = controller.get()(request)
        status(result) must be(OK)
        contentAsString(result) must include(Messages("deregistration.reason.title"))

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
  }
}

class DeregistrationReasonControllerToggleOffSpec extends GenericTestHelper with OneAppPerSuite {

  override lazy val app = new GuiceApplicationBuilder()
    .configure("microservice.services.feature-toggle.allow-deregister" -> false)
    .build()

  trait TestFixture extends AuthorisedFixture {
    self =>

    val request = addToken(authRequest)
    val amlsConnector = mock[AmlsConnector]
    val dataCacheConnector = mock[DataCacheConnector]
    val authService = mock[AuthEnrolmentsService]
    val statusService = mock[StatusService]

    lazy val controller = new DeregistrationReasonController(authConnector, amlsConnector, authService, dataCacheConnector, statusService)
  }

  "The DeregistrationReasonController" when {
    "the GET method is called" must {
      "return 404 not found" in new TestFixture {
        status(controller.get(request)) mustBe NOT_FOUND
      }
    }
  }
}