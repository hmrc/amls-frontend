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

package controllers.businessmatching.updateservice.add


import models.businessmatching.HighValueDealing
import models.flowmanagement.AddServiceFlowModel
import models.status.SubmissionDecisionApproved
import models.tradingpremises.TradingPremises
import org.scalatest.mock.MockitoSugar
import play.api.i18n.Messages
import play.api.test.Helpers._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.frontend.auth.AuthContext
import utils.{AuthorisedFixture, DependencyMocks, GenericTestHelper}

import scala.concurrent.ExecutionContext

class UpdateServicesSummaryControllerSpec extends GenericTestHelper with MockitoSugar {

  sealed trait Fixture extends AuthorisedFixture with DependencyMocks {
    self =>
    val request = addToken(authRequest)

    implicit val hc: HeaderCarrier = HeaderCarrier()
    implicit val authContext: AuthContext = mock[AuthContext]
    implicit val ec: ExecutionContext = mock[ExecutionContext]

    mockCacheFetch(Some(AddServiceFlowModel(Some(HighValueDealing))))
    mockApplicationStatus(SubmissionDecisionApproved)

    val controller = new UpdateServicesSummaryController(
      self.authConnector,
      mockCacheConnector
    )
  }

  "Get" must {
    "return OK with update_service_summary view" in new Fixture {

      val result = controller.get()(request)
      status(result) must be(OK)

      contentAsString(result) must include(Messages("businessmatching.updateservice.selectactivities.title"))

      contentAsString(result) must include(Messages("button.checkyouranswers.acceptandcomplete"))
    }
  }

  "post is called" must {
    "respond with OK and redirect to the 'do you want to add more activities' page " +
      "if the user clicks continue and there are available Activities to select" in new Fixture {

      val result = controller.post()(request)

      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(controllers.businessmatching.updateservice.add.routes.AddMoreActivitiesController.get().url))
    }

  }
}

