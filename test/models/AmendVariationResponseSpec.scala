package models


import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsSuccess, Json}

class AmendVariationResponseSpec extends PlaySpec {

  val response = AmendVariationResponse("pdate", "12345", 115.0, Some(125.0), Some(115.0), 0, None, 240.0, Some("ref"), None)

  "AmendVariationResponse" must {

    "Deserialize correctly where Fit and Proper Fee is fPFee" in {

      val json =
        """{
  "processingDate" : "pdate",
  "etmpFormBundleNumber" : "12345",
  "registrationFee" : 115.0,
  "fPFee" : 125.0,
  "fpFeeRate" : 115.0,
  "premiseFee" : 0,
  "totalFees" : 240.0,
  "paymentReference" : "ref",
  "addedResponsiblePeople" : 0,
  "addedResponsiblePeopleFitAndProper" : 0,
  "addedFullYearTradingPremises" : 0,
  "halfYearlyTradingPremises" : 0,
  "zeroRatedTradingPremises" : 0
}"""

      AmendVariationResponse.reads.reads(Json.parse(json)) must be(JsSuccess(response))

    }


    "Deserialize correctly where Fit and Proper Fee is fpFee" in {

      val json =
        """{
  "processingDate" : "pdate",
  "etmpFormBundleNumber" : "12345",
  "registrationFee" : 115.0,
  "fpFee" : 125.0,
  "premiseFee" : 0,
  "totalFees" : 240.0,
  "paymentReference" : "ref",
  "addedResponsiblePeople" : 0,
  "addedResponsiblePeopleFitAndProper" : 0,
  "addedFullYearTradingPremises" : 0,
  "halfYearlyTradingPremises" : 0,
  "zeroRatedTradingPremises" : 0
}"""

      AmendVariationResponse.reads.reads(Json.parse(json)) must be(JsSuccess(response.copy(fpFeeRate = None)))

    }

    "Deserialize correctly where Fit and Proper Fee is not returned" in {

      val json =
        """{
  "processingDate" : "pdate",
  "etmpFormBundleNumber" : "12345",
  "registrationFee" : 115.0,
  "premiseFee" : 0,
  "totalFees" : 240.0,
  "paymentReference" : "ref",
  "addedResponsiblePeople" : 0,
  "addedResponsiblePeopleFitAndProper" : 0,
  "addedFullYearTradingPremises" : 0,
  "halfYearlyTradingPremises" : 0,
  "zeroRatedTradingPremises" : 0
}"""

      AmendVariationResponse.reads.reads(Json.parse(json)) must be(JsSuccess(response.copy(fpFee = None, fpFeeRate = None)))

    }
  }

}
