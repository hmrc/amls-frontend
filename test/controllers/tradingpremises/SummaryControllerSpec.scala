package controllers.tradingpremises

import java.util.UUID

import connectors.DataCacheConnector
import models.tradingpremises.TradingPremises
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.mvc.Result
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
      when(mockDataCacheConnector.fetchDataShortLivedCache[Seq[TradingPremises]](any())
        (any(), any(), any())).thenReturn(Future.successful(Some(Seq(model))))
      val result = summaryController.get()(request)
      status(result) must be(OK)
    }

    "load the pre page when the model is not present" in new Fixture {

      when(mockDataCacheConnector.fetchDataShortLivedCache[Seq[TradingPremises]](any())
        (any(), any(), any())).thenReturn(Future.successful(None))

      val result: Future[Result] = summaryController.get()(request)

      println(contentAsString(result))


      status(result) must be(SEE_OTHER)

    }

  }

}
