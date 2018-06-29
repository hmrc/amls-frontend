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

package controllers.payments

import connectors.DataCacheConnector
import generators.PaymentGenerator
import models.{FeeResponse, SubmissionRequestStatus}
import models.ResponseType.SubscriptionResponseType
import models.status.SubmissionDecisionApproved
import org.joda.time.DateTime
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito.when
import play.api.i18n.Messages
import play.api.test.Helpers._
import services.{AuthEnrolmentsService, FeeResponseService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.frontend.auth.AuthContext
import utils.{AmlsSpec, AuthorisedFixture, DependencyMocks}

import scala.concurrent.{ExecutionContext, Future}

class BankDetailsControllerSpec extends AmlsSpec with PaymentGenerator {

  trait Fixture extends AuthorisedFixture with DependencyMocks { self =>

    val request = addToken(authRequest)

    implicit val hc: HeaderCarrier = new HeaderCarrier()
    implicit val ac: AuthContext = mock[AuthContext]
    implicit val ec: ExecutionContext = mock[ExecutionContext]

    val controller = new BankDetailsController(
      dataCacheConnector = mock[DataCacheConnector],
      authConnector = self.authConnector,
      authEnrolmentsService = mock[AuthEnrolmentsService],
      feeResponseService = mock[FeeResponseService],
      statusService = mockStatusService
    )

  }

  "BankDetailsController" when {

    "get is called" must {
      "return OK with view" in new Fixture {

        mockApplicationStatus(SubmissionDecisionApproved)

        when {
          controller.authEnrolmentsService.amlsRegistrationNumber(any(),any(),any())
        } thenReturn Future.successful(Some(amlsRegistrationNumber))

        when {
          controller.feeResponseService.getFeeResponse(eqTo(amlsRegistrationNumber))(any(),any(),any())
        } thenReturn Future.successful(Some(FeeResponse(
          SubscriptionResponseType,
          amlsRegistrationNumber, 100, None, 0, 200, Some(paymentReferenceNumber), None, DateTime.now()))
        )

        when {
            controller.dataCacheConnector.fetch[SubmissionRequestStatus](eqTo(SubmissionRequestStatus.key))(any(),any(),any())
        } thenReturn Future.successful(Some(SubmissionRequestStatus(true)))

        val result = controller.get()(request)

        status(result) must be(OK)
        contentAsString(result) must include(Messages("payments.bankdetails.title"))

      }
    }

  }

}
