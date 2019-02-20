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

import connectors.{AmlsConnector, DataCacheConnector, KeystoreConnector}
import models.declaration.BusinessNominatedOfficer
import models.responsiblepeople.ResponsiblePerson.flowFromDeclaration
import models.responsiblepeople._
import models.status.{ReadyForRenewal, SubmissionDecisionApproved, SubmissionReady, SubmissionReadyForReview}
import org.joda.time.LocalDate
import org.scalatest.mock.MockitoSugar
import play.api.i18n.Messages
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers._
import services.StatusService
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.{AmlsSpec, AuthorisedFixture, DependencyMocks, StatusConstants}

class WhoIsTheBusinessNominatedOfficerControllerSpec extends AmlsSpec with MockitoSugar {

  trait Fixture extends AuthorisedFixture with DependencyMocks { self =>

    val request = addToken(authRequest)

    lazy val defaultBuilder = new GuiceApplicationBuilder()
      .configure("microservice.services.feature-toggle.show-fees" -> true)
      .disable[com.kenshoo.play.metrics.PlayModule]
      .overrides(bind[AuthConnector].to(self.authConnector))
      .overrides(bind[AmlsConnector].to(mock[AmlsConnector]))
      .overrides(bind[DataCacheConnector].to(mockCacheConnector))
      .overrides(bind[StatusService].to(mockStatusService))

    val builder = defaultBuilder
    lazy val app = builder.build()
    lazy val controller = app.injector.instanceOf[WhoIsTheBusinessNominatedOfficerController]

  }

  "WhoIsTheBusinessNominatedOfficerController" must {

    val personName = PersonName("firstName", Some("middleName"), "lastName")
    val personName1 = PersonName("firstName1", Some("middleName1"), "lastName1")
    val positions = Positions(Set(BeneficialOwner, InternalAccountant), Some(new LocalDate()))
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

        "status is pre-submission" in new Fixture {

          mockApplicationStatus(SubmissionReady)
          mockCacheGetEntry[Seq[ResponsiblePerson]](Some(responsiblePeoples), ResponsiblePerson.key)
          mockCacheGetEntry[BusinessNominatedOfficer](None, BusinessNominatedOfficer.key)

          val result = controller.get()(request)
          status(result) must be(OK)

          contentAsString(result) must include(Messages("submit.registration"))
        }

        "status is pending" in new Fixture {

          mockApplicationStatus(SubmissionReadyForReview)
          mockCacheGetEntry[Seq[ResponsiblePerson]](Some(responsiblePeoples), ResponsiblePerson.key)
          mockCacheGetEntry[BusinessNominatedOfficer](None, BusinessNominatedOfficer.key)

          val result = controller.get()(request)
          status(result) must be(OK)

          contentAsString(result) must include(Messages("submit.amendment.application"))
        }

        "status is approved" in new Fixture {

          mockApplicationStatus(SubmissionDecisionApproved)
          mockCacheGetEntry[Seq[ResponsiblePerson]](Some(responsiblePeoples), ResponsiblePerson.key)
          mockCacheGetEntry[BusinessNominatedOfficer](None, BusinessNominatedOfficer.key)

          val result = controller.get()(request)
          status(result) must be(OK)

          contentAsString(result) must include(Messages("submit.amendment.application"))
        }

        "status is ready for renewal" in new Fixture {

          mockApplicationStatus(ReadyForRenewal(Some(new LocalDate())))
          mockCacheGetEntry[Seq[ResponsiblePerson]](Some(responsiblePeoples), ResponsiblePerson.key)
          mockCacheGetEntry[BusinessNominatedOfficer](None, BusinessNominatedOfficer.key)

          val result = controller.get()(request)
          status(result) must be(OK)

          contentAsString(result) must include(Messages("submit.renewal.application"))
        }
    }

    "redirect to Fee Guidance" when {

      "selected option is a valid responsible person" in new Fixture {

        val newRequest = request.withFormUrlEncodedBody("value" -> "firstNamemiddleNamelastName")

        val updatedList = Seq(rp.copy(
          positions = Some(positions.copy(positions = Set(BeneficialOwner, InternalAccountant, NominatedOfficer)))
        ), rp2)

        mockCacheFetch[Seq[ResponsiblePerson]](Some(responsiblePeoples), Some(ResponsiblePerson.key))
        mockApplicationStatus(SubmissionReady)
        mockCacheSave[Option[Seq[ResponsiblePerson]]](Some(updatedList))

        val result = controller.post()(newRequest)
        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some(controllers.routes.FeeGuidanceController.get().url))
      }

    }

    "redirect to 'Who is registering this business?' page" when {

      "post submission" when {

        "selected option is a valid responsible person in amendment mode" in new Fixture {

          val newRequest = request.withFormUrlEncodedBody("value" -> "firstNamemiddleNamelastName")

          val updatedList = Seq(rp.copy(
            positions = Some(positions.copy(positions = Set(BeneficialOwner, InternalAccountant, NominatedOfficer)))
          ), rp2)

          mockCacheFetch[Seq[ResponsiblePerson]](Some(responsiblePeoples), Some(ResponsiblePerson.key))
          mockApplicationStatus(SubmissionDecisionApproved)
          mockCacheSave[Option[Seq[ResponsiblePerson]]](Some(updatedList))

          val result = controller.post()(newRequest)
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(routes.WhoIsRegisteringController.get().url))
        }

        "selected option is a valid responsible person" in new Fixture {

          val newRequest = request.withFormUrlEncodedBody("value" -> "firstNamemiddleNamelastName")

          val updatedList = Seq(rp.copy(
            positions = Some(positions.copy(positions = Set(BeneficialOwner, InternalAccountant, NominatedOfficer)))
          ), rp2)

          mockCacheFetch[Seq[ResponsiblePerson]](Some(responsiblePeoples), Some(ResponsiblePerson.key))
          mockApplicationStatus(SubmissionReadyForReview)
          mockCacheSave[Option[Seq[ResponsiblePerson]]](Some(updatedList))

          val result = controller.post()(newRequest)
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(controllers.declaration.routes.WhoIsRegisteringController.get().url))
        }

      }

      "pre-submission" when {

        "show-fees is toggled off" in new Fixture {

          override val builder = defaultBuilder.configure("microservice.services.feature-toggle.show-fees" -> false)

          val newRequest = request.withFormUrlEncodedBody("value" -> "firstNamemiddleNamelastName")

          val updatedList = Seq(rp.copy(
            positions = Some(positions.copy(positions = Set(BeneficialOwner, InternalAccountant, NominatedOfficer)))
          ), rp2)

          mockCacheFetch[Seq[ResponsiblePerson]](Some(responsiblePeoples), Some(ResponsiblePerson.key))
          mockApplicationStatus(SubmissionReady)
          mockCacheSave[Option[Seq[ResponsiblePerson]]](Some(updatedList))

          val result = controller.post()(newRequest)
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(controllers.declaration.routes.WhoIsRegisteringController.get().url))

        }

      }

    }

    "successfully redirect to adding new responsible people .i.e what you need page of RP" when {
      "selected option is 'Register someone else'" in new Fixture {
        val newRequest = request.withFormUrlEncodedBody("value" -> "-1")

        mockCacheGetEntry[Seq[ResponsiblePerson]](Some(responsiblePeoples), ResponsiblePerson.key)
        mockApplicationStatus(SubmissionReady)

        val result = controller.post()(newRequest)
        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some(controllers.responsiblepeople.routes.ResponsiblePeopleAddController.get(true, Some(flowFromDeclaration)).url))
      }
    }

    "fail validation" when {
      "no option is selected on the UI" in new Fixture {
        val newRequest = request.withFormUrlEncodedBody()

        mockCacheFetch[Seq[ResponsiblePerson]](Some(responsiblePeoples), Some(ResponsiblePerson.key))
        mockApplicationStatus(SubmissionReady)

        val result = controller.post()(newRequest)
        status(result) must be(BAD_REQUEST)
        contentAsString(result) must include(Messages("error.required.declaration.nominated.officer"))
      }
    }
  }

}

