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

package controllers.declaration

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import models.declaration.release7.RoleWithinBusinessRelease7
import models.declaration.{AddPerson, InternalAccountant}
import models.status.{NotCompleted, SubmissionReadyForReview}
import models.{ReadStatusResponse, SubscriptionFees, SubscriptionResponse}
import org.joda.time.LocalDateTime
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import utils.GenericTestHelper
import play.api.i18n.Messages
import play.api.test.FakeApplication
import play.api.test.Helpers._
import services.StatusService
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.AuthorisedFixture

import scala.concurrent.Future

class DeclarationControllerWithAmendmentToggleOffSpec extends GenericTestHelper with MockitoSugar with ScalaFutures {
  override lazy val app = FakeApplication(additionalConfiguration = Map("microservice.services.feature-toggle.amendments" -> false))

  trait Fixture extends AuthorisedFixture {
    self =>
    val request = addToken(authRequest)

    val declarationController = new DeclarationController {
      override val authConnector = self.authConnector
      override val dataCacheConnector = mock[DataCacheConnector]
      override val statusService = mock[StatusService]
    }

    val mockCacheMap = mock[CacheMap]
    val response = SubscriptionResponse(
      etmpFormBundleNumber = "",
      amlsRefNo = "", Some(SubscriptionFees(
        registrationFee = 0,
        fpFee = None,
        fpFeeRate = None,
        premiseFee = 0,
        premiseFeeRate = None,
        totalFees = 0,
        paymentReference = "")
      )
    )
    val pendingReadStatusResponse = ReadStatusResponse(LocalDateTime.now(), "Pending", None, None, None, None, false)
    val notCompletedReadStatusResponse = ReadStatusResponse(LocalDateTime.now(), "NotCompleted", None, None, None, None, false)
    val addPerson = AddPerson("firstName", Some("middleName"), "LastName", RoleWithinBusinessRelease7(Set(models.declaration.release7.InternalAccountant)))
  }

  "Declaration get" must {

    "use the correct services" in new Fixture {
      DeclarationController.authConnector must be(AMLSAuthConnector)
      DeclarationController.dataCacheConnector must be(DataCacheConnector)
      DeclarationController.statusService must be(StatusService)
    }

    "redirect to the declaration-persons page if name and/or business matching not found" in new Fixture {

      when(declarationController.dataCacheConnector.fetch[AddPerson](any())
        (any(), any(), any())).thenReturn(Future.successful(None))

      when(declarationController.statusService.getStatus(any(), any(), any())).thenReturn(Future.successful(NotCompleted))

      val result = declarationController.get()(request)
      status(result) must be(SEE_OTHER)

      redirectLocation(result) mustBe Some(routes.AddPersonController.get().url)
    }

    "load the declaration page for pre-submissions if name and business matching is found" in new Fixture {

      when(declarationController.dataCacheConnector.fetch[AddPerson](any())
        (any(), any(), any())).thenReturn(Future.successful(Some(addPerson)))

      val result = declarationController.get()(request)
      status(result) must be(OK)

      contentAsString(result) must include(addPerson.firstName)
      contentAsString(result) must include(addPerson.middleName mkString)
      contentAsString(result) must include(addPerson.lastName)
      contentAsString(result) must include(Messages("submit.registration"))
    }

    "report error if retrieval of amlsRegNo fails" in new Fixture {
      when(declarationController.dataCacheConnector.fetch[AddPerson](any())
        (any(), any(), any())).thenReturn(Future.successful(Some(addPerson)))

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

      when(declarationController.dataCacheConnector.fetch[AddPerson](any())
        (any(), any(), any())).thenReturn(Future.successful(Some(addPerson)))

      val result = declarationController.getWithAmendment()(request)
      status(result) must be(OK)

      contentAsString(result) must include(addPerson.firstName)
      contentAsString(result) must include(addPerson.middleName mkString)
      contentAsString(result) must include(addPerson.lastName)
      contentAsString(result) must include(Messages("submit.registration"))
      contentAsString(result) must include(Messages("declaration.declaration.title"))
    }

    "redirect to the declaration-persons page if name and/or business matching not found" in new Fixture {

      when(declarationController.dataCacheConnector.fetch[AddPerson](any())
        (any(), any(), any())).thenReturn(Future.successful(None))

      when(declarationController.statusService.getStatus(any(), any(), any())).thenReturn(Future.successful(NotCompleted))

      val result = declarationController.getWithAmendment()(request)
      status(result) must be(SEE_OTHER)

      redirectLocation(result) mustBe Some(routes.AddPersonController.get().url)
    }
  }
}

class DeclarationControllerWithAmendmentToggleOnSpec extends GenericTestHelper with MockitoSugar with ScalaFutures {
  override lazy val app = FakeApplication(additionalConfiguration = Map("microservice.services.feature-toggle.amendments" -> true))

  trait Fixture extends AuthorisedFixture {
    self =>
    val request = addToken(authRequest)

    val declarationController = new DeclarationController {
      override val authConnector = self.authConnector
      override val dataCacheConnector = mock[DataCacheConnector]
      override val statusService = mock[StatusService]
    }

    val mockCacheMap = mock[CacheMap]
    val response = SubscriptionResponse(
      etmpFormBundleNumber = "",
      amlsRefNo = "", Some(SubscriptionFees(
      registrationFee = 0,
      fpFee = None,
      fpFeeRate = None,
      premiseFee = 0,
      premiseFeeRate = None,
      totalFees = 0,
      paymentReference = "")
      )
    )
    val pendingReadStatusResponse = ReadStatusResponse(LocalDateTime.now(), "Pending", None, None, None, None, false)
    val notCompletedReadStatusResponse = ReadStatusResponse(LocalDateTime.now(), "NotCompleted", None, None, None, None, false)
    val addPerson = AddPerson("firstName", Some("middleName"), "lastName", RoleWithinBusinessRelease7(Set(models.declaration.release7.InternalAccountant)))
  }

  "Declaration get" must {

    "use the correct services" in new Fixture {
      DeclarationController.authConnector must be(AMLSAuthConnector)
      DeclarationController.dataCacheConnector must be(DataCacheConnector)
      DeclarationController.statusService must be(StatusService)
    }

    "redirect to the declaration-persons page if name and/or business matching not found" in new Fixture {

      when(declarationController.dataCacheConnector.fetch[AddPerson](any())
        (any(), any(), any())).thenReturn(Future.successful(None))

      when(declarationController.statusService.getStatus(any(), any(), any())).thenReturn(Future.successful(NotCompleted))

      val result = declarationController.get()(request)
      status(result) must be(SEE_OTHER)

      redirectLocation(result) mustBe Some(routes.AddPersonController.get().url)
    }

    "load the declaration page for pre-submissions if name and business matching is found" in new Fixture {

      when(declarationController.dataCacheConnector.fetch[AddPerson](any())
        (any(), any(), any())).thenReturn(Future.successful(Some(addPerson)))

      val result = declarationController.get()(request)
      status(result) must be(OK)

      contentAsString(result) must include(addPerson.firstName)
      contentAsString(result) must include(addPerson.middleName mkString)
      contentAsString(result) must include(addPerson.lastName)
      contentAsString(result) must include(Messages("submit.registration"))
    }

    "report error if retrieval of amlsRegNo fails" in new Fixture {
      when(declarationController.dataCacheConnector.fetch[AddPerson](any())
        (any(), any(), any())).thenReturn(Future.successful(Some(addPerson)))

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

      when(declarationController.dataCacheConnector.fetch[AddPerson](any())
        (any(), any(), any())).thenReturn(Future.successful(Some(addPerson)))

      val result = declarationController.getWithAmendment()(request)
      status(result) must be(OK)

      contentAsString(result) must include(addPerson.firstName)
      contentAsString(result) must include(addPerson.middleName mkString)
      contentAsString(result) must include(addPerson.lastName)
      contentAsString(result) must include(Messages("submit.amendment.application"))
      contentAsString(result) must include(Messages("declaration.declaration.amendment.title"))
    }

    "redirect to the declaration-persons page if name and/or business matching not found" in new Fixture {

      when(declarationController.dataCacheConnector.fetch[AddPerson](any())
        (any(), any(), any())).thenReturn(Future.successful(None))

      when(declarationController.statusService.getStatus(any(), any(), any())).thenReturn(Future.successful(NotCompleted))

      val result = declarationController.getWithAmendment()(request)
      status(result) must be(SEE_OTHER)

      redirectLocation(result) mustBe Some(routes.AddPersonController.get().url)
    }

    "redirect to the declaration-persons for amendments page if name and/or business matching not found and submission is ready for review" in new Fixture {

      when(declarationController.dataCacheConnector.fetch[AddPerson](any())
        (any(), any(), any())).thenReturn(Future.successful(None))

      when(declarationController.statusService.getStatus(any(), any(), any())).thenReturn(Future.successful(SubmissionReadyForReview))

      val result = declarationController.getWithAmendment()(request)
      status(result) must be(SEE_OTHER)

      redirectLocation(result) mustBe Some(routes.AddPersonController.getWithAmendment().url)
    }
  }
}
