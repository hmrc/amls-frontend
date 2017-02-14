package views.tradingpremises

import models.tradingpremises.{Address, RegisteringAgentPremises, TradingPremises, YourTradingPremises}
import org.joda.time.LocalDate
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.FakeRequest
import utils.{GenericTestHelper, StatusConstants}
import views.Fixture

trait ViewTestHelper extends GenericTestHelper {
  val tradingPremises = Seq(TradingPremises(
    registeringAgentPremises = Some(RegisteringAgentPremises(true)),
    status = Some(StatusConstants.Added),
    yourTradingPremises = Some(YourTradingPremises("Test", Address("Line 1", "Line 2", None, None, "TEST", None), true, LocalDate.now, None))
  ))

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(FakeRequest())
  }
}

class summarySpec extends ViewTestHelper {

  override lazy val app = new GuiceApplicationBuilder()
      .configure(Map("Test.microservice.services.feature-toggle.release7" -> false))
      .build()

  "The summary page (release 6)" must {

    "redirect to the 'remove trading premises' page when 'delete' is clicked" in new ViewFixture {

      def view = views.html.tradingpremises.summary(tradingPremises, true)

      doc.getElementsByClass("check-your-answers__listing").select("a:nth-child(2)").attr("href") must be(
        controllers.tradingpremises.routes.RemoveTradingPremisesController.get(1, true).url
      )
    }
  }

}

class summarySpecRelease7 extends ViewTestHelper {

  override lazy val app = new GuiceApplicationBuilder()
    .configure(Map("Test.microservice.services.feature-toggle.release7" -> true))
    .build()

  "The summary page (release 6)" must {

    "redirect to the 'remove trading premises' page when 'delete' is clicked" in new ViewFixture {

      def view = views.html.tradingpremises.summary(tradingPremises, true)

      doc.getElementsByClass("check-your-answers__listing").select("a:nth-child(2)").attr("href") must be(
        controllers.tradingpremises.routes.RemoveAgentPremisesReasonsController.get(1, true).url
      )
    }
  }

}