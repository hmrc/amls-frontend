package models.tradingpremises

import org.scalatest.{MustMatchers, WordSpec}
import play.api.libs.json._
import org.joda.time.LocalDate

class YourTradingPremisesSpec extends WordSpec with MustMatchers{
  "YourTradingPremises" must {
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
  }

  "TradingPremisesAddress" must {
    "Read UK Address correctly from JSON" in {
      val input = Json.obj("isUK" -> true,
          "tradingPremisesAddressLine1" -> "test Address Line 1",
          "tradingPremisesAddressLine2" -> "test Address Line 2",
          "tradingPremisesAddressLine3" -> "test Address Line 3",
          "tradingPremisesAddressLine4" -> "test Address Line 4",
          "postcode" -> "AA67 HJU")

      TradingPremisesAddress.jsonReadsTradingPremisesAddress.reads(input) must be (JsSuccess(
        TradingPremisesAddressUK(
          "test Address Line 1",
          "test Address Line 2",
          Some("test Address Line 3"),
          Some("test Address Line 4"),
          "AA67 HJU"),
          JsPath \ "isUK"))
    }

    "Read non UK Address correctly from JSON" in {
      val input = Json.obj("isUK" -> false,
        "tradingPremisesAddressLine1" -> "test Address Line 1",
        "tradingPremisesAddressLine2" -> "test Address Line 2",
        "tradingPremisesAddressLine3" -> "test Address Line 3",
        "tradingPremisesAddressLine4" -> "test Address Line 4",
        "country" -> "France")

      TradingPremisesAddress.jsonReadsTradingPremisesAddress.reads(input) must be (JsSuccess(
        TradingPremisesAddressNonUK(
          "test Address Line 1",
          "test Address Line 2",
          Some("test Address Line 3"),
          Some("test Address Line 4"),
          "France"),
          JsPath \ "isUK"))
    }
  }


  "IsResidential" must {
    "Read Yes correctly from JSON" in {
      val input = Json.obj("isResidential" -> true)

      IsResidential.jsonReadsIsResidential.reads(input) must be (JsSuccess(ResidentialYes, JsPath \ "isResidential"))
    }

    "Read No correctly from JSON" in {
      val input = Json.obj("isResidential" -> false)

      IsResidential.jsonReadsIsResidential.reads(input) must be (JsSuccess(ResidentialNo, JsPath \ "isResidential"))
    }
  }
}
