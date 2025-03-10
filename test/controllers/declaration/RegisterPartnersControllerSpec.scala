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

package controllers.declaration

import connectors.DataCacheConnector
import controllers.actions.SuccessfulAuthAction
import forms.declaration.BusinessPartnersFormProvider
import models.registrationprogress.{Completed, Started, TaskRow}
import models.responsiblepeople._
import models.status._
import org.mockito.ArgumentMatchers.{eq => meq, _}
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Injecting}
import services.{ProgressService, RenewalService, SectionsProvider}
import services.cache.Cache
import utils._
import views.html.declaration.RegisterPartnersView

import java.time.LocalDate
import scala.concurrent.Future

class RegisterPartnersControllerSpec extends AmlsSpec with MockitoSugar with Injecting {

  trait Fixture extends DependencyMocks {
    self =>
    val request              = addToken(authRequest)
    val dataCacheConnector   = mock[DataCacheConnector]
    val statusService        = mockStatusService
    val progressService      = mock[ProgressService]
    val mockSectionsProvider = mock[SectionsProvider]
    val renewalService       = mock[RenewalService]
    when(renewalService.isRenewalFlow(any(), any(), any())(any(), any())).thenReturn(Future.successful(false))

    val controller = new RegisterPartnersController(
      authAction = SuccessfulAuthAction,
      ds = commonDependencies,
      dataCacheConnector = dataCacheConnector,
      statusService = statusService,
      progressService = progressService,
      cc = mockMcc,
      sectionsProvider = mockSectionsProvider,
      formProvider = inject[BusinessPartnersFormProvider],
      renewalService = renewalService,
      view = inject[RegisterPartnersView]
    )

    val emptyCache = Cache.empty

    val personName         = PersonName("firstName", Some("middleName"), "lastName")
    val personName1        = PersonName("firstName1", Some("middleName1"), "lastName1")
    val positions          = Positions(Set(BeneficialOwner, InternalAccountant), Some(PositionStartDate(LocalDate.now())))
    val positions1         =
      Positions(Set(BeneficialOwner, InternalAccountant, Partner), Some(PositionStartDate(LocalDate.now())))
    val rp                 = ResponsiblePerson(
      personName = Some(personName),
      positions = Some(positions),
      status = None
    )
    val rp2                = ResponsiblePerson(
      personName = Some(personName1),
      positions = Some(positions1),
      status = None
    )
    val rp1                = ResponsiblePerson(
      personName = Some(personName),
      positions = Some(positions),
      status = Some(StatusConstants.Deleted)
    )
    val responsiblePeoples = Seq(rp, rp1, rp2)
  }

  "The RegisterPartnersController" when {
    "get is called" must {
      "with completed sections" must {
        val taskRows = Seq(
          TaskRow("s1", "/foo", true, Completed, TaskRow.completedTag),
          TaskRow("s2", "/bar", true, Completed, TaskRow.completedTag)
        )
        "respond with OK" in new Fixture {
          when {
            mockSectionsProvider.taskRows(any[String])(any(), any())
          }.thenReturn(Future.successful(taskRows))

          when {
            dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any())
          } thenReturn Future.successful(Some(Seq(ResponsiblePerson())))

          mockApplicationStatus(SubmissionDecisionApproved)

          val result = controller.get()(request)

          status(result) mustBe OK

        }
      }

      "with incomplete sections" must {
        val taskRows = Seq(
          TaskRow("s1", "/foo", true, Completed, TaskRow.completedTag),
          TaskRow("s2", "/bar", true, Started, TaskRow.incompleteTag)
        )

        "redirect to the RegistrationProgressController" in new Fixture {
          when {
            mockSectionsProvider.taskRows(any[String])(any(), any())
          }.thenReturn(Future.successful(taskRows))

          when {
            dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any())
          } thenReturn Future.successful(Some(Seq(ResponsiblePerson())))

          mockApplicationStatus(SubmissionDecisionApproved)

          val result = controller.get()(request)

          redirectLocation(result) mustBe Some(controllers.routes.RegistrationProgressController.get().url)
        }
      }
    }

    "post is called" must {
      "pass validation" when {
        "selected option is a valid responsible person" in new Fixture {

          val newRequest = FakeRequest(POST, routes.RegisterPartnersController.post().url)
            .withFormUrlEncodedBody("value" -> "firstNamemiddleNamelastName")

          when(controller.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any()))
            .thenReturn(Future.successful(Some(responsiblePeoples)))

          mockApplicationStatus(SubmissionReady)

          val updatedList = Seq(
            rp.copy(positions = Some(positions.copy(positions = Set(BeneficialOwner, InternalAccountant, Partner)))),
            rp2
          )

          when(
            controller.dataCacheConnector.save[Option[Seq[ResponsiblePerson]]](any(), any(), meq(Some(updatedList)))(
              any()
            )
          )
            .thenReturn(Future.successful(emptyCache))

          when(controller.progressService.getSubmitRedirect(Some(any()), any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(controllers.declaration.routes.WhoIsRegisteringController.get)))

          val result = controller.post()(newRequest)
          status(result) must be(SEE_OTHER)
        }
      }

      "fail validation" when {
        "no option is selected on the UI and status is submissionready" in new Fixture {
          val newRequest = FakeRequest(POST, routes.RegisterPartnersController.post().url)
            .withFormUrlEncodedBody("" -> "")
          when(controller.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any()))
            .thenReturn(Future.successful(Some(responsiblePeoples)))

          mockApplicationStatus(SubmissionReady)

          val result = controller.post()(newRequest)
          status(result)          must be(BAD_REQUEST)
          contentAsString(result) must include(messages("error.required.declaration.partners"))
          contentAsString(result) must include(messages("submit.registration"))
        }

        "no option is selected on the UI and status is SubmissionReadyForReview" in new Fixture {
          val newRequest = FakeRequest(POST, routes.RegisterPartnersController.post().url)
            .withFormUrlEncodedBody("" -> "")
          when(controller.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any()))
            .thenReturn(Future.successful(Some(responsiblePeoples)))

          mockApplicationStatus(SubmissionReadyForReview)

          val result = controller.post()(newRequest)
          status(result)          must be(BAD_REQUEST)
          contentAsString(result) must include(messages("error.required.declaration.partners"))
          contentAsString(result) must include(messages("submit.amendment.application"))
        }

        "no option is selected on the UI and status is ReadyForRenewal" in new Fixture {
          val newRequest = FakeRequest(POST, routes.RegisterPartnersController.post().url)
            .withFormUrlEncodedBody("" -> "")
          when(controller.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any()))
            .thenReturn(Future.successful(Some(responsiblePeoples)))

          mockApplicationStatus(ReadyForRenewal(Some(LocalDate.now())))

          val result = controller.post()(newRequest)
          status(result)          must be(BAD_REQUEST)
          contentAsString(result) must include(messages("error.required.declaration.partners"))
          contentAsString(result) must include(messages("submit.renewal.application"))
        }

        "no option is selected on the UI and no responsible people returned" in new Fixture {
          val newRequest = FakeRequest(POST, routes.RegisterPartnersController.post().url)
            .withFormUrlEncodedBody("" -> "")
          when(controller.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any()))
            .thenReturn(Future.successful(None))

          mockApplicationStatus(SubmissionReadyForReview)

          val result = controller.post()(newRequest)
          status(result)          must be(BAD_REQUEST)
          contentAsString(result) must include(messages("error.required.declaration.partners"))
        }
      }
    }
  }
}
