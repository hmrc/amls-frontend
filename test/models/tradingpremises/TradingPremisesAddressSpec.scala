package models.tradingpremises

import connectors.DataCacheConnector
import controllers.tradingpremises.WhereAreTradingPremisesController
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.data.mapping.Success
import play.api.libs.json.{JsPath, JsSuccess, Json}
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
      "postcode" -> Seq("NE98 1ZZ"),
      "country" -> Seq("UK"))

    TradingPremisesAddress.formRuleAddress.validate(validUKAddress) must
      be(Success(UKAddress("Address Line 1", "Address Line 2", Some("Address Line 3"), Some("Address Line 4"), Some("NE98 1ZZ"), "UK")))
  }

  "successfully validate FormRule URLEncoded NONUK Address" in new Fixture {

    val nonValidUKAddress = Map(
      "addressLine1" -> Seq("Address Line 1"),
      "addressLine2" -> Seq("Address Line 2"),
      "addressLine3" -> Seq("Address Line 3"),
      "addressLine4" -> Seq("Address Line 4"),
      "postcode" -> Seq("226001"),
      "country" -> Seq("IN")
    )

    TradingPremisesAddress.formRuleAddress.validate(nonValidUKAddress) must
      be(Success(UKAddress("Address Line 1", "Address Line 2", Some("Address Line 3"), Some("Address Line 4"), Some("226001"), "IN")))
  }


  "For UKTradingPremises" when {

    "working with a UK Address" must {

      val jsonUKTradingPremises = Json.obj(
        "addressLine1" -> "test Address Line 1",
        "addressLine2" -> "test Address Line 2",
        "addressLine3" -> "test Address Line 3",
        "addressLine4" -> "test Address Line 4",
        "postcode" -> "AA67 HJU",
        "country" -> "UK")

      val ukTradingPremises = UKAddress(
        "test Address Line 1",
        "test Address Line 2",
        Some("test Address Line 3"),
        Some("test Address Line 4"),
        Some("AA67 HJU"),
        "UK")

      "JSON READS must match all TradingPremisesAddress fields" in {
        TradingPremisesAddress.jsonReadsAddress.reads(jsonUKTradingPremises) must be(JsSuccess(ukTradingPremises, JsPath))
      }

      "JSON WRITES must match all TradingPremisesAddress fields" in {
        TradingPremisesAddress.jsonWritesAddress.writes(ukTradingPremises) must be(jsonUKTradingPremises)
      }

      val urlFormEncodedAddress = Map(
        "addressLine1" -> Seq("test Address Line 1"),
        "addressLine2" -> Seq("test Address Line 2"),
        "addressLine3" -> Seq("test Address Line 3"),
        "addressLine4" -> Seq("test Address Line 4"),
        "postcode" -> Seq("AA67 HJU"),
        "country" -> Seq("UK")
      )

      "FORM RULE validate the fields FROM the Form" in {
        TradingPremisesAddress.formRuleAddress.validate(urlFormEncodedAddress) must be(Success(ukTradingPremises))
      }

      "FORM WRITE populates the fields TO the Form" in {
        TradingPremisesAddress.formWritesAddress.writes(ukTradingPremises) must be(urlFormEncodedAddress)
      }

    }

    "working with a Non UK Address" must {

      val nonUKTradingPremises = UKAddress(
        "test Address Line 1",
        "test Address Line 2",
        Some("test Address Line 3"),
        Some("test Address Line 4"),
        Some("226007"),
        "IN")

      val nonUKJson = Json.obj(
        "addressLine1" -> "test Address Line 1",
        "addressLine2" -> "test Address Line 2",
        "addressLine3" -> "test Address Line 3",
        "addressLine4" -> "test Address Line 4",
        "postcode" -> "226007",
        "country" -> "IN")

      "READS the JSON for NonUK Address correctly" in {
        TradingPremisesAddress.jsonReadsAddress.reads(nonUKJson) must be(JsSuccess(nonUKTradingPremises, JsPath))
      }

      "WRITES the JSON for NonUK Address correctly" in {
        TradingPremisesAddress.jsonWritesAddress.writes(nonUKTradingPremises) must be(nonUKJson)
      }
    }

  }

}
