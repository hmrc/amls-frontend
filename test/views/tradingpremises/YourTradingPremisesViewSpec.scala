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

package views.tradingpremises

import generators.tradingpremises.TradingPremisesGenerator
import models.status.NotCompleted
import models.tradingpremises.{Address, TradingPremises, YourTradingPremises}
import org.scalatest.matchers.must.Matchers
import play.api.mvc.{AnyContentAsEmpty, Request}
import utils.AmlsViewSpec
import views.Fixture
import views.html.tradingpremises.YourTradingPremisesView

class YourTradingPremisesViewSpec extends AmlsViewSpec with Matchers with TradingPremisesGenerator {

  trait ViewFixture extends Fixture {
    lazy val your_trading_premises                                 = app.injector.instanceOf[YourTradingPremisesView]
    implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView()
  }

  val tpAddress = Address("CPLT 1", Some("SecondLine"), Some("ThirdLine"), Some("FourthLine"), "AQ11QA")

  val completeTp    = TradingPremises(yourTradingPremises = Some(YourTradingPremises("Complete TP 1", tpAddress)))
  val completeTp2   = TradingPremises(yourTradingPremises =
    Some(YourTradingPremises("Complete TP 2", tpAddress.copy(addressLine1 = "CPLT 2")))
  )
  val incompleteTp1 = TradingPremises(yourTradingPremises =
    Some(YourTradingPremises("inComplete TP 1", tpAddress.copy(addressLine1 = "INCPLT 1")))
  )
  val incompleteTp2 = TradingPremises(yourTradingPremises =
    Some(YourTradingPremises("inComplete TP 2", tpAddress.copy(addressLine1 = "INCPLT 2")))
  )

  val completeTpSeq   = Seq((completeTp, 0), (completeTp2, 1))
  val incompleteTpSeq = Seq((incompleteTp1, 2), (incompleteTp2, 3))

  "YourTradingPremisesView" must {
    "have correct title, headings displayed, no complete/incomplete headers displayed" in new ViewFixture {

      def view =
        your_trading_premises(false, NotCompleted, Seq.empty[(TradingPremises, Int)], Seq.empty[(TradingPremises, Int)])

      doc.title       must be(
        messages("tradingpremises.yourpremises.title") +
          " - " + messages("tradingpremises.subheading") +
          " - " + messages("title.amls") +
          " - " + messages("title.gov")
      )
      heading.html    must be(messages("tradingpremises.yourpremises.title"))
      subHeading.html must include(messages("tradingpremises.subheading"))

      doc.getElementsByAttributeValue("id", "addTradingPremises") must not be empty

      html must not include messages("tradingpremises.yourpremises.incomplete")
      html must not include messages("tradingpremises.yourpremises.complete")

      html must include(messages("tradingpremises.yourpremises.line_1"))
      html must include(messages("tradingpremises.yourpremises.line_2"))
      html must include(messages("tradingpremises.yourpremises.line_3"))
    }

    "have list with Incomplete and Complete headers displayed when there are both types of lists" in new ViewFixture {
      def view = your_trading_premises(false, NotCompleted, completeTpSeq, incompleteTpSeq)

      html must include(messages("tradingpremises.yourpremises.incomplete"))
      html must include(messages("tradingpremises.yourpremises.complete"))
    }

    "have list with Incomplete header displayed when there are only incomplete TP's" in new ViewFixture {
      def view = your_trading_premises(false, NotCompleted, Seq.empty[(TradingPremises, Int)], incompleteTpSeq)

      html must include(messages("tradingpremises.yourpremises.incomplete"))
      html must not include messages("tradingpremises.yourpremises.complete")
    }

    "have list without Complete/Incomplete headers displayed when there are only complete TP's" in new ViewFixture {
      def view = your_trading_premises(false, NotCompleted, completeTpSeq, Seq.empty[(TradingPremises, Int)])

      html must not include messages("tradingpremises.yourpremises.incomplete")
      html must not include messages("tradingpremises.yourpremises.complete")
    }

    "have an add a trading premises link with the correct text and going to the what you need page" in new ViewFixture {
      def view = your_trading_premises(false, NotCompleted, completeTpSeq, incompleteTpSeq)

      doc.getElementById("addTradingPremises").text         must be(messages("tradingpremises.summary.addanother"))
      doc.getElementById("addTradingPremises").attr("href") must be(
        controllers.tradingpremises.routes.TradingPremisesAddController.get(true).url
      )
    }

    "have an incomplete/complete sections with addresses displayed and edit/remove links" in new ViewFixture {
      def view = your_trading_premises(false, NotCompleted, completeTpSeq, incompleteTpSeq)

      doc.getElementById("complete-header").text must include(messages("tradingpremises.yourpremises.complete"))

      doc.getElementById("detail-edit-0").attr("href")   must be(
        controllers.tradingpremises.routes.CheckYourAnswersController.get(1).url
      )
      doc.getElementById("detail-remove-0").attr("href") must be(
        controllers.tradingpremises.routes.RemoveTradingPremisesController.get(1).url
      )

      doc.getElementById("detail-edit-1").attr("href")   must be(
        controllers.tradingpremises.routes.CheckYourAnswersController.get(2).url
      )
      doc.getElementById("detail-remove-1").attr("href") must be(
        controllers.tradingpremises.routes.RemoveTradingPremisesController.get(2).url
      )

      doc.getElementById("incomplete-header").text must include(messages("tradingpremises.yourpremises.incomplete"))

      doc.getElementById("incomplete-detail-2").text     must include("INCPLT 1")
      doc.getElementById("detail-edit-2").attr("href")   must be(
        controllers.tradingpremises.routes.YourTradingPremisesController.getIndividual(3, true).url
      )
      doc.getElementById("detail-remove-2").attr("href") must be(
        controllers.tradingpremises.routes.RemoveTradingPremisesController.get(3).url
      )

      doc.getElementById("incomplete-detail-3").text     must include("INCPLT 2")
      doc.getElementById("detail-edit-3").attr("href")   must be(
        controllers.tradingpremises.routes.YourTradingPremisesController.getIndividual(4, true).url
      )
      doc.getElementById("detail-remove-3").attr("href") must be(
        controllers.tradingpremises.routes.RemoveTradingPremisesController.get(4).url
      )

    }

    "show the correct continuation button when there are both types of lists" in new ViewFixture {
      def view = your_trading_premises(false, NotCompleted, completeTpSeq, incompleteTpSeq)
      html must include(messages("button.returntoapplicationprogress"))
    }

    "show the correct continuation button when there are only incomplete tps" in new ViewFixture {
      def view = your_trading_premises(false, NotCompleted, Seq.empty[(TradingPremises, Int)], incompleteTpSeq)
      html must include(messages("button.returntoapplicationprogress"))
    }

    "show the correct continuation button when there are only complete TP's" in new ViewFixture {
      def view = your_trading_premises(false, NotCompleted, completeTpSeq, Seq.empty[(TradingPremises, Int)])
      html must include(messages("button.checkyouranswers.acceptandcomplete"))
    }

    "show the correct continuation button when there are no TPs" in new ViewFixture {
      def view =
        your_trading_premises(false, NotCompleted, Seq.empty[(TradingPremises, Int)], Seq.empty[(TradingPremises, Int)])
      html must include(messages("button.returntoapplicationprogress"))
    }
  }
}
