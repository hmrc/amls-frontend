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
import models.ReadStatusResponse
import models.businesscustomer.ReviewDetails
import models.businessmatching.{AccountancyServices, BusinessActivities, BusinessActivity, BusinessMatching}
import models.deregister.DeRegisterSubscriptionResponse
import models.status.SubmissionDecisionApproved
import org.joda.time.{LocalDate, LocalDateTime}
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito.when
import org.scalatest.MustMatchers
import org.scalatestplus.play.OneAppPerSuite
import play.api.i18n.Messages
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers._
import services.{AuthEnrolmentsService, StatusService}
import utils.{AuthorisedFixture, DateHelper, GenericTestHelper}

import scala.concurrent.Future

class DeRegisterApplicationControllerSpec extends GenericTestHelper with MustMatchers with OneAppPerSuite {

  implicit override lazy val app = new GuiceApplicationBuilder()
    .configure("microservice.services.feature-toggle.allow-deregister" -> true)
    .configure("microservice.services.feature-toggle.business-matching-variation" -> true)
    .build()

  trait TestFixture extends AuthorisedFixture { self =>
    val request = addToken(authRequest)

    val businessName = "Test Business"
    val applicationReference = "SUIYD3274890384"
    val registrationDate = LocalDateTime.now()
    val reviewDetails = mock[ReviewDetails]
    val activities = mock[BusinessActivities]
    val statusService = mock[StatusService]
    val dataCache = mock[DataCacheConnector]
    val enrolments = mock[AuthEnrolmentsService]
    val amlsConnector = mock[AmlsConnector]
    val controller = new DeRegisterApplicationController(self.authConnector, dataCache, statusService, enrolments, amlsConnector)

    when {
      dataCache.fetch[BusinessMatching](eqTo(BusinessMatching.key))(any(), any(), any())
    } thenReturn Future.successful(BusinessMatching(reviewDetails.some, activities.some).some)

    when(reviewDetails.businessName).thenReturn(businessName)

    when(activities.businessActivities).thenReturn(Set[BusinessActivity](AccountancyServices))

    when {
      enrolments.amlsRegistrationNumber(any(), any(), any())
    } thenReturn Future.successful(applicationReference.some)

    when {
      amlsConnector.deregister(any(), any())(any(), any(), any())
    } thenReturn Future.successful(DeRegisterSubscriptionResponse("Some date"))


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

      "show the application reference" in new TestFixture {
        val result = controller.get()(request)
        contentAsString(result) must include(applicationReference)
      }

      "show the business activities" in new TestFixture {
        val result = controller.get()(request)
        contentAsString(result) must include("Accountancy services")
      }

      "contain a link to change the business activities" in new TestFixture {
        val result = controller.get()(request)
        contentAsString(result) must include("<a href=\"/anti-money-laundering/business-matching/check-your-answers\">")
      }
    }

    "POST is called" must {
      "make a request to the middle tier to perform the deregistration" in new TestFixture {
        val result = controller.post()(request)
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe routes.DeregistrationReasonController.get().url.some
      }
    }
  }
}

class DeRegisterApplicationControllerWithoutBusinessMatchingVariationToggleSpec extends GenericTestHelper with MustMatchers with OneAppPerSuite {

  implicit override lazy val app = new GuiceApplicationBuilder()
    .configure("microservice.services.feature-toggle.allow-deregister" -> true)
    .configure("microservice.services.feature-toggle.business-matching-variation" -> false)
    .build()

  trait TestFixture extends AuthorisedFixture { self =>
    val request = addToken(authRequest)

    val businessName = "Test Business"
    val applicationReference = "SUIYD3274890384"
    val registrationDate = LocalDateTime.now()
    val reviewDetails = mock[ReviewDetails]
    val activities = mock[BusinessActivities]
    val statusService = mock[StatusService]
    val dataCache = mock[DataCacheConnector]
    val enrolments = mock[AuthEnrolmentsService]
    val amlsConnector = mock[AmlsConnector]
    val controller = new DeRegisterApplicationController(self.authConnector, dataCache, statusService, enrolments, amlsConnector)

    when {
      dataCache.fetch[BusinessMatching](eqTo(BusinessMatching.key))(any(), any(), any())
    } thenReturn Future.successful(BusinessMatching(reviewDetails.some, activities.some).some)

    when(reviewDetails.businessName).thenReturn(businessName)

    when(activities.businessActivities).thenReturn(Set[BusinessActivity](AccountancyServices))

    when {
      enrolments.amlsRegistrationNumber(any(), any(), any())
    } thenReturn Future.successful(applicationReference.some)

    when {
      amlsConnector.deregister(any(), any())(any(), any(), any())
    } thenReturn Future.successful(DeRegisterSubscriptionResponse("Some date"))


  }

  "The DeRegisterApplicationController" when {
    "GET is called" must {

      "NOT contain a link to change the business activities" in new TestFixture {
        val result = controller.get()(request)
        contentAsString(result) mustNot include("<a href=\"/anti-money-laundering/business-matching/check-your-answers\">")
      }
    }
  }
}

