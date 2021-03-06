/*
 * Copyright 2021 HM Revenue & Customs
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
import play.api.test.FakeRequest
import utils.{AmlsViewSpec, StatusConstants}
import views.Fixture
import views.html.tradingpremises.summary

sealed trait ViewTestHelper extends AmlsViewSpec {
  val tradingPremises = Seq(TradingPremises(
    registeringAgentPremises = Some(RegisteringAgentPremises(true)),
    status = Some(StatusConstants.Added),
    lineId = Some(11),
    yourTradingPremises = Some(YourTradingPremises("Test", Address("Line 1", "Line 2", None, None, "TEST", None), Some(true), Some(LocalDate.now), None))
  ))

  trait ViewFixture extends Fixture {
    lazy val summary = app.injector.instanceOf[summary]
    implicit val requestWithToken = addTokenForView()
  }
}

class SummarySpec extends ViewTestHelper {

  "The summary page" must {

    "redirect to the 'remove agent premises reasons' page when 'delete' is clicked" in new ViewFixture {

      def view = summary(EmptyForm, tradingPremises, add = true, SubmissionDecisionApproved)

      doc.getElementsByClass("check-your-answers__listing").select("a:nth-child(2)").attr("href") must be(
        controllers.tradingpremises.routes.RemoveAgentPremisesReasonsController.get(1, complete = true).url
      )
    }

    "redirect to the 'remove trading premises' page when 'delete' is clicked and it's an agent premises and it's an amendment" in new ViewFixture {
      def view = summary(EmptyForm, tradingPremises, add = true, SubmissionReadyForReview)

      doc.getElementsByClass("check-your-answers__listing").select("a:nth-child(2)").attr("href") must be (
        controllers.tradingpremises.routes.RemoveTradingPremisesController.get(1, complete = true).url
      )
    }
  }

}
