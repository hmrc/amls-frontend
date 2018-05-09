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

package controllers.declaration

import connectors.DataCacheConnector
import models.status.{SubmissionReady, SubmissionReadyForReview, SubmissionDecisionApproved, ReadyForRenewal}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import play.api.i18n.Messages
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.FakeApplication
import services.StatusService
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.{AuthorisedFixture, AmlsSpec}
import play.api.inject.bind
import org.mockito.Mockito._
import org.mockito.Matchers._
import play.api.test.Helpers._


import scala.concurrent.Future

class RegisterResponsiblePersonControllerSpec extends AmlsSpec with MockitoSugar with ScalaFutures {
  override lazy val app = FakeApplication(additionalConfiguration = Map("microservice.services.feature-toggle.amendments" -> false))

  trait Fixture extends AuthorisedFixture {
    self =>
    val request = addToken(authRequest)
    val dataCacheConnector = mock[DataCacheConnector]
    val statusService = mock[StatusService]

    lazy val app = new GuiceApplicationBuilder()
      .disable[com.kenshoo.play.metrics.PlayModule]
      .overrides(bind[DataCacheConnector].to(dataCacheConnector))
      .overrides(bind[AuthConnector].to(self.authConnector))
      .overrides(bind[StatusService].to(self.statusService))
      .build()

    val controller = app.injector.instanceOf[RegisterResponsiblePersonController]

  }

  "RegisterResponsiblePersonController" when {

    "status is ReadyForRenewal" must {
      "respond with OK and show the correct subtitle" in new Fixture {

        when(controller.statusService.getStatus(any(), any(), any()))
          .thenReturn(Future.successful(ReadyForRenewal(None)))

        val result = controller.get()(request)
        status(result) must be(OK)

        contentAsString(result) must include(Messages("submit.amendment.application"))
      }
    }
    "status is SubmissionDecisionApproved" must {
      "respond with OK and show the correct subtitle" in new Fixture {

        when(controller.statusService.getStatus(any(), any(), any()))
          .thenReturn(Future.successful(SubmissionDecisionApproved))

        val result = controller.get()(request)
        status(result) must be(OK)

        contentAsString(result) must include(Messages("submit.amendment.application"))
      }
    }
    "status is SubmissionReadyForReview" must {
      "respond with OK and show the correct subtitle" in new Fixture {

        when(controller.statusService.getStatus(any(), any(), any()))
          .thenReturn(Future.successful(SubmissionReadyForReview))

        val result = controller.get()(request)
        status(result) must be(OK)

        contentAsString(result) must include(Messages("submit.amendment.application"))
      }
    }
    "status is other e.g. SubmissionReady" must {
      "respond with OK and show the correct subtitle" in new Fixture {

        when(controller.statusService.getStatus(any(), any(), any()))
          .thenReturn(Future.successful(SubmissionReady))

        val result = controller.get()(request)
        status(result) must be(OK)

        contentAsString(result) must include(Messages("submit.registration"))
      }
    }
  }
}