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

package controllers.withdrawal

import cats.implicits._
import connectors.{AmlsConnector, DataCacheConnector}
import models.ReadStatusResponse
import models.businesscustomer.ReviewDetails
import models.businessmatching.BusinessMatching
import models.status.SubmissionReadyForReview
import models.withdrawal.{WithdrawSubscriptionResponse, WithdrawalReason}
import org.joda.time.LocalDateTime
import org.jsoup.Jsoup
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatestplus.play.OneAppPerSuite
import play.api.i18n.Messages
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers._
import services.{AuthEnrolmentsService, StatusService}
import utils.{AuthorisedFixture, DateHelper, GenericTestHelper}

import scala.concurrent.Future

class WithdrawalReasonControllerSpec extends GenericTestHelper with OneAppPerSuite{

  override lazy val app = new GuiceApplicationBuilder()
    .configure("microservice.services.feature-toggle.allow-withdrawal" -> true)
    .build()

  trait TestFixture extends AuthorisedFixture {
    self =>

    val request = addToken(authRequest)
    val amlsConnector = mock[AmlsConnector]
    val authService = mock[AuthEnrolmentsService]
    val cacheConnector = mock[DataCacheConnector]
    val statusService = mock[StatusService]

    lazy val controller = new WithdrawalReasonController(authConnector, amlsConnector, authService, cacheConnector, statusService)

  }


  "WithdrawalReasonController" when {

    "get is called" must {

      "display withdrawal_reasons view without data" in new TestFixture{

        when(controller.dataCacheConnector.fetch[WithdrawalReason](any())(any(), any(), any()))
          .thenReturn(Future.successful(None))

        val result = controller.get()(request)
        status(result) must be(OK)
        contentAsString(result) must include(Messages("withdrawal.reason.title"))

        val document = Jsoup.parse(contentAsString(result))
        document.getElementById("withdrawalReason-01").hasAttr("checked") must be(false)
        document.getElementById("withdrawalReason-02").hasAttr("checked") must be(false)
        document.getElementById("withdrawalReason-03").hasAttr("checked") must be(false)
        document.getElementById("withdrawalReason-04").hasAttr("checked") must be(false)
        document.getElementById("specifyOtherReason").`val`() is empty
      }

      "display the page with pre-populated data" in new TestFixture{
        val result = controller.get()(request)
        status(result) must be(OK)
      }
    }

  }

}

class WithdrawalReasonControllerToggleOffSpec extends GenericTestHelper with OneAppPerSuite {

  override lazy val app = new GuiceApplicationBuilder()
    .configure("microservice.services.feature-toggle.allow-withdrawal" -> false)
    .build()

  trait TestFixture extends AuthorisedFixture {
    self =>

    val request = addToken(authRequest)
    val amlsConnector = mock[AmlsConnector]
    val authService = mock[AuthEnrolmentsService]
    val cacheConnector = mock[DataCacheConnector]
    val statusService = mock[StatusService]

    lazy val controller = new WithdrawalReasonController(authConnector, amlsConnector, authService, cacheConnector, statusService)
  }

  "The WithdrawalReasonController" when {
    "the GET method is called" must {
      "return 404 not found" in new TestFixture {
        status(controller.get(request)) mustBe NOT_FOUND
      }
    }
  }
}