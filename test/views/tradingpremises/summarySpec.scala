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

package views.tradingpremises

import forms.EmptyForm
import models.status.{SubmissionDecisionApproved, SubmissionReadyForReview}
import models.tradingpremises.{Address, RegisteringAgentPremises, TradingPremises, YourTradingPremises}
import org.joda.time.LocalDate
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.FakeRequest
import utils.{GenericTestHelper, StatusConstants}
import views.Fixture

sealed trait ViewTestHelper extends GenericTestHelper {
  val tradingPremises = Seq(TradingPremises(
    registeringAgentPremises = Some(RegisteringAgentPremises(true)),
    status = Some(StatusConstants.Added),
    lineId = Some(11),
    yourTradingPremises = Some(YourTradingPremises("Test", Address("Line 1", "Line 2", None, None, "TEST", None), Some(true), Some(LocalDate.now), None))
  ))

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(FakeRequest())
  }
}

class summarySpec extends ViewTestHelper {

  override lazy val app = new GuiceApplicationBuilder()
      .configure(Map("microservice.services.feature-toggle.release7" -> false))
      .build()

  "The summary page" must {

    "redirect to the 'remove trading premises' page when 'delete' is clicked" in new ViewFixture {

      def view = views.html.tradingpremises.summary(EmptyForm, tradingPremises, add = true, SubmissionDecisionApproved)

      doc.getElementsByClass("check-your-answers__listing").select("a:nth-child(2)").attr("href") must be(
        controllers.tradingpremises.routes.RemoveTradingPremisesController.get(1, complete = true).url
      )
    }
  }

}

class summarySpecRelease7 extends ViewTestHelper {

  override lazy val app = new GuiceApplicationBuilder()
    .configure(Map("microservice.services.feature-toggle.release7" -> true))
    .build()

  "The summary page" must {

    "redirect to the 'remove agent premises reasons' page when 'delete' is clicked" in new ViewFixture {

      def view = views.html.tradingpremises.summary(EmptyForm, tradingPremises, add = true, SubmissionDecisionApproved)

      doc.getElementsByClass("check-your-answers__listing").select("a:nth-child(2)").attr("href") must be(
        controllers.tradingpremises.routes.RemoveAgentPremisesReasonsController.get(1, complete = true).url
      )
    }

    "redirect to the 'remove trading premises' page when 'delete' is clicked and it's an agent premises and it's an amendment" in new ViewFixture {
      def view = views.html.tradingpremises.summary(EmptyForm, tradingPremises, add = true, SubmissionReadyForReview)

      doc.getElementsByClass("check-your-answers__listing").select("a:nth-child(2)").attr("href") must be (
        controllers.tradingpremises.routes.RemoveTradingPremisesController.get(1, complete = true).url
      )
    }
  }

}
