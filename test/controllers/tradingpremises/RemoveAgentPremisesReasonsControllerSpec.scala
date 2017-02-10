package controllers.tradingpremises

import models.hvd.Hvd
import models.tradingpremises.TradingPremises
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import play.api.i18n.Messages
import play.api.test.Helpers._
import utils.GenericTestHelper

import scala.concurrent.Future

/**
  * Created by lorenzo on 09/02/17.
  */
class RemoveAgentPremisesReasonsControllerSpec extends GenericTestHelper with MockitoSugar{

  import connectors.DataCacheConnector
  import services.StatusService
  import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
  import utils.AuthorisedFixture

  trait Fixture extends AuthorisedFixture {
    self => val request = addToken(authRequest)

    val controller = new RemoveAgentPremisesReasonsController {

      override val dataCacheConnector: DataCacheConnector = mock[DataCacheConnector]
      override protected def authConnector: AuthConnector = self.authConnector
      override val statusService: StatusService = mock[StatusService]
    }
  }

  "Remove agent premises reasons controller" must {
    "load the 'Why are you removing this trading premises?' page" in new Fixture  {

      when(controller.dataCacheConnector.fetch[TradingPremises](any())
        (any(), any(), any())).thenReturn(Future.successful(None))

      val result = controller.get()(request)
      status(result) must be(OK)
      contentAsString(result) must include(Messages("hvd.products.title"))

    }
  }

}
