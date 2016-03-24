package controllers.tradingpremises

import java.util.UUID

import connectors.DataCacheConnector
import models.tradingpremises.TradingPremises
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import utils.AuthorisedFixture

import scala.concurrent.Future

class SummaryControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar {

  implicit val request = FakeRequest
  val userId = s"user-${UUID.randomUUID()}"
  val mockDataCacheConnector = mock[DataCacheConnector]

  trait Fixture extends AuthorisedFixture {
    self =>

    val summaryController = new SummaryController {
      override val dataCacheConnector = mockDataCacheConnector
      override val authConnector = self.authConnector
    }
  }

  "SummaryController" must {

    "load the summary page when the model is present" in new Fixture {

      val model = TradingPremises()
      when(mockDataCacheConnector.fetch[Seq[TradingPremises]](any())
        (any(), any(), any())).thenReturn(Future.successful(Some(Seq(model))))
      val result = summaryController.get()(request)
      status(result) must be(OK)
    }


    "redirect to the main amls summary page when section data is unavailable" in new Fixture {

      when(summaryController.dataCacheConnector.fetch[Seq[TradingPremises]](any())
        (any(), any(), any())).thenReturn(Future.successful(None))
      val result = summaryController.get()(request)
      redirectLocation(result) must be(Some(controllers.routes.RegistrationProgressController.get.url))
      status(result) must be(SEE_OTHER)
    }

    "for an individual display the trading premises summary page for individual" in new Fixture {

      val model = TradingPremises()
      when(mockDataCacheConnector.fetch[Seq[TradingPremises]](any())
        (any(), any(), any())).thenReturn(Future.successful(Some(Seq(model))))
      val result = summaryController.getIndividual(1)(request)
      status(result) must be(OK)
    }


    "for an individual redirect to the trading premises summary summary if data is not present" in new Fixture {
      when(mockDataCacheConnector.fetch[Seq[TradingPremises]](any())
        (any(), any(), any())).thenReturn(Future.successful(None))
      val result = summaryController.getIndividual(1)(request)
      redirectLocation(result) must be(Some(routes.SummaryController.get.url))
      status(result) must be(SEE_OTHER)
    }

  }
}
