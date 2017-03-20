package controllers.tradingpremises

import connectors.DataCacheConnector
import models.aboutthebusiness._
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import play.api.i18n.Messages
import play.api.test.Helpers._
import utils.{AuthorisedFixture, GenericTestHelper}

import scala.concurrent.Future

class ConfirmAddressControllerSpec extends GenericTestHelper with MockitoSugar {

  trait Fixture extends AuthorisedFixture {
    self => val request = addToken(authRequest)
    val dataCache:  DataCacheConnector  = mock[DataCacheConnector]
    override val authConnector = self.authConnector
    val controller = new ConfirmAddressController (messagesApi, self.dataCache, self.authConnector)
  }

  private val ukAddress = RegisteredOfficeUK("line_1", "line_2", Some(""), Some(""), "CA3 9ST")
  private val aboutTheBusiness = AboutTheBusiness(None, None, None, None, None, Some(ukAddress), None)

  "ConfirmTradingPremisesAddress" must {

    "Get Option:" must {

      "Load Confirm trading premises address page successfully" in new Fixture {

        when(controller.dataCacheConnector.fetch[AboutTheBusiness](any())(any(), any(), any())).thenReturn(Future.successful(Some(aboutTheBusiness)))

        val result = controller.get(1)(request)

      }


    }

    "Post" must {

    }
  }
}
