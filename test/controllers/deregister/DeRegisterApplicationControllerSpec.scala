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

package controllers.deregister

import cats.implicits._
import connectors.{AmlsConnector, DataCacheConnector}
import controllers.actions.SuccessfulAuthAction
import models.businesscustomer.ReviewDetails
import models.businessmatching.{BusinessActivities, BusinessActivity, BusinessMatching}
import models.businessmatching.BusinessActivity.AccountancyServices
import models.deregister.DeRegisterSubscriptionResponse
import models.registrationdetails.RegistrationDetails
import org.mockito.ArgumentMatchers.{eq => eqTo, _}
import org.mockito.Mockito.when
import play.api.test.Helpers._
import services.{AuthEnrolmentsService, StatusService}
import utils.{AmlsSpec, AuthorisedFixture}
import views.html.deregister.DeregisterApplicationView

import java.time.LocalDateTime
import scala.concurrent.Future

class DeRegisterApplicationControllerSpec extends AmlsSpec {

  trait TestFixture extends AuthorisedFixture { self =>
    val request = addToken(authRequest)

    val businessName         = "Business Name from registration details"
    val applicationReference = "SUIYD3274890384"
    val safeId               = "X87FUDIKJJKJH87364"
    val registrationDate     = LocalDateTime.now()
    val reviewDetails        = mock[ReviewDetails]
    val activities           = mock[BusinessActivities]
    val statusService        = mock[StatusService]
    val dataCache            = mock[DataCacheConnector]
    val enrolments           = mock[AuthEnrolmentsService]
    val amlsConnector        = mock[AmlsConnector]
    lazy val view            = app.injector.instanceOf[DeregisterApplicationView]
    val controller           = new DeRegisterApplicationController(
      SuccessfulAuthAction,
      ds = commonDependencies,
      dataCache,
      statusService,
      enrolments,
      amlsConnector,
      mockMcc,
      view = view
    )

    when {
      dataCache.fetch[BusinessMatching](any(), eqTo(BusinessMatching.key))(any())
    } thenReturn Future.successful(BusinessMatching(reviewDetails.some, activities.some).some)

    when {
      amlsConnector.registrationDetails(any(), eqTo(safeId))(any(), any())
    } thenReturn Future.successful(RegistrationDetails(businessName, isIndividual = false))

    when(activities.businessActivities).thenReturn(Set[BusinessActivity](AccountancyServices))

    when {
      enrolments.amlsRegistrationNumber(Some(any()), Some(any()))(any(), any())
    } thenReturn Future.successful(applicationReference.some)

    when {
      amlsConnector.deregister(any(), any(), any())(any(), any())
    } thenReturn Future.successful(DeRegisterSubscriptionResponse("Some date"))

    when {
      controller.statusService.getSafeIdFromReadStatus(any(), any(), any())(any(), any())
    } thenReturn Future.successful(Some(safeId))
  }

  "The DeRegisterApplicationController" when {
    "GET is called" must {
      "show the correct page" in new TestFixture {
        val result = controller.get()(request)
        status(result) mustBe OK
        contentAsString(result) must include(
          messages("Deregister Business Name from registration details under the Money Laundering Regulations")
        )
      }

      "show the name of the business" in new TestFixture {
        val result = controller.get()(request)
        contentAsString(result) must include(businessName)
      }
    }

    "POST is called" must {
      "make a request to the middle tier to perform the deregistration" in new TestFixture {
        val result = controller.post()(request)
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe routes.DeregistrationReasonController.get.url.some
      }
    }
  }
}
