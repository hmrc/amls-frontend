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

import connectors.DataCacheConnector
import models.responsiblepeople._
import models.status.{ReadyForRenewal, SubmissionDecisionApproved, SubmissionReady, SubmissionReadyForReview}
import org.joda.time.LocalDate
import org.scalatest.mock.MockitoSugar
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.FakeApplication
import services.{ProgressService, StatusService}
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.{AuthorisedFixture, GenericTestHelper, StatusConstants}
import play.api.inject.bind
import play.api.test.Helpers._
import org.mockito.Mockito._
import org.mockito.Matchers._
import play.api.i18n.Messages

import scala.concurrent.Future


class RegisterPartnersControllerSpec extends GenericTestHelper with MockitoSugar {

  override lazy val app = FakeApplication(additionalConfiguration = Map("microservice.services.feature-toggle.amendments" -> false))

  trait Fixture extends AuthorisedFixture {
    self =>
    val request = addToken(authRequest)
    val dataCacheConnector = mock[DataCacheConnector]
    val statusService = mock[StatusService]
    val progressService = mock[ProgressService]

    lazy val app = new GuiceApplicationBuilder()
      .disable[com.kenshoo.play.metrics.PlayModule]
      .overrides(bind[AuthConnector].to(authConnector))
      .overrides(bind[DataCacheConnector].to(dataCacheConnector))
      .overrides(bind[StatusService].to(statusService))
      .overrides(bind[ProgressService].to(progressService))
      .build()

    val controller = app.injector.instanceOf[RegisterPartnersController]

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
  }

  "The RegisterPartnersController" when {
    "get is called" must {
      "respond with OK" in new Fixture {

        when {
          dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(),any(),any())
        } thenReturn Future.successful(Some(Seq(ResponsiblePeople())))

        when {
          statusService.getStatus(any(),any(),any())
        } thenReturn Future.successful(SubmissionDecisionApproved)

        val result = controller.get()(request)

        status(result) mustBe OK

      }
    }

    "post is called" must {
      "fail validation" when {
        "no option is selected on the UI and status is submissionready" in new Fixture {
          val newRequest = request.withFormUrlEncodedBody()
          when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(responsiblePeoples)))

          when(controller.statusService.getStatus(any(),any(),any()))
            .thenReturn(Future.successful(SubmissionReady))

          val result = controller.post()(newRequest)
          status(result) must be(BAD_REQUEST)
          contentAsString(result) must include(Messages("error.required.declaration.partners"))
          contentAsString(result) must include(Messages("submit.registration"))
        }

        "no option is selected on the UI and status is SubmissionReadyForReview" in new Fixture {
          val newRequest = request.withFormUrlEncodedBody()
          when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(responsiblePeoples)))

          when(controller.statusService.getStatus(any(),any(),any()))
            .thenReturn(Future.successful(SubmissionReadyForReview))

          val result = controller.post()(newRequest)
          status(result) must be(BAD_REQUEST)
          contentAsString(result) must include(Messages("error.required.declaration.partners"))
          contentAsString(result) must include(Messages("submit.amendment.application"))
        }

        "no option is selected on the UI and status is ReadyForRenewal" in new Fixture {
          val newRequest = request.withFormUrlEncodedBody()
          when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(responsiblePeoples)))

          when(controller.statusService.getStatus(any(),any(),any()))
            .thenReturn(Future.successful(ReadyForRenewal(Some(new LocalDate()))))

          val result = controller.post()(newRequest)
          status(result) must be(BAD_REQUEST)
          contentAsString(result) must include(Messages("error.required.declaration.partners"))
          contentAsString(result) must include(Messages("submit.renewal.application"))
        }

        "no option is selected on the UI and no responsible people returned" in new Fixture {
          val newRequest = request.withFormUrlEncodedBody()
          when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
            .thenReturn(Future.successful(None))

          when(controller.statusService.getStatus(any(),any(),any()))
            .thenReturn(Future.successful(SubmissionReady))

          val result = controller.post()(newRequest)
          status(result) must be(BAD_REQUEST)
          contentAsString(result) must include(Messages("error.required.declaration.partners"))
        }
      }
    }
  }
}
