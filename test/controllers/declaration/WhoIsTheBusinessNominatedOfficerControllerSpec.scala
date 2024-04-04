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

import controllers.actions.SuccessfulAuthAction
import forms.declaration.BusinessNominatedOfficerFormProvider
import models.declaration.BusinessNominatedOfficer
import models.registrationprogress.{Completed, Started, TaskRow}
import models.responsiblepeople.ResponsiblePerson.flowFromDeclaration
import models.responsiblepeople._
import models.status.{ReadyForRenewal, SubmissionDecisionApproved, SubmissionReady, SubmissionReadyForReview}
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Injecting}
import services.SectionsProvider
import utils._
import views.html.declaration.SelectBusinessNominatedOfficerView

import java.time.LocalDate
import scala.concurrent.Future

class WhoIsTheBusinessNominatedOfficerControllerSpec extends AmlsSpec with MockitoSugar with Injecting {

  trait Fixture extends DependencyMocks { self =>

    val request = addToken(authRequest)
    val mockSectionsProvider = mock[SectionsProvider]

    lazy val controller = new WhoIsTheBusinessNominatedOfficerController(
      mockCacheConnector,
      SuccessfulAuthAction,
      commonDependencies,
      mockStatusService,
      mockMcc,
      inject[BusinessNominatedOfficerFormProvider],
      mockSectionsProvider,
      inject[SelectBusinessNominatedOfficerView]
    )

  }

  "WhoIsTheBusinessNominatedOfficerController" must {

    val personName = PersonName("firstName", Some("middleName"), "lastName")
    val personName1 = PersonName("firstName1", Some("middleName1"), "lastName1")
    val positions = Positions(Set(BeneficialOwner, InternalAccountant), Some(PositionStartDate(LocalDate.now())))
    val rp = ResponsiblePerson (
      personName = Some(personName),
      positions = Some(positions),
      status = None
    )
    val rp2 = ResponsiblePerson (
      personName = Some(personName1),
      positions = Some(positions),
      status = None
    )
    val rp1 = ResponsiblePerson(
      personName = Some(personName),
      positions = Some(positions),
      status = Some(StatusConstants.Deleted)
    )
    val responsiblePeoples = Seq(rp, rp1, rp2)

    "load 'Who is the business’s nominated officer?' page successfully" when {
      "with completed sections" must {
        val completedSections = Seq(
          TaskRow("s1", "/foo", true, Completed, TaskRow.completedTag),
          TaskRow("s2", "/bar", true, Completed, TaskRow.completedTag)
        )

        "status is pre-submission" in new Fixture {
          when {
            mockSectionsProvider.taskRows(any[String])(any(), any(), any())
          }.thenReturn(Future.successful(completedSections))

          mockApplicationStatus(SubmissionReady)
          mockCacheGetEntry[Seq[ResponsiblePerson]](Some(responsiblePeoples), ResponsiblePerson.key)
          mockCacheGetEntry[BusinessNominatedOfficer](None, BusinessNominatedOfficer.key)

          val result = controller.get()(request)
          status(result) must be(OK)

          contentAsString(result) must include(messages("submit.registration"))
        }

        "status is pending" in new Fixture {
          when {
            mockSectionsProvider.taskRows(any[String])(any(), any(), any())
          }.thenReturn(Future.successful(completedSections))

          mockApplicationStatus(SubmissionReadyForReview)
          mockCacheGetEntry[Seq[ResponsiblePerson]](Some(responsiblePeoples), ResponsiblePerson.key)
          mockCacheGetEntry[BusinessNominatedOfficer](None, BusinessNominatedOfficer.key)

          val result = controller.get()(request)
          status(result) must be(OK)

          contentAsString(result) must include(messages("submit.amendment.application"))
        }

        "status is approved" in new Fixture {
          when {
            mockSectionsProvider.taskRows(any[String])(any(), any(), any())
          }.thenReturn(Future.successful(completedSections))

          mockApplicationStatus(SubmissionDecisionApproved)
          mockCacheGetEntry[Seq[ResponsiblePerson]](Some(responsiblePeoples), ResponsiblePerson.key)
          mockCacheGetEntry[BusinessNominatedOfficer](None, BusinessNominatedOfficer.key)

          val result = controller.get()(request)
          status(result) must be(OK)

          contentAsString(result) must include(messages("submit.amendment.application"))
        }

        "status is ready for renewal" in new Fixture {
          when {
            mockSectionsProvider.taskRows(any[String])(any(), any(), any())
          }.thenReturn(Future.successful(completedSections))

          mockApplicationStatus(ReadyForRenewal(Some(LocalDate.now())))
          mockCacheGetEntry[Seq[ResponsiblePerson]](Some(responsiblePeoples), ResponsiblePerson.key)
          mockCacheGetEntry[BusinessNominatedOfficer](None, BusinessNominatedOfficer.key)

          val result = controller.get()(request)
          status(result) must be(OK)

          contentAsString(result) must include(messages("submit.renewal.application"))
        }
      }

      "with incomplete sections" must {
        val incompleteSections = Seq(
          TaskRow("s1", "/foo", true, Completed, TaskRow.completedTag),
          TaskRow("s2", "/bar", true, Started, TaskRow.incompleteTag)
        )

        "redirect to the RegistrationProgressController" in new Fixture {
          when {
            mockSectionsProvider.taskRows(any[String])(any(), any(), any())
          }.thenReturn(Future.successful(incompleteSections))

          mockApplicationStatus(SubmissionReady)
          mockCacheGetEntry[Seq[ResponsiblePerson]](Some(responsiblePeoples), ResponsiblePerson.key)
          mockCacheGetEntry[BusinessNominatedOfficer](None, BusinessNominatedOfficer.key)

          val result = controller.get()(request)
          status(result) must be(SEE_OTHER)

          redirectLocation(result) mustBe Some(controllers.routes.RegistrationProgressController.get.url)
        }
      }
    }

    "redirect to 'Who is registering this business?' page" when {

      "post submission" when {

        "selected option is a valid responsible person in amendment mode" in new Fixture {

          val newRequest = FakeRequest(POST, routes.WhoIsTheBusinessNominatedOfficerController.post().url)
          .withFormUrlEncodedBody("value" -> "firstNamemiddleNamelastName")

          val updatedList = Seq(rp.copy(
            positions = Some(positions.copy(positions = Set(BeneficialOwner, InternalAccountant, NominatedOfficer)))
          ), rp2)

          mockCacheFetch[Seq[ResponsiblePerson]](Some(responsiblePeoples), Some(ResponsiblePerson.key))
          mockApplicationStatus(SubmissionDecisionApproved)
          mockCacheSave[Option[Seq[ResponsiblePerson]]](Some(updatedList))

          val result = controller.post()(newRequest)
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(routes.WhoIsRegisteringController.get.url))
        }

        "selected option is a valid responsible person" in new Fixture {

          val newRequest = FakeRequest(POST, routes.WhoIsTheBusinessNominatedOfficerController.post().url)
          .withFormUrlEncodedBody("value" -> "firstNamemiddleNamelastName")

          val updatedList = Seq(rp.copy(
            positions = Some(positions.copy(positions = Set(BeneficialOwner, InternalAccountant, NominatedOfficer)))
          ), rp2)

          mockCacheFetch[Seq[ResponsiblePerson]](Some(responsiblePeoples), Some(ResponsiblePerson.key))
          mockApplicationStatus(SubmissionReadyForReview)
          mockCacheSave[Option[Seq[ResponsiblePerson]]](Some(updatedList))

          val result = controller.post()(newRequest)
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(controllers.declaration.routes.WhoIsRegisteringController.get.url))
        }

      }
    }

    "successfully redirect to adding new responsible people .i.e what you need page of RP" when {
      "selected option is 'Register someone else'" in new Fixture {
        val newRequest = FakeRequest(POST, routes.WhoIsTheBusinessNominatedOfficerController.post().url)
        .withFormUrlEncodedBody("value" -> "-1")

        mockCacheGetEntry[Seq[ResponsiblePerson]](Some(responsiblePeoples), ResponsiblePerson.key)
        mockApplicationStatus(SubmissionReady)

        val result = controller.post()(newRequest)
        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some(controllers.responsiblepeople.routes.ResponsiblePeopleAddController.get(true, Some(flowFromDeclaration)).url))
      }
    }

    "fail validation" when {
      "no option is selected on the UI" in new Fixture {
        val newRequest = FakeRequest(POST, routes.WhoIsTheBusinessNominatedOfficerController.post().url)
        .withFormUrlEncodedBody("" -> "")

        mockCacheFetch[Seq[ResponsiblePerson]](Some(responsiblePeoples), Some(ResponsiblePerson.key))
        mockApplicationStatus(SubmissionReady)

        val result = controller.post()(newRequest)
        status(result) must be(BAD_REQUEST)
        contentAsString(result) must include(messages("error.required.declaration.nominated.officer"))
      }
    }
  }

}

