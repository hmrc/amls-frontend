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

package controllers

import connectors.DataCacheConnector
import models.businesscustomer.ReviewDetails
import models.businessmatching.BusinessMatching
import org.scalatest.MustMatchers
import org.scalatestplus.play.OneAppPerSuite
import play.api.i18n.Messages
import play.api.test.Helpers._
import utils.{AuthorisedFixture, DateHelper, GenericTestHelper}
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito.when
import cats.implicits._
import models.ReadStatusResponse
import models.status.{SubmissionDecisionApproved, SubmissionReadyForReview}
import org.joda.time.{LocalDate, LocalDateTime}
import play.api.test.FakeRequest
import services.StatusService

import scala.concurrent.Future

class DeRegisterApplicationControllerSpec extends GenericTestHelper with MustMatchers with OneAppPerSuite {

  trait TestFixture extends AuthorisedFixture { self =>
    val request = addToken(authRequest)

    val businessName = "Test Business"
    val registrationDate = LocalDateTime.now()
    val reviewDetails = mock[ReviewDetails]
    val statusService = mock[StatusService]
    val dataCache = mock[DataCacheConnector]
    val statusResponse = ReadStatusResponse(registrationDate, "", None, None, None, None, renewalConFlag = false)
    val controller = new DeRegisterApplicationController(self.authConnector, messages, dataCache, statusService)

    when(reviewDetails.businessName).thenReturn(businessName)

    when {
      dataCache.fetch[BusinessMatching](eqTo(BusinessMatching.key))(any(), any(), any())
    } thenReturn Future.successful(BusinessMatching(reviewDetails.some).some)

    when {
      statusService.getDetailedStatus(any(), any(), any())
    } thenReturn Future.successful(SubmissionDecisionApproved, statusResponse.some)
  }

  "The DeRegisterApplicationController" when {
    "GET is called" must {
      "show the correct page" in new TestFixture {
        val result = controller.get()(request)
        status(result) mustBe OK
        contentAsString(result) must include(Messages("status.deregister.title"))
      }

      "show the name of the business" in new TestFixture {
        val result = controller.get()(request)
        contentAsString(result) must include(businessName)
      }

      "show the processing date" in new TestFixture {
        val result = controller.get()(request)
        contentAsString(result) must include(DateHelper.formatDate(registrationDate))
      }
    }
  }

}
