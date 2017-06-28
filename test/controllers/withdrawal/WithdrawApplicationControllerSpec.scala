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
import models.withdrawal.WithdrawSubscriptionResponse
import org.joda.time.LocalDateTime
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatestplus.play.OneAppPerSuite
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers._
import services.{AuthEnrolmentsService, StatusService}
import utils.{AuthorisedFixture, DateHelper, GenericTestHelper}

import scala.concurrent.Future

class WithdrawApplicationControllerSpec extends GenericTestHelper with OneAppPerSuite {

  override lazy val app = new GuiceApplicationBuilder()
    .configure("microservice.services.feature-toggle.allow-withdrawal" -> true)
    .build()

  trait TestFixture extends AuthorisedFixture {
    self =>

    val request = addToken(authRequest)
    val amlsConnector = mock[AmlsConnector]
    val cacheConnector = mock[DataCacheConnector]
    val statusService = mock[StatusService]

    lazy val controller = new WithdrawApplicationController(authConnector, amlsConnector, cacheConnector, statusService)

    val amlsRegistrationNumber = "XA1234567890L"
    val businessName = "Test Business"
    val reviewDetails = mock[ReviewDetails]

    //noinspection ScalaStyle
    val processingDate = new LocalDateTime(2002, 1, 1, 12, 0, 0)
    val statusResponse = ReadStatusResponse(processingDate, "", None, None, None, None, renewalConFlag = false)

    when(reviewDetails.businessName).thenReturn(businessName)

    when {
      cacheConnector.fetch[BusinessMatching](eqTo(BusinessMatching.key))(any(), any(), any())
    } thenReturn Future.successful(BusinessMatching(reviewDetails.some).some)

    when {
      statusService.getDetailedStatus(any(), any(), any())
    } thenReturn Future.successful(SubmissionReadyForReview, statusResponse.some)
  }

  "The WithdrawApplication controller" when {
    "the get method is called" must {
      "show the 'withdraw your application' page" in new TestFixture {
        val result = controller.get()(request)
        status(result) mustBe OK
      }

      "show the business name" in new TestFixture {
        val result = controller.get()(request)
        contentAsString(result) must include(businessName)
      }

      "show the registration date" in new TestFixture {
        val result = controller.get()(request)
        contentAsString(result) must include(DateHelper.formatDate(processingDate))
      }
    }

    "go to WithdrawalReasonController" when {
      "the post method is called" in new TestFixture {
        val result = controller.post()(request)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe routes.WithdrawalReasonController.get().url.some

      }
    }
  }
}

class WithdrawApplicationControllerToggleOffSpec extends GenericTestHelper with OneAppPerSuite {

  override lazy val app = new GuiceApplicationBuilder()
    .configure("microservice.services.feature-toggle.allow-withdrawal" -> false)
    .build()

  trait TestFixture extends AuthorisedFixture {
    self =>

    val request = addToken(authRequest)
    val amlsConnector = mock[AmlsConnector]
    val cacheConnector = mock[DataCacheConnector]
    val statusService = mock[StatusService]

    lazy val controller = new WithdrawApplicationController(authConnector, amlsConnector, cacheConnector, statusService)
  }

  "The WithdrawApplicationController" when {
    "the GET method is called" must {
      "return 404 not found" in new TestFixture {
        status(controller.get(request)) mustBe NOT_FOUND
      }
    }
  }
}
