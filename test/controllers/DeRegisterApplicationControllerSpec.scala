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

import cats.implicits._
import connectors.DataCacheConnector
import models.ReadStatusResponse
import models.businesscustomer.ReviewDetails
import models.businessmatching.BusinessMatching
import models.status.SubmissionDecisionApproved
import org.joda.time.LocalDateTime
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito.when
import org.scalatest.MustMatchers
import org.scalatestplus.play.OneAppPerSuite
import play.api.i18n.Messages
import play.api.test.Helpers._
import services.{AuthEnrolmentsService, StatusService}
import utils.{AuthorisedFixture, DateHelper, GenericTestHelper}

import scala.concurrent.Future

class DeRegisterApplicationControllerSpec extends GenericTestHelper with MustMatchers with OneAppPerSuite {

  trait TestFixture extends AuthorisedFixture { self =>
    val request = addToken(authRequest)

    val businessName = "Test Business"
    val applicationReference = "SUIYD3274890384"
    val registrationDate = LocalDateTime.now()
    val reviewDetails = mock[ReviewDetails]
    val statusService = mock[StatusService]
    val dataCache = mock[DataCacheConnector]
    val enrolments = mock[AuthEnrolmentsService]
    val statusResponse = ReadStatusResponse(registrationDate, "", None, None, None, None, renewalConFlag = false)
    val controller = new DeRegisterApplicationController(self.authConnector, messages, dataCache, statusService, enrolments)

    when(reviewDetails.businessName).thenReturn(businessName)

    when {
      dataCache.fetch[BusinessMatching](eqTo(BusinessMatching.key))(any(), any(), any())
    } thenReturn Future.successful(BusinessMatching(reviewDetails.some).some)

    when {
      statusService.getDetailedStatus(any(), any(), any())
    } thenReturn Future.successful(SubmissionDecisionApproved, statusResponse.some)

    when {
      enrolments.amlsRegistrationNumber(any(), any(), any())
    } thenReturn Future.successful(applicationReference.some)
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

      "show the application reference" in new TestFixture {
        val result = controller.get()(request)
        contentAsString(result) must include(applicationReference)
      }
    }
  }
}
