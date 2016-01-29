package models.tradingpremises

import org.scalatest.{MustMatchers, WordSpec}
import play.api.libs.json._
import org.joda.time.LocalDate

class YourTradingPremisesSpec extends WordSpec with MustMatchers{
  "YourTradingPremises JSON serialisation" must {
    "Read details correctly from JSON" in {
      val input = Json.obj(
        "tradingName" -> "Test Business Name",
        "startOfTrading" -> "2014-04-03",
      "isUK" -> true,
      "tradingPremisesAddressLine1" -> "test Address Line 1",
      "tradingPremisesAddressLine2" -> "test Address Line 2",
      "tradingPremisesAddressLine3" -> "test Address Line 3",
      "tradingPremisesAddressLine4" -> "test Address Line 4",
      "postcode" -> "AA67 HJU",
      "isResidential" -> true)

      YourTradingPremises.jsonReadsYourTradingPremises.reads(input) must be (JsSuccess(
        YourTradingPremises(
          "Test Business Name",
          TradingPremisesAddressUK(
            "test Address Line 1",
            "test Address Line 2",
            Some("test Address Line 3"),
            Some("test Address Line 4"),
            "AA67 HJU"),
          new LocalDate(2014, 4, 3),
          ResidentialYes
        )
      ))
    }

    "Write details correctly to JSON" in {
      val input = YourTradingPremises(
        "Test Business Name",
        TradingPremisesAddressUK(
          "test Address Line 1",
          "test Address Line 2",
          Some("test Address Line 3"),
          Some("test Address Line 4"),
          "AA67 HJU"),
        new LocalDate(2014, 4, 3),
        ResidentialYes)

      YourTradingPremises.jsonWritesYourTradingPremises.writes(input) must be (Json.obj(
        "tradingName" -> "Test Business Name",
        "isUK" -> true,
        "tradingPremisesAddressLine1" -> "test Address Line 1",
        "tradingPremisesAddressLine2" -> "test Address Line 2",
        "tradingPremisesAddressLine3" -> "test Address Line 3",
        "tradingPremisesAddressLine4" -> "test Address Line 4",
        "postcode" -> "AA67 HJU",
        "startOfTrading" -> "2014-04-03",
        "isResidential" -> true))
    }
  }

  "TradingPremisesAddress Json Serialisation" when {
    "working with a UK Address" must {
      val addressObject = TradingPremisesAddressUK(
                            "test Address Line 1",
                            "test Address Line 2",
                            Some("test Address Line 3"),
                            Some("test Address Line 4"),
                            "AA67 HJU")

      val addressJson = Json.obj("isUK" -> true,
        "tradingPremisesAddressLine1" -> "test Address Line 1",
        "tradingPremisesAddressLine2" -> "test Address Line 2",
        "tradingPremisesAddressLine3" -> "test Address Line 3",
        "tradingPremisesAddressLine4" -> "test Address Line 4",
        "postcode" -> "AA67 HJU")

      "Read Address correctly" in {TradingPremisesAddress.jsonReadsTradingPremisesAddress.reads(addressJson) must be (JsSuccess(addressObject, JsPath \ "isUK"))}

      "Write address correctly" in {TradingPremisesAddress.jsonWritesTradingPremisesAddress.writes(addressObject) must be (addressJson)}
     }

    "working with a Non UK Address" must {
      val addressObject = TradingPremisesAddressNonUK(
        "test Address Line 1",
        "test Address Line 2",
        Some("test Address Line 3"),
        Some("test Address Line 4"),
        "Somalia")

      val addressJson = Json.obj("isUK" -> false,
        "tradingPremisesAddressLine1" -> "test Address Line 1",
        "tradingPremisesAddressLine2" -> "test Address Line 2",
        "tradingPremisesAddressLine3" -> "test Address Line 3",
        "tradingPremisesAddressLine4" -> "test Address Line 4",
        "country" -> "Somalia")

      "Read Address correctly" in {TradingPremisesAddress.jsonReadsTradingPremisesAddress.reads(addressJson) must be (JsSuccess(addressObject, JsPath \ "isUK"))}

      "Write address correctly" in {TradingPremisesAddress.jsonWritesTradingPremisesAddress.writes(addressObject) must be (addressJson)}
    }


  }


  "IsResidential JSON serialisation" must {
    "Read Yes correctly from JSON" in {
      val input = Json.obj("isResidential" -> true)

      IsResidential.jsonReadsIsResidential.reads(input) must be (JsSuccess(ResidentialYes, JsPath \ "isResidential"))
    }

    "Read No correctly from JSON" in {
      val input = Json.obj("isResidential" -> false)

      IsResidential.jsonReadsIsResidential.reads(input) must be (JsSuccess(ResidentialNo, JsPath \ "isResidential"))
    }

    "Write Yes as true" in {
      IsResidential.jsonWritesIsResidential.writes(ResidentialYes) must be (Json.obj("isResidential" -> true))
    }

    "Write No as false" in {
      IsResidential.jsonWritesIsResidential.writes(ResidentialNo) must be (Json.obj("isResidential" -> false))
    }
  }
}
