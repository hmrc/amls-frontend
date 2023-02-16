/*
 * Copyright 2023 HM Revenue & Customs
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

import connectors.DataCacheConnector
import controllers.actions.SuccessfulAuthAction
import models.registrationprogress.{Completed, Section, Started}
import models.responsiblepeople._
import models.status._
import org.joda.time.LocalDate
import org.mockito.Matchers.{eq => meq, _}
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import play.api.i18n.Messages
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.Call
import play.api.test.Helpers._
import services.{ProgressService, SectionsProvider, StatusService}
import uk.gov.hmrc.http.cache.client.CacheMap
import utils._

import scala.concurrent.Future


class RegisterPartnersControllerSpec extends AmlsSpec with MockitoSugar {

  trait Fixture extends DependencyMocks {
    self =>
    val request = addToken(authRequest)
    val dataCacheConnector = mock[DataCacheConnector]
    val statusService = mockStatusService
    val progressService = mock[ProgressService]
    val mockSectionsProvider = mock[SectionsProvider]

    lazy val app = new GuiceApplicationBuilder()
      .disable[com.kenshoo.play.metrics.PlayModule]
      .overrides(bind[AuthAction].to(SuccessfulAuthAction))
      .overrides(bind[DataCacheConnector].to(dataCacheConnector))
      .overrides(bind[StatusService].to(statusService))
      .overrides(bind[ProgressService].to(progressService))
      .overrides(bind[SectionsProvider].to(mockSectionsProvider))
      .build()

    val controller = app.injector.instanceOf[RegisterPartnersController]

    val emptyCache = CacheMap("", Map.empty)

    val personName = PersonName("firstName", Some("middleName"), "lastName")
    val personName1 = PersonName("firstName1", Some("middleName1"), "lastName1")
    val positions = Positions(Set(BeneficialOwner, InternalAccountant), Some(PositionStartDate(new LocalDate())))
    val positions1 = Positions(Set(BeneficialOwner, InternalAccountant, Partner), Some(PositionStartDate(new LocalDate())))
    val rp = ResponsiblePerson (
      personName = Some(personName),
      positions = Some(positions),
      status = None
    )
    val rp2 = ResponsiblePerson (
      personName = Some(personName1),
      positions = Some(positions1),
      status = None
    )
    val rp1 = ResponsiblePerson(
      personName = Some(personName),
      positions = Some(positions),
      status = Some(StatusConstants.Deleted)
    )
    val responsiblePeoples = Seq(rp, rp1, rp2)
  }

  "The RegisterPartnersController" when {
    "get is called" must {
      "with completed sections" must {
        val completedSections = Seq(
          Section("s1", Completed, true, mock[Call]),
          Section("s2", Completed, true, mock[Call])
        )
        "respond with OK" in new Fixture {
          when {
            mockSectionsProvider.sections(any[String])(any(), any())
          }.thenReturn(Future.successful(completedSections))

          when {
            dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any(), any())
          } thenReturn Future.successful(Some(Seq(ResponsiblePerson())))

          mockApplicationStatus(SubmissionDecisionApproved)

          val result = controller.get()(request)

          status(result) mustBe OK

        }
      }

      "with incomplete sections" must {
        val incompleteSections = Seq(
          Section("s1", Completed, true, mock[Call]),
          Section("s2", Started, true, mock[Call])
        )

        "redirect to the RegistrationProgressController" in new Fixture {
          when {
            mockSectionsProvider.sections(any[String])(any(), any())
          }.thenReturn(Future.successful(incompleteSections))

          when {
            dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any(), any())
          } thenReturn Future.successful(Some(Seq(ResponsiblePerson())))

          mockApplicationStatus(SubmissionDecisionApproved)

          val result = controller.get()(request)

          redirectLocation(result) mustBe Some(controllers.routes.RegistrationProgressController.get.url)
        }
      }
    }

    "post is called" must {
      "pass validation" when {
        "selected option is a valid responsible person" in new Fixture {

          val newRequest = requestWithUrlEncodedBody("value" -> "firstNamemiddleNamelastName")

          when(controller.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any(), any()))
            .thenReturn(Future.successful(Some(responsiblePeoples)))

          mockApplicationStatus(SubmissionReady)

          val updatedList = Seq(rp.copy(positions = Some(positions.copy(positions
            = Set(BeneficialOwner, InternalAccountant, Partner)))), rp2)

          when(controller.dataCacheConnector.save[Option[Seq[ResponsiblePerson]]](any(), any(), meq(Some(updatedList)))(any(), any()))
            .thenReturn(Future.successful(emptyCache))

          when(controller.progressService.getSubmitRedirect(Some(any()), any(), any())(any(), any()))
            .thenReturn(Future.successful(Some(controllers.declaration.routes.WhoIsRegisteringController.get)))

          val result = controller.post()(newRequest)
          status(result) must be(SEE_OTHER)

        }
      }

      "fail validation" when {
        "no option is selected on the UI and status is submissionready" in new Fixture {
          val newRequest = requestWithUrlEncodedBody("" -> "")
          when(controller.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any(), any()))
            .thenReturn(Future.successful(Some(responsiblePeoples)))

          mockApplicationStatus(SubmissionReady)

          val result = controller.post()(newRequest)
          status(result) must be(BAD_REQUEST)
          contentAsString(result) must include(Messages("error.required.declaration.partners"))
          contentAsString(result) must include(Messages("submit.registration"))
        }

        "no option is selected on the UI and status is SubmissionReadyForReview" in new Fixture {
          val newRequest = requestWithUrlEncodedBody("" -> "")
          when(controller.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any(), any()))
            .thenReturn(Future.successful(Some(responsiblePeoples)))

          mockApplicationStatus(SubmissionReadyForReview)

          val result = controller.post()(newRequest)
          status(result) must be(BAD_REQUEST)
          contentAsString(result) must include(Messages("error.required.declaration.partners"))
          contentAsString(result) must include(Messages("submit.amendment.application"))
        }

        "no option is selected on the UI and status is ReadyForRenewal" in new Fixture {
          val newRequest = requestWithUrlEncodedBody("" -> "")
          when(controller.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any(), any()))
            .thenReturn(Future.successful(Some(responsiblePeoples)))

          mockApplicationStatus(ReadyForRenewal(Some(new LocalDate())))

          val result = controller.post()(newRequest)
          status(result) must be(BAD_REQUEST)
          contentAsString(result) must include(Messages("error.required.declaration.partners"))
          contentAsString(result) must include(Messages("submit.renewal.application"))
        }

        "no option is selected on the UI and no responsible people returned" in new Fixture {
          val newRequest = requestWithUrlEncodedBody("" -> "")
          when(controller.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any(), any()))
            .thenReturn(Future.successful(None))

          mockApplicationStatus(SubmissionReadyForReview)

          val result = controller.post()(newRequest)
          status(result) must be(BAD_REQUEST)
          contentAsString(result) must include(Messages("error.required.declaration.partners"))
        }
      }
    }
  }
}
