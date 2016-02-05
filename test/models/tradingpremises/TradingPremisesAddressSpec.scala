package models.tradingpremises

import connectors.DataCacheConnector
import controllers.tradingpremises.WhereAreTradingPremisesController
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.data.mapping.Success
import utils.AuthorisedFixture

class TradingPremisesAddressSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with ScalaFutures {

  trait Fixture extends AuthorisedFixture {
    self =>
    val controller = new WhereAreTradingPremisesController() {
      override val dataCacheConnector = mock[DataCacheConnector]
      override val authConnector = self.authConnector
    }
  }

  "successfully validate FormRule URLEncoded UK Address" in new Fixture {

    val validUKAddress = Map(
      "addressLine1" -> Seq("Address Line 1"),
      "addressLine2" -> Seq("Address Line 2"),
      "addressLine3" -> Seq("Address Line 3"),
      "addressLine4" -> Seq("Address Line 4"),
      "postCode" -> Seq("NE98 1ZZ"),
      "country" -> Seq("UK"))

    TradingPremisesAddress.formRuleTradingPremiseAddress.validate(validUKAddress) must
      be(Success(UKTradingPremises("Address Line 1", "Address Line 2", Some("Address Line 3"), Some("Address Line 4"), Some("NE98 1ZZ"), "UK")))
  }

  "successfully validate FormRule URLEncoded NONUK Address" in new Fixture {

    val nonValidUKAddress = Map(
      "addressLine1" -> Seq("Address Line 1"),
      "addressLine2" -> Seq("Address Line 2"),
      "addressLine3" -> Seq("Address Line 3"),
      "addressLine4" -> Seq("Address Line 4"),
      "postCode" -> Seq("226001"),
      "country" -> Seq("IN")
    )

    TradingPremisesAddress.formRuleTradingPremiseAddress.validate(nonValidUKAddress) must
      be(Success(UKTradingPremises("Address Line 1", "Address Line 2", Some("Address Line 3"), Some("Address Line 4"), Some("226001"), "IN")))
  }

}
