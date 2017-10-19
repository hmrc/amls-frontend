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

import connectors.{AmlsConnector, DataCacheConnector}
import models.declaration.BusinessNominatedOfficer
import models.responsiblepeople._
import models.status.{ReadyForRenewal, SubmissionDecisionApproved, SubmissionReady, SubmissionReadyForReview}
import org.joda.time.LocalDate
import org.mockito.Matchers.{eq => meq, _}
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import utils.GenericTestHelper
import play.api.i18n.Messages
import play.api.test.Helpers._
import services.StatusService
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.frontend.auth.AuthContext
import utils.{AuthorisedFixture, StatusConstants}

import scala.concurrent.Future
import models.responsiblepeople.ResponsiblePeople.flowFromDeclaration
import uk.gov.hmrc.http.HeaderCarrier

class WhoIsTheBusinessNominatedOfficerControllerSpec extends GenericTestHelper with MockitoSugar {

  trait Fixture extends AuthorisedFixture {
    self => val request = addToken(authRequest)
    val controller = new  WhoIsTheBusinessNominatedOfficerController {
      override val dataCacheConnector = mock[DataCacheConnector]
      override val authConnector = self.authConnector
      override val amlsConnector = mock[AmlsConnector]
      override val statusService: StatusService = mock[StatusService]
    }
  }

  val emptyCache = CacheMap("", Map.empty)

  "WhoIsTheBusinessNominatedOfficerController" must {

    val personName = PersonName("firstName", Some("middleName"), "lastName", None, Some("name"))
    val personName1 = PersonName("firstName1", Some("middleName1"), "lastName1", None, Some("random"))
    val positions = Positions(Set(BeneficialOwner, InternalAccountant), Some(new LocalDate()))
    val rp = ResponsiblePeople (
      personName = Some(personName),
      positions = Some(positions),
      status = None
    )
    val rp2 = ResponsiblePeople (
      personName = Some(personName1),
      positions = Some(positions),
      status = None
    )
    val rp1 = ResponsiblePeople(
      personName = Some(personName),
      positions = Some(positions),
      status = Some(StatusConstants.Deleted)
    )
    val responsiblePeoples = Seq(rp, rp1, rp2)

    val mockCacheMap = mock[CacheMap]
      "load 'Who is the business’s nominated officer?' page successfully" when {

        "status is pre-submission" in new Fixture {

          when(controller.dataCacheConnector.fetchAll(any[HeaderCarrier], any[AuthContext]))
            .thenReturn(Future.successful(Some(mockCacheMap)))

          when(controller.statusService.getStatus(any(),any(),any()))
            .thenReturn(Future.successful(SubmissionReady))

          when(mockCacheMap.getEntry[Seq[ResponsiblePeople]](any())(any()))
            .thenReturn(Some(responsiblePeoples))

          when(mockCacheMap.getEntry[BusinessNominatedOfficer](BusinessNominatedOfficer.key))
            .thenReturn(None)

          val result = controller.get()(request)
          status(result) must be(OK)

          contentAsString(result) must include(Messages("submit.registration"))
        }

        "status is pending" in new Fixture {

          when(controller.dataCacheConnector.fetchAll(any[HeaderCarrier], any[AuthContext]))
            .thenReturn(Future.successful(Some(mockCacheMap)))

          when(controller.statusService.getStatus(any(),any(),any()))
            .thenReturn(Future.successful(SubmissionReadyForReview))

          when(mockCacheMap.getEntry[Seq[ResponsiblePeople]](any())(any()))
            .thenReturn(Some(responsiblePeoples))

          when(mockCacheMap.getEntry[BusinessNominatedOfficer](BusinessNominatedOfficer.key))
            .thenReturn(None)

          val result = controller.get()(request)
          status(result) must be(OK)

          contentAsString(result) must include(Messages("submit.amendment.application"))
        }

        "status is approved" in new Fixture {

          when(controller.dataCacheConnector.fetchAll(any[HeaderCarrier], any[AuthContext]))
            .thenReturn(Future.successful(Some(mockCacheMap)))

          when(controller.statusService.getStatus(any(),any(),any()))
            .thenReturn(Future.successful(SubmissionDecisionApproved))

          when(mockCacheMap.getEntry[Seq[ResponsiblePeople]](any())(any()))
            .thenReturn(Some(responsiblePeoples))

          when(mockCacheMap.getEntry[BusinessNominatedOfficer](BusinessNominatedOfficer.key))
            .thenReturn(None)

          val result = controller.get()(request)
          status(result) must be(OK)

          contentAsString(result) must include(Messages("submit.amendment.application"))
        }

        "status is ready for renewal" in new Fixture {

          when(controller.dataCacheConnector.fetchAll(any[HeaderCarrier], any[AuthContext]))
            .thenReturn(Future.successful(Some(mockCacheMap)))

          when(controller.statusService.getStatus(any(),any(),any()))
            .thenReturn(Future.successful(ReadyForRenewal(Some(new LocalDate()))))

          when(mockCacheMap.getEntry[Seq[ResponsiblePeople]](any())(any()))
            .thenReturn(Some(responsiblePeoples))

          when(mockCacheMap.getEntry[BusinessNominatedOfficer](BusinessNominatedOfficer.key))
            .thenReturn(None)

          val result = controller.get()(request)
          status(result) must be(OK)

          contentAsString(result) must include(Messages("submit.renewal.application"))
        }
    }

    "redirect to Fee Guidance" when {

      "selected option is a valid responsible person" in new Fixture {

        val newRequest = request.withFormUrlEncodedBody("value" -> "firstNamemiddleNamelastName")

        when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
          .thenReturn(Future.successful(Some(responsiblePeoples)))

        when(controller.statusService.getStatus(any(),any(),any()))
          .thenReturn(Future.successful(SubmissionReady))

        val updatedList = Seq(rp.copy(positions = Some(positions.copy(positions
          = Set(BeneficialOwner, InternalAccountant, NominatedOfficer)))), rp2)

        when(controller.dataCacheConnector.save[Option[Seq[ResponsiblePeople]]](any(), meq(Some(updatedList)))(any(), any(), any()))
          .thenReturn(Future.successful(emptyCache))

        val result = controller.post()(newRequest)
        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some(controllers.routes.FeeGuidanceController.get().url))
      }

    }

    "redirect to 'Who is registering this business?' page" when {

      "making an amendment" when {

        "selected option is a valid responsible person in amendment mode" in new Fixture {

          val newRequest = request.withFormUrlEncodedBody("value" -> "firstNamemiddleNamelastName")

          when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())
            (any(), any(), any())).thenReturn(Future.successful(Some(responsiblePeoples)))

          when(controller.statusService.getStatus(any(), any(), any()))
            .thenReturn(Future.successful(SubmissionDecisionApproved))

          val updatedList = Seq(rp.copy(positions = Some(positions.copy(positions
            = Set(BeneficialOwner, InternalAccountant, NominatedOfficer)))), rp2)

          when(controller.dataCacheConnector.save[Option[Seq[ResponsiblePeople]]](any(), meq(Some(updatedList)))
            (any(), any(), any())).thenReturn(Future.successful(emptyCache))

          val result = controller.post()(newRequest)
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(routes.WhoIsRegisteringController.get().url))
        }

        "selected option is a valid responsible person" in new Fixture {

          val newRequest = request.withFormUrlEncodedBody("value" -> "firstNamemiddleNamelastName")

          val updatedList = Seq(rp.copy(positions = Some(positions.copy(positions
            = Set(BeneficialOwner, InternalAccountant, NominatedOfficer)))), rp2)

          when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(responsiblePeoples)))

          when(controller.statusService.getStatus(any(),any(),any()))
            .thenReturn(Future.successful(SubmissionReadyForReview))

          when(controller.dataCacheConnector.save[Option[Seq[ResponsiblePeople]]](any(), meq(Some(updatedList)))(any(), any(), any()))
            .thenReturn(Future.successful(emptyCache))

          val result = controller.post()(newRequest)
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(controllers.declaration.routes.WhoIsRegisteringController.get().url))
        }

      }
    }

    "successfully redirect to adding new responsible people .i.e what you need page of RP" when {
      "selected option is 'Register someone else'" in new Fixture {
        val newRequest = request.withFormUrlEncodedBody("value" -> "-1")

        when(controller.statusService.getStatus(any(),any(),any()))
          .thenReturn(Future.successful(SubmissionReady))

        when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())
          (any(), any(), any())).thenReturn(Future.successful(Some(responsiblePeoples)))

        val result = controller.post()(newRequest)
        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some(controllers.responsiblepeople.routes.ResponsiblePeopleAddController.get(true, Some(flowFromDeclaration)).url))
      }
    }

    "fail validation" when {
      "no option is selected on the UI" in new Fixture {
        val newRequest = request.withFormUrlEncodedBody()
        when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
          .thenReturn(Future.successful(Some(responsiblePeoples)))

        when(controller.statusService.getStatus(any(),any(),any()))
          .thenReturn(Future.successful(SubmissionReady))

        when(controller.statusService.getStatus(any(),any(),any()))
          .thenReturn(Future.successful(SubmissionReady))

        val result = controller.post()(newRequest)
        status(result) must be(BAD_REQUEST)
        contentAsString(result) must include(Messages("error.required.declaration.nominated.officer"))
      }
    }
  }

}

