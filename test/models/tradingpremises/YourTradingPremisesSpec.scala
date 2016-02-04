package models.tradingpremises

import org.joda.time.LocalDate
import org.scalatest.{Ignore, MustMatchers, WordSpec}
import play.api.libs.json._

class YourTradingPremisesSpec extends WordSpec with MustMatchers {

  "YourTradingPremises JSON serialisation" must {

    //TODO To fix NullPointer Exception
/*
   "Read details correctly from JSON" in {

      val input = Json.parse(
        """{
          |"isUK":true,
          |"tradingName":"Test Business Name",
          |"addressLine1":"test Address Line 1",
          |"addressLine2":"test Address Line 2",
          |"addressLine3":"test Address Line 3",
          |"addressLine4":"test Address Line 4",
          |"postcode":"AA67 HJU",
          |"country":"UK",
          |"premiseOwner":false,
          |"startOfTradingDate":"2014-04-03",
          |"isResidential":true}""".stripMargin)

      val outputObj = YourTradingPremises(
        "Test Business Name",
        UKTradingPremises(
          "test Address Line 1",
          "test Address Line 2",
          Some("test Address Line 3"),
          Some("test Address Line 4"),
          Some("AA67 HJU"),
          "UK"),
        PremiseOwnerSelf,
        new LocalDate(2014, 4, 3),
        ResidentialYes)

      YourTradingPremises.jsonReadsYourTradingPremises.reads(input) must be(JsSuccess(outputObj))

    }
*/

    "Write details correctly to JSON including the LocalDate Conversion" in {
      val input = YourTradingPremises(
        "Test Business Name",
        UKTradingPremises(
          "test Address Line 1",
          "test Address Line 2",
          Some("test Address Line 3"),
          Some("test Address Line 4"),
          Some("AA67 HJU"),
          "UK"),
        PremiseOwnerAnother,
        new LocalDate(2014, 4, 3),
        ResidentialYes)

      YourTradingPremises.jsonWritesYourTradingPremises.writes(input) must be(Json.obj(
        "tradingName" -> "Test Business Name",
        "addressLine1" -> "test Address Line 1",
        "addressLine2" -> "test Address Line 2",
        "addressLine3" -> "test Address Line 3",
        "addressLine4" -> "test Address Line 4",
        "postcode" -> "AA67 HJU",
        "country" -> "UK",
        "premiseOwner" -> false,
        "startOfTradingDate" -> "2014-04-03",
        "isResidential" -> true))
    }
  }

  "TradingPremisesAddress Json Serialisation" when {

    "working with a UK Address" must {

      "Read UK Address correctly" in {

        val UKJsonToRead = Json.obj(
          "isUK" -> true,
          "addressLine1" -> "test Address Line 1",
          "addressLine2" -> "test Address Line 2",
          "addressLine3" -> "test Address Line 3",
          "addressLine4" -> "test Address Line 4",
          "postcode" -> "AA67 HJU",
          "country" -> "UK")

        val uKTradingPremises = UKTradingPremises(
          "test Address Line 1",
          "test Address Line 2",
          Some("test Address Line 3"),
          Some("test Address Line 4"),
          Some("AA67 HJU"),
          "UK")


        TradingPremisesAddress.jsonReadsTradingPremisesAddress.reads(UKJsonToRead) must be(JsSuccess(uKTradingPremises, JsPath))
      }

      "Write UK address correctly" in {

        val uKTradingPremises = UKTradingPremises(
          "test Address Line 1",
          "test Address Line 2",
          Some("test Address Line 3"),
          Some("test Address Line 4"),
          Some("AA67 HJU"),
          "UK")


        val UKJsontoWrite = Json.obj(
          "addressLine1" -> "test Address Line 1",
          "addressLine2" -> "test Address Line 2",
          "addressLine3" -> "test Address Line 3",
          "addressLine4" -> "test Address Line 4",
          "postcode" -> "AA67 HJU",
          "country" -> "UK")

        TradingPremisesAddress.jsonWritesTradingPremisesAddress.writes(uKTradingPremises) must be(UKJsontoWrite)
      }
    }

    "working with a Non UK Address" must {
      val nonUKTradingPremises = UKTradingPremises(
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

      "Read Non UK Address correctly" in {
        TradingPremisesAddress.jsonReadsTradingPremisesAddress.reads(nonUKJson) must be(JsSuccess(nonUKTradingPremises, JsPath))
      }

      "Write Non UK address correctly" in {
        TradingPremisesAddress.jsonWritesTradingPremisesAddress.writes(nonUKTradingPremises) must be(nonUKJson)
      }
    }

  }


  "Json String must successfully convert to LocalDate" must {

    "Read the JSON String and convert to LocalDate" in {
      val jsonLocalDate: JsObject = Json.obj("startOfTradingDate" -> "2015-08-15")
      YourTradingPremises.readsJSONStringToLocalDate.reads(jsonLocalDate) must be(JsSuccess(LocalDate.parse("2015-08-15"), JsPath \ "startOfTradingDate"))
    }

    "Write the LocalDate to JSON String" in {
      val localDate: LocalDate = LocalDate.parse("2015-08-15")
      YourTradingPremises.writeLocalDateToJSONString.writes(localDate) must be(Json.obj("startOfTradingDate" -> "2015-08-15"))
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
