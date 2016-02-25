package controllers.tradingpremises

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import models.tradingpremises._
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.test.Helpers._
import utils.AuthorisedFixture

import scala.concurrent.Future

class AgentSummaryControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar {

  trait Fixture extends AuthorisedFixture {
    self =>

    val controller = new SummaryController {
      override val dataCacheConnector = mock[DataCacheConnector]
      override val authConnector = self.authConnector
    }
  }

  "Get" must {

    "use correct services" in new Fixture {
      SummaryController.authConnector must be(AMLSAuthConnector)
      SummaryController.dataCacheConnector must be(DataCacheConnector)
    }

    "load the your agents' premises summary page when section data is available" in new Fixture {

      val model = TradingPremises(None, None)

      when(controller.dataCacheConnector.fetchDataShortLivedCache[TradingPremises](any())
        (any(), any(), any())).thenReturn(Future.successful(Some(model)))

      val result = controller.get()(request)
      status(result) must be(OK)
    }

    "redirect to the trading premises summary page when section data is unavailable" in new Fixture {

      when(controller.dataCacheConnector.fetchDataShortLivedCache[TradingPremises](any())
        (any(), any(), any())).thenReturn(Future.successful(None))

      val result = controller.get()(request)
      status(result) must be(SEE_OTHER)
    }
  }
}
