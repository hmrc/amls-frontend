/*
 * Copyright 2019 HM Revenue & Customs
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

package controllers.declaration

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.actions.SuccessfulAuthAction
import models.declaration.release7.RoleWithinBusinessRelease7
import models.declaration.{AddPerson, InternalAccountant}
import models.status.{NotCompleted, ReadyForRenewal, SubmissionReadyForReview}
import models.{ReadStatusResponse, SubscriptionFees, SubscriptionResponse}
import org.joda.time.{LocalDate, LocalDateTime}
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import utils.{AmlsSpec, AuthorisedFixture, DependencyMocks}
import play.api.i18n.Messages
import play.api.test.FakeApplication
import play.api.test.Helpers._
import services.StatusService
import uk.gov.hmrc.http.cache.client.CacheMap

import scala.concurrent.Future

class DeclarationControllerSpec extends AmlsSpec with MockitoSugar with ScalaFutures {

  trait Fixture extends AuthorisedFixture with DependencyMocks {
    self =>
    val request = addToken(authRequest)

    val declarationController = new DeclarationController (
      authAction = SuccessfulAuthAction,
      dataCacheConnector = mock[DataCacheConnector],
      statusService = mockStatusService
    )
    val response = SubscriptionResponse(
      etmpFormBundleNumber = "",
      amlsRefNo = "", Some(SubscriptionFees(
      registrationFee = 0,
      fpFee = None,
      fpFeeRate = None,
      approvalCheckFee = None,
      approvalCheckFeeRate = None,
      premiseFee = 0,
      premiseFeeRate = None,
      totalFees = 0,
      paymentReference = "")
      )
    )
    val pendingReadStatusResponse = ReadStatusResponse(LocalDateTime.now(), "Pending", None, None, None,
      None, false)
    val notCompletedReadStatusResponse = ReadStatusResponse(LocalDateTime.now(), "NotCompleted", None, None, None,
      None, false)
    val addPerson = AddPerson("firstName", Some("middleName"), "lastName",
      RoleWithinBusinessRelease7(Set(models.declaration.release7.InternalAccountant)))
  }

  "Declaration get" must {

    "redirect to the declaration-persons page if name and/or business matching not found" in new Fixture {

      when(declarationController.dataCacheConnector.fetch[AddPerson](any(), any())
        (any(), any())).thenReturn(Future.successful(None))

      mockApplicationStatus(NotCompleted)

      val result = declarationController.get()(request)
      status(result) must be(SEE_OTHER)

      redirectLocation(result) mustBe Some(routes.AddPersonController.get().url)
    }

    "load the declaration page for pre-submissions if name and business matching is found" in new Fixture {

      when(declarationController.dataCacheConnector.fetch[AddPerson](any(), any())
        (any(), any())).thenReturn(Future.successful(Some(addPerson)))

      mockApplicationStatus(NotCompleted)

      val result = declarationController.get()(request)
      status(result) must be(OK)

      contentAsString(result) must include(addPerson.firstName)
      contentAsString(result) must include(addPerson.middleName mkString)
      contentAsString(result) must include(addPerson.lastName)
      contentAsString(result) must include(Messages("submit.registration"))
    }

    "load the declaration page for pre-submissions if name and business matching is found (renewal)" in new Fixture {

      when(declarationController.dataCacheConnector.fetch[AddPerson](any(), any())
        (any(), any())).thenReturn(Future.successful(Some(addPerson)))

      mockApplicationStatus(ReadyForRenewal(Some(new LocalDate())))

      val result = declarationController.get()(request)
      status(result) must be(OK)

      contentAsString(result) must include(addPerson.firstName)
      contentAsString(result) must include(addPerson.middleName mkString)
      contentAsString(result) must include(addPerson.lastName)
      contentAsString(result) must include(Messages("submit.renewal.application"))
    }

    "load the declaration page for pre-submissions if name and business matching is found (amendment)" in new Fixture {

      when(declarationController.dataCacheConnector.fetch[AddPerson](any(), any())
        (any(), any())).thenReturn(Future.successful(Some(addPerson)))

      mockApplicationStatus(SubmissionReadyForReview)

      val result = declarationController.get()(request)
      status(result) must be(OK)

      contentAsString(result) must include(addPerson.firstName)
      contentAsString(result) must include(addPerson.middleName mkString)
      contentAsString(result) must include(addPerson.lastName)
      contentAsString(result) must include(Messages("submit.amendment.application"))
    }

    "report error if retrieval of amlsRegNo fails" in new Fixture {
      when(declarationController.dataCacheConnector.fetch[AddPerson](any(), any())
        (any(), any())).thenReturn(Future.successful(Some(addPerson)))

      mockApplicationStatus(NotCompleted)

      val result = declarationController.get()(request)
      status(result) must be(OK)

      contentAsString(result) must include(addPerson.firstName)
      contentAsString(result) must include(addPerson.middleName mkString)
      contentAsString(result) must include(addPerson.lastName)
      contentAsString(result) must include(Messages("submit.registration"))
      contentAsString(result) must include(Messages("declaration.declaration.title"))
    }

  }

  "Declaration getWithAmendment" must {
    "load the declaration for amendments page for submissions if name and business matching is found" in new Fixture {

      when(declarationController.dataCacheConnector.fetch[AddPerson](any(), any())
        (any(), any())).thenReturn(Future.successful(Some(addPerson)))

      val result = declarationController.getWithAmendment()(request)
      status(result) must be(OK)

      contentAsString(result) must include(addPerson.firstName)
      contentAsString(result) must include(addPerson.middleName mkString)
      contentAsString(result) must include(addPerson.lastName)
      contentAsString(result) must include(Messages("submit.amendment.application"))
      contentAsString(result) must include(Messages("declaration.declaration.amendment.title"))
    }

    "redirect to the declaration-persons page if name and/or business matching not found" in new Fixture {

      when(declarationController.dataCacheConnector.fetch[AddPerson](any(), any())
        (any(), any())).thenReturn(Future.successful(None))

      mockApplicationStatus(NotCompleted)

      val result = declarationController.getWithAmendment()(request)
      status(result) must be(SEE_OTHER)

      redirectLocation(result) mustBe Some(routes.AddPersonController.get().url)
    }

    "redirect to the declaration-persons for amendments page if name and/or business matching not found and submission is ready for review" in new Fixture {

      when(declarationController.dataCacheConnector.fetch[AddPerson](any(), any())
        (any(), any())).thenReturn(Future.successful(None))

      mockApplicationStatus(SubmissionReadyForReview)

      val result = declarationController.getWithAmendment()(request)
      status(result) must be(SEE_OTHER)

      redirectLocation(result) mustBe Some(routes.AddPersonController.getWithAmendment().url)
    }
  }
}
