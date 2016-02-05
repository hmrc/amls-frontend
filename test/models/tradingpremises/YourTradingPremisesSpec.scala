package models.tradingpremises

import models.FormTypes
import org.joda.time.LocalDate
import org.scalatest.{MustMatchers, WordSpec}
import play.api.data.mapping.{Path, Failure, Success}
import play.api.data.validation.ValidationError
import play.api.libs.json._

class YourTradingPremisesSpec extends WordSpec with MustMatchers {

  "For YourTradingPremises" must {

    val jsonReadsOrWritesMongo = Json.parse(
      """{
        |"tradingName":"Test Business Name",
        |"addressLine1":"test Address Line 1",
        |"addressLine2":"test Address Line 2",
        |"addressLine3":"test Address Line 3",
        |"addressLine4":"test Address Line 4",
        |"postcode":"AA67 HJU",
        |"country":"UK",
        |"premiseOwner":false,
        |"startOfTradingDate":"2015-08-15",
        |"isResidential":true}""".stripMargin)

    val yourTradingPremises = YourTradingPremises(
      "Test Business Name",
      UKAddress(
        "test Address Line 1",
        "test Address Line 2",
        Some("test Address Line 3"),
        Some("test Address Line 4"),
        Some("AA67 HJU"),
        "UK"),
      PremiseOwnerAnother,
      HMRCLocalDate("2015", "08", "15"), //new LocalDate(2014, 4, 3),
      ResidentialYes)

    "JSON READS must includes all fields" in {
      YourTradingPremises.jsonReadsYourTradingPremises.reads(jsonReadsOrWritesMongo) must be(JsSuccess(yourTradingPremises))
    }

    "JSON WRITES includes the Date Formed" in {
      YourTradingPremises.jsonWritesYourTradingPremises.writes(yourTradingPremises) must be(jsonReadsOrWritesMongo)
    }

    val urlFormEncodedYourTradingPremises = Map(
      "tradingName" -> Seq("Test Business Name"),
      "addressLine1" -> Seq("test Address Line 1"),
      "addressLine2" -> Seq("test Address Line 2"),
      "addressLine3" -> Seq("test Address Line 3"),
      "addressLine4" -> Seq("test Address Line 4"),
      "postcode" -> Seq("AA67 HJU"),
      "country" -> Seq("UK"),
      "premiseOwner" -> Seq("false"),
      "yyyy" -> Seq("2015"),
      "mm" -> Seq("08"),
      "dd" -> Seq("15"),
      "isResidential" -> Seq("true"))


    "FORM RULE validates the Fields FROM the Form" in {
      YourTradingPremises.formRuleYourTradingPremises.validate(urlFormEncodedYourTradingPremises) must be(Success(yourTradingPremises))
    }

    "FORM WRITE populates all the Fields TO the Form" in {
      YourTradingPremises.formWriteYourTradingPremises.writes(yourTradingPremises) must be(urlFormEncodedYourTradingPremises)
    }
  }


  "JSON and HMRCLocalDate serialisation" must {

    "JSON READ convert to Domain" in {
      val jsonLocalDate: JsObject = Json.obj("startOfTradingDate" -> "2015-08-15")
      HMRCLocalDate.jsonReadsHMRCLocalDate.reads(jsonLocalDate) must be(JsSuccess(HMRCLocalDate("2015", "08", "15"), JsPath \ "startOfTradingDate"))
    }

    "JSON WRITE the LocalDate to JSON String" in {
      val hmrcLocalDate = HMRCLocalDate("2015", "08", "15")
      HMRCLocalDate.jsonWritesHMRCLocalDate.writes(hmrcLocalDate) must be(Json.obj("startOfTradingDate" -> "2015-08-15"))
    }

  }

  "FORM and HMRCLocalDate conversion. Must successfully" must {

    "FORM RULE String and convert to LocalDate" in {
      val formDateField = Map(
        "yyyy" -> Seq("2015"),
        "mm" -> Seq("08"),
        "dd" -> Seq("15")
      )
      HMRCLocalDate.formRuleHMRCLocalDate.validate(formDateField) must be(Success(HMRCLocalDate("2015", "08", "15")))
    }

    "FORM WRITE the LocalDate to Form" in {
      val hmrcLocalDate = HMRCLocalDate("2015", "08", "15")
      HMRCLocalDate.formWriteHMRCLocalDate.writes(hmrcLocalDate) must be(Map(
        "yyyy" -> Seq("2015"),
        "mm" -> Seq("08"),
        "dd" -> Seq("15")
      ))
    }

  }


  "premiseOwner serialisation" must {

    "JSON READ Yes correctly from JSON" in {
      val input = Json.obj("premiseOwner" -> true)
      PremiseOwner.jsonReadsPremiseOwner.reads(input) must be(JsSuccess(PremiseOwnerSelf, JsPath \ "premiseOwner"))
    }

    "JSON WRITE No as false" in {
      PremiseOwner.jsonWritesPremiseOwner.writes(PremiseOwnerAnother) must be(Json.obj("premiseOwner" -> false))
    }

    "FORM RULE validate and create the Domain" in {
      val urlFormEncoded = Map(
        "premiseOwner" -> Seq("true")
      )
      PremiseOwner.formRulePremiseOwner.validate(urlFormEncoded) must be(Success(PremiseOwnerSelf))
    }

    "FORM WRITE create the Form Field" in {
      val urlFormEncoded = Map("" +
        "premiseOwner" -> Seq("false")
      )
      PremiseOwner.formWritePremiseOwner.writes(PremiseOwnerAnother) must be(urlFormEncoded)
    }
  }


  "IsResidential serialisation" must {

    "JSON READ Yes correctly from JSON" in {
      val input = Json.obj("isResidential" -> true)
      IsResidential.jsonReadsIsResidential.reads(input) must be(JsSuccess(ResidentialYes, JsPath \ "isResidential"))
    }

    "JSON WRITE No as false" in {
      IsResidential.jsonWritesIsResidential.writes(ResidentialNo) must be(Json.obj("isResidential" -> false))
    }

    "FORM RULE validate and create the Domain" in {
      val urlFormEncoded = Map(
        "isResidential" -> Seq("true")
      )
      IsResidential.formRuleIsResidential.validate(urlFormEncoded) must be(Success(ResidentialYes))
    }

    "FORM WRITE create the Form Field" in {
      val urlFormEncoded = Map("" +
        "isResidential" -> Seq("false")
      )
      IsResidential.formWriteIsResidential.writes(ResidentialNo) must be(urlFormEncoded)
    }

  }

}
