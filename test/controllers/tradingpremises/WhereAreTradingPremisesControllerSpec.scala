package controllers.tradingpremises

import connectors.DataCacheConnector
import models.tradingpremises._
import org.joda.time.LocalDate
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.i18n.Messages
import play.api.test.Helpers._
import utils.AuthorisedFixture

import scala.concurrent.Future

class WhereAreTradingPremisesControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with ScalaFutures {

  trait Fixture extends AuthorisedFixture {
    self =>
    val controller = new WhereAreTradingPremisesController() {
      override val dataCacheConnector = mock[DataCacheConnector]
      override val authConnector = self.authConnector
    }
  }

  "WhereAreTradingPremisesController" must {

    val ukAddress = Address("ukline_1", "ukline_2", Some("ukline_3"), Some("ukline_4"), "NE98 1ZZ")
    val yourTradingPremises = YourTradingPremises("Trading Name", ukAddress, true, new LocalDate("2015-04-03"), true)

    "load a blank where are trading premises page when the user visits for the first time" in new Fixture {
      when(controller.dataCacheConnector.fetchDataShortLivedCache[TradingPremises](any())
        (any(), any(), any())).thenReturn(Future.successful(None))

      val result = controller.get()(request)
      status(result) must be(OK)
      contentAsString(result) must include(Messages("tradingpremises.yourtradingpremises.title"))

    }


    "prepopulate the data when the trading premises page loads" in new Fixture {

      val tradingPremises = TradingPremises(Some(yourTradingPremises), None)

      when(controller.dataCacheConnector.fetchDataShortLivedCache[TradingPremises](any())
        (any(), any(), any())).thenReturn(Future.successful(Some(tradingPremises)))

      val result = controller.get()(request)
      status(result) must be(OK)

      contentAsString(result) must include(yourTradingPremises.tradingName)

      //Validate that address gets loaded
      contentAsString(result) must include(Messages("tradingpremises.yourtradingpremises.title"))
      contentAsString(result) must include(ukAddress.addressLine1)
      contentAsString(result) must include(ukAddress.addressLine2)
      contentAsString(result) must include(ukAddress.addressLine3.get)
      contentAsString(result) must include(ukAddress.addressLine4.get)
    }

    "on post of the page with valid data must load the next page" in new Fixture {

      val tradingPremises = TradingPremises(Some(yourTradingPremises), None)

      val newRequest = request.withFormUrlEncodedBody(
        "tradingName" -> "Trading Name",
        "addressLine1" -> "Address Line 1",
        "addressLine2" -> "Address Line 2",
        "premiseOwner" -> "true",
        "startOfTradingDate" -> "01-02-2016",
        "isResidential" -> "true"
      )

      when(controller.dataCacheConnector.fetchDataShortLivedCache[TradingPremises](any())(any(), any(), any()))
        .thenReturn(Future.successful(Some(tradingPremises)))

      val futureResult = controller.post()(newRequest)

      //TODO This will change when the next page is ready
      contentAsString(futureResult) must include(yourTradingPremises.tradingName)
      contentAsString(futureResult) must include(Messages("tradingpremises.yourtradingpremises.title"))
      contentAsString(futureResult) must include("Address Line 1")
      contentAsString(futureResult) must include("Address Line 2")
    }


    "on post of the page with invalid data must reload the page" in new Fixture {

      val tradingPremises = TradingPremises(Some(yourTradingPremises), None)

      val postRequestWithDataPopulated = request.withFormUrlEncodedBody(
        "tradingName" -> "Test Business Name",
        "addressLine1" -> "test Address Line 1",
        "addressLine2" -> "test Address Line 2",
        "addressLine3" -> "test Address Line 3",
        "addressLine4" -> "test Address Line 4",
        "postcode" -> "AA67 HJU",
        "country" -> "UK",
        "premiseOwner" -> "false",
        "startOfTradingDate" -> "3-4-2015",
        "isResidential" -> "true"
      )

      when(controller.dataCacheConnector.fetchDataShortLivedCache[TradingPremises](any())(any(), any(), any()))
        .thenReturn(Future.successful(Some(tradingPremises)))

      val result = controller.post()(postRequestWithDataPopulated)
      status(result) must be(BAD_REQUEST)
    }

  }
}
