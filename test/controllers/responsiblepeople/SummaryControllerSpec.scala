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

package controllers.responsiblepeople

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import models.responsiblepeople._
import models.status.{SubmissionDecisionApproved, SubmissionReady, SubmissionReadyForReview}
import org.joda.time.LocalDate
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import utils.GenericTestHelper
import play.api.test.Helpers._
import services.StatusService
import utils.AuthorisedFixture

import scala.concurrent.Future

class SummaryControllerSpec extends GenericTestHelper with MockitoSugar {

  trait Fixture extends AuthorisedFixture { self =>
    val request = addToken(authRequest)

    val controller = new SummaryController {
      override val dataCacheConnector = mock[DataCacheConnector]
      override val authConnector = self.authConnector
      override val statusService = mock[StatusService]
    }
  }

  "Get" must {

    "use correct services" in new Fixture {
      SummaryController.authConnector must be(AMLSAuthConnector)
      SummaryController.dataCacheConnector must be(DataCacheConnector)
    }

    "load the summary page when section data is available" in new Fixture {

      val model = ResponsiblePeople(None, None)

      when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
        .thenReturn(Future.successful(Some(Seq(model))))

      val result = controller.get()(request)

      status(result) must be(OK)
    }

    "redirect to the main amls summary page when section data is unavailable" in new Fixture {
      when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
        .thenReturn(Future.successful(None))

      val result = controller.get()(request)
      redirectLocation(result) must be(Some(controllers.routes.RegistrationProgressController.get.url))
      status(result) must be(SEE_OTHER)
    }
  }

  "Post" must {

    "redirect to 'registration progress page'" when {
      "'fromDeclaration flat set to false'" in new Fixture {
        val result = controller.post(false)(request)
        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some(controllers.routes.RegistrationProgressController.get.url))
      }
    }

    "redirect to 'Who is the business’s nominated officer?'" when {
      "'fromDeclaration flat set to true and status is pending'" in new Fixture {
        val positions = Positions(Set(BeneficialOwner, InternalAccountant), Some(new LocalDate()))
        val rp1 = ResponsiblePeople(Some(PersonName("first", Some("middle"), "last", None, None)), None, None, None, None, None, None, Some(positions))
        val rp2 = ResponsiblePeople(Some(PersonName("first2", None, "middle2", None, None)), None, None, None, None, None, None, Some(positions))
        val responsiblePeople = Seq(rp1, rp2)

        when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
          .thenReturn(Future.successful(Some(responsiblePeople)))

        when(controller.statusService.getStatus(any(), any(), any()))
          .thenReturn(Future.successful(SubmissionReady))

        val result = controller.post(true)(request)
        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some(controllers.declaration.routes.WhoIsTheBusinessNominatedOfficerController.get.url))
      }
    }

    "redirect to 'Who is the business’s nominated officer?'" when {
      "'fromDeclaration flat set to true and status is SubmissionDecisionApproved'" in new Fixture {
        val positions = Positions(Set(BeneficialOwner, InternalAccountant), Some(new LocalDate()))
        val rp1 = ResponsiblePeople(Some(PersonName("first", Some("middle"), "last", None, None)), None,None,None, None, None, None, Some(positions))
        val rp2 = ResponsiblePeople(Some(PersonName("first2", None, "middle2", None, None)), None, None, None, None, None, None, Some(positions))
        val responsiblePeople = Seq(rp1, rp2)

        when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
          .thenReturn(Future.successful(Some(responsiblePeople)))

        when(controller.statusService.getStatus(any(), any(), any()))
          .thenReturn(Future.successful(SubmissionDecisionApproved))

        val result = controller.post(true)(request)
        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some(controllers.declaration.routes.WhoIsTheBusinessNominatedOfficerController.getWithAmendment().url))
      }
    }


    "redirect to 'Fee Guidance'" when {
      "'fromDeclaration flag is true and status is pre amendment'" in new Fixture {
        val positions = Positions(Set(BeneficialOwner, InternalAccountant, NominatedOfficer), Some(new LocalDate()))
        val rp1 = ResponsiblePeople(Some(PersonName("first", Some("middle"), "last", None, None)), None, None, None, None, None, None, Some(positions))
        val rp2 = ResponsiblePeople(Some(PersonName("first2", None, "middle2", None, None)), None, None, None, None, None, None, Some(positions))
        val responsiblePeople = Seq(rp1, rp2)

        when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
          .thenReturn(Future.successful(Some(responsiblePeople)))

        when(controller.statusService.getStatus(any(), any(), any()))
          .thenReturn(Future.successful(SubmissionReady))

        val result = controller.post(true)(request)

        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some(controllers.routes.FeeGuidanceController.get.url))
      }
    }

    "redirect to 'Who is registering this business?'" when {
      "'fromDeclaration flat set to true and status is SubmissionDecisionApproved'" in new Fixture {
        val positions = Positions(Set(BeneficialOwner, InternalAccountant, NominatedOfficer), Some(new LocalDate()))
        val rp1 = ResponsiblePeople(Some(PersonName("first", Some("middle"), "last", None, None)), None, None, None, None, None, None, Some(positions))
        val rp2 = ResponsiblePeople(Some(PersonName("first2", None, "middle2", None, None)), None, None, None, None, None, None, Some(positions))
        val responsiblePeople = Seq(rp1, rp2)

        when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
          .thenReturn(Future.successful(Some(responsiblePeople)))

        when(controller.statusService.getStatus(any(), any(), any()))
          .thenReturn(Future.successful(SubmissionDecisionApproved))

        val result = controller.post(true)(request)
        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some(controllers.declaration.routes.WhoIsRegisteringController.getWithAmendment().url))
      }
      "'fromDeclaration is true and status is amendment'" in new Fixture {
        val positions = Positions(Set(BeneficialOwner, InternalAccountant, NominatedOfficer), Some(new LocalDate()))
        val rp1 = ResponsiblePeople(Some(PersonName("first", Some("middle"), "last", None, None)), None, None, None, None, None, None, Some(positions))
        val rp2 = ResponsiblePeople(Some(PersonName("first2", None, "middle2", None, None)), None, None,None, None, None, None, Some(positions))
        val responsiblePeople = Seq(rp1, rp2)

        when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
          .thenReturn(Future.successful(Some(responsiblePeople)))

        when(controller.statusService.getStatus(any(), any(), any()))
          .thenReturn(Future.successful(SubmissionReadyForReview))

        val result = controller.post(true)(request)

        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some(controllers.declaration.routes.WhoIsRegisteringController.get().url))
      }
    }
  }
}
