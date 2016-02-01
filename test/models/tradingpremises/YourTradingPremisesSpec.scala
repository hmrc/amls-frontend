package models.tradingpremises

import org.joda.time.LocalDate
import org.scalatest.{MustMatchers, WordSpec}
import play.api.libs.json._

class YourTradingPremisesSpec extends WordSpec with MustMatchers {
  "YourTradingPremises JSON serialisation" must {
    "Read details correctly from JSON" in {
      val input = Json.obj(
        "tradingName" -> "Test Business Name",
        "startOfTrading" -> "2014-04-03",
        "premiseOwner" -> false,
        "isUK" -> true,
        "tradingPremisesAddressLine1" -> "test Address Line 1",
        "tradingPremisesAddressLine2" -> "test Address Line 2",
        "tradingPremisesAddressLine3" -> "test Address Line 3",
        "tradingPremisesAddressLine4" -> "test Address Line 4",
        "postcode" -> "AA67 HJU",
        "country" -> "UK",
        "isResidential" -> true)

      val ukPremise = UKTradingPremises(
        "test Address Line 1",
        "test Address Line 2",
        Some("test Address Line 3"),
        Some("test Address Line 4"),
        Some("AA67 HJU"),
        "UK")

      val outputObj = YourTradingPremises(
        "Test Business Name",
        ukPremise,
        PremiseOwnerSelf,
        new LocalDate(2014, 4, 3),
        ResidentialYes)

      YourTradingPremises.jsonReadsYourTradingPremises.reads(input) must be(JsSuccess(outputObj))

    }

    "Write details correctly to JSON" in {
      val input = YourTradingPremises(
        "Test Business Name",
        UKTradingPremises(
          "test Address Line 1",
          "test Address Line 2",
          Some("test Address Line 3"),
          Some("test Address Line 4"),
          Some("AA67 HJU"), "UK"),
        PremiseOwnerAnother,
        new LocalDate(2014, 4, 3),
        ResidentialYes)

      YourTradingPremises.jsonWritesYourTradingPremises.writes(input) must be(Json.obj(
        "tradingName" -> "Test Business Name",
        "isUK" -> true,
        "tradingPremisesAddressLine1" -> "test Address Line 1",
        "tradingPremisesAddressLine2" -> "test Address Line 2",
        "tradingPremisesAddressLine3" -> "test Address Line 3",
        "tradingPremisesAddressLine4" -> "test Address Line 4",
        "postcode" -> "AA67 HJU",
        "premiseOwner" -> true,
        "startOfTrading" -> "2014-04-03",
        "isResidential" -> true))
    }
  }

  "TradingPremisesAddress Json Serialisation" when {
    "working with a UK Address" must {
      val addressObject = UKTradingPremises(
        "test Address Line 1",
        "test Address Line 2",
        Some("test Address Line 3"),
        Some("test Address Line 4"),
        Some("AA67 HJU"), "UK")

      val addressJson = Json.obj("isUK" -> true,
        "tradingPremisesAddressLine1" -> "test Address Line 1",
        "tradingPremisesAddressLine2" -> "test Address Line 2",
        "tradingPremisesAddressLine3" -> "test Address Line 3",
        "tradingPremisesAddressLine4" -> "test Address Line 4",
        "postcode" -> "AA67 HJU")

      "Read Address correctly" in {
        TradingPremisesAddress.jsonReadsTradingPremisesAddress.reads(addressJson) must be(JsSuccess(addressObject, JsPath \ "isUK"))
      }

      "Write address correctly" in {
        TradingPremisesAddress.jsonWritesTradingPremisesAddress.writes(addressObject) must be(addressJson)
      }
    }

    "working with a Non UK Address" must {
      val addressObject = NonUKTradingPremises(
        "test Address Line 1",
        "test Address Line 2",
        Some("test Address Line 3"),
        Some("test Address Line 4"),
        Some("560093"), "IN")

      val addressJson = Json.obj("isUK" -> false,
        "tradingPremisesAddressLine1" -> "test Address Line 1",
        "tradingPremisesAddressLine2" -> "test Address Line 2",
        "tradingPremisesAddressLine3" -> "test Address Line 3",
        "tradingPremisesAddressLine4" -> "test Address Line 4",
        "country" -> "Somalia")

      "Read Address correctly" in {
        TradingPremisesAddress.jsonReadsTradingPremisesAddress.reads(addressJson) must be(JsSuccess(addressObject, JsPath \ "isUK"))
      }

      "Write address correctly" in {
        TradingPremisesAddress.jsonWritesTradingPremisesAddress.writes(addressObject) must be(addressJson)
      }
    }

  }


  "IsResidential JSON serialisation" must {
    "Read Yes correctly from JSON" in {
      val input = Json.obj("isResidential" -> true)

      IsResidential.jsonReadsIsResidential.reads(input) must be(JsSuccess(ResidentialYes, JsPath \ "isResidential"))
    }

    "Read No correctly from JSON" in {
      val input = Json.obj("isResidential" -> false)

      IsResidential.jsonReadsIsResidential.reads(input) must be(JsSuccess(ResidentialNo, JsPath \ "isResidential"))
    }

    "Write Yes as true" in {
      IsResidential.jsonWritesIsResidential.writes(ResidentialYes) must be(Json.obj("isResidential" -> true))
    }

    "Write No as false" in {
      IsResidential.jsonWritesIsResidential.writes(ResidentialNo) must be(Json.obj("isResidential" -> false))
    }
  }
}
