package controllers.tradingpremises

import connectors.DataCacheConnector
import models.aboutthebusiness.AboutTheBusiness
import models.tradingpremises.{NonUKTradingPremises, TradingPremisesAddress, UKTradingPremises, YourTradingPremises}
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.data.mapping.Success
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

    "load a blank where are trading premises page when the user visits for the first time" in new Fixture {
      when(controller.dataCacheConnector.fetchDataShortLivedCache[YourTradingPremises](any())
        (any(), any(), any())).thenReturn(Future.successful(None))

      val result = controller.get()(request)
      status(result) must be(OK)
      contentAsString(result) must include (Messages("tradingpremises.yourtradingpremises.title"))

    }

    "successfully validate a UK Address" in new Fixture {

      val validUKAddress = Map(
      "isUK" -> Seq("true"),
      "addressLine1" -> Seq("Address Line 1"),
      "addressLine2" -> Seq("Address Line 2"),
      "addressLine3" -> Seq("Address Line 3"),
      "addressLine4" -> Seq("Address Line 4"),
      "postCode" -> Seq("NE98 1ZZ"),
      "country" ->  Seq("UK"))

      TradingPremisesAddress.formRule.validate(validUKAddress) must
      be(Success(UKTradingPremises("Address Line 1", "Address Line 2", Some("Address Line 3"), Some("Address Line 4"), Some("NE98 1ZZ"), "UK")))
  }

    "successfully validate a Non UK Address" in new Fixture {

      val nonValidUKAddress = Map(
        "isUK" -> Seq("false"),
        "addressLine1" -> Seq("Address Line 1"),
        "addressLine2" -> Seq("Address Line 2"),
        "addressLine3" -> Seq("Address Line 3"),
        "addressLine4" -> Seq("Address Line 4"),
        "postCode" -> Seq("226001"),
        "country" -> Seq("IN")
      )

      TradingPremisesAddress.formRule.validate(nonValidUKAddress) must
        be(Success(NonUKTradingPremises("Address Line 1", "Address Line 2", Some("Address Line 3"), Some("Address Line 4"), Some("226001"), "IN")))
    }

    "on post of the page with invalid data must reload the page" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody()
      when(controller.dataCacheConnector.fetchDataShortLivedCache[AboutTheBusiness](any())(any(),any(),any())).thenReturn(Future.successful(None))

      val result = controller.post()(newRequest)
      status(result) must be(BAD_REQUEST)

    }

  }

}
