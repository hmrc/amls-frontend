package controllers.tradingpremises

import connectors.DataCacheConnector
import models.tradingpremises.{TradingPremises, YourTradingPremises}
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import play.api.test.Helpers._
import services.StatusService
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.{AuthorisedFixture, GenericTestHelper}

import scala.concurrent.Future

class RemoveAgentPremisesReasonsControllerSpec extends GenericTestHelper with MockitoSugar{

  trait Fixture extends AuthorisedFixture {
    self =>

    implicit val request = addToken(authRequest)

    val controller = new RemoveAgentPremisesReasonsController {
      override val dataCacheConnector: DataCacheConnector = mock[DataCacheConnector]
      override protected def authConnector: AuthConnector = self.authConnector
      override val statusService: StatusService = mock[StatusService]
    }

    val tradingPremises = mock[TradingPremises]
    val yourTradingPremises = mock[YourTradingPremises]

    when(tradingPremises.yourTradingPremises) thenReturn Some(yourTradingPremises)
    when(yourTradingPremises.tradingName) thenReturn "My Company"

    def mockFetch(model: Option[Seq[TradingPremises]]) =
      when(controller.dataCacheConnector.fetch[Seq[TradingPremises]](any())
        (any(), any(), any())).thenReturn(Future.successful(model))
  }

  "Remove agent premises reasons controller" when {

    "invoking the GET action" must {

      "load the 'Why are you removing this trading premises?' page" in new Fixture {

        mockFetch(Some(Seq(tradingPremises)))

        val result = controller.get(1)(request)
        status(result) must be(OK)

      }

      "return NOT FOUND when there is no trading premises found" in new Fixture {

        mockFetch(None)

        val result = controller.get(1)(request)
        status(result) must be(NOT_FOUND)

      }
    }
  }

}
