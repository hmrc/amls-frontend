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
import models.responsiblepeople.ResponsiblePeople
import models.status.SubmissionDecisionApproved
import org.scalatest.mock.MockitoSugar
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.FakeApplication
import services.{ProgressService, StatusService}
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.{AuthorisedFixture, GenericTestHelper}
import play.api.inject.bind
import play.api.test.Helpers._
import org.mockito.Mockito._
import org.mockito.Matchers._

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
  }
}
