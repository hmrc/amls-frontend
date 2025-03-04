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
import models.status.{ReadyForRenewal, SubmissionDecisionApproved, SubmissionReady, SubmissionReadyForReview}
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import play.api.i18n.Messages
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers._
import services.StatusService
import utils.{AmlsSpec, AuthAction, DependencyMocks}

class RegisterResponsiblePersonControllerSpec extends AmlsSpec with MockitoSugar with ScalaFutures {

  trait Fixture extends DependencyMocks {
    self =>
    val request            = addToken(authRequest)
    val dataCacheConnector = mock[DataCacheConnector]
    val statusService      = mockStatusService

    lazy val app = new GuiceApplicationBuilder()
      .overrides(bind[DataCacheConnector].to(dataCacheConnector))
      .overrides(bind[AuthAction].to(SuccessfulAuthAction))
      .overrides(bind[StatusService].to(self.statusService))
      .configure(
        "play.filters.disabled" -> List("uk.gov.hmrc.play.bootstrap.frontend.filters.crypto.SessionCookieCryptoFilter")
      )
      .build()

    val controller = app.injector.instanceOf[RegisterResponsiblePersonController]

  }

  "RegisterResponsiblePersonController" when {

    "status is ReadyForRenewal"            must {
      "respond with OK and show the correct subtitle" in new Fixture {

        mockApplicationStatus(ReadyForRenewal(None))

        val result = controller.get()(request)
        status(result) must be(OK)

        contentAsString(result) must include(Messages("submit.amendment.application"))
      }
    }
    "status is SubmissionDecisionApproved" must {
      "respond with OK and show the correct subtitle" in new Fixture {

        mockApplicationStatus(SubmissionDecisionApproved)

        val result = controller.get()(request)
        status(result) must be(OK)

        contentAsString(result) must include(Messages("submit.amendment.application"))
      }
    }
    "status is SubmissionReadyForReview"   must {
      "respond with OK and show the correct subtitle" in new Fixture {

        mockApplicationStatus(SubmissionReadyForReview)

        val result = controller.get()(request)
        status(result) must be(OK)

        contentAsString(result) must include(Messages("submit.amendment.application"))
      }
    }
    "status is other e.g. SubmissionReady" must {
      "respond with OK and show the correct subtitle" in new Fixture {

        mockApplicationStatus(SubmissionReady)

        val result = controller.get()(request)
        status(result) must be(OK)

        contentAsString(result) must include(Messages("submit.registration"))
      }
    }
  }
}
