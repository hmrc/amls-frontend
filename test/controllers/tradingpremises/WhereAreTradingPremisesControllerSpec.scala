package controllers.tradingpremises

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import models.tradingpremises.TradingPremises
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.i18n.Messages
import play.api.test.Helpers._
import utils.AuthorisedFixture

import scala.concurrent.Future

class WhereAreTradingPremisesControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar {

  trait Fixture extends AuthorisedFixture {
    self =>

    val controller = new WhereAreTradingPremisesController {
      override val dataCacheConnector = mock[DataCacheConnector]
      override val authConnector = self.authConnector
    }
  }

  "WhereAreTradingPremisesController" must {

    "use correct services" in new Fixture {
      WhereAreTradingPremisesController.authConnector must be(AMLSAuthConnector)
      WhereAreTradingPremisesController.dataCacheConnector must be(DataCacheConnector)
    }


    "on get display Where are Trading Premises page" in new Fixture {
      when(controller.dataCacheConnector.fetchDataShortLivedCache[TradingPremises](any())
        (any(), any(), any())).thenReturn(Future.successful(None))
      val result = controller.get(any())(request)
      status(result) must be(OK)
      contentAsString(result) must include(Messages("tradingpremises.yourtradingpremises.title"))
    }
  }

}
