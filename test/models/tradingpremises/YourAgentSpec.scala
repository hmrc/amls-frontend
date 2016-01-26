package models.tradingpremises

import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.data.mapping.{Failure, Path, Success}
import play.api.data.validation.ValidationError
import play.api.libs.json.{JsError, JsPath, JsSuccess, Json}

class YourAgentSpec extends PlaySpec with MockitoSugar {


  val yourAgent1 = YourAgent(AgentsRegisteredName("STUDENT"), TaxTypeSelfAssesment, SoleProprietor)
  val yourAgent2 = YourAgent(AgentsRegisteredName("XYZ"), TaxTypeSelfAssesment, LimitedLiabilityPartnership)
  val yourAgent3 = YourAgent(AgentsRegisteredName("CTU"), TaxTypeCorporationTax, UnincorporatedBody)

  val inputData1 = Map("agentsRegisteredName" -> Seq("STUDENT"),
                       "taxType" -> Seq("01"),
                       "agentsBusinessStructure"-> Seq("01"))

  val inputData2 = Map("agentsRegisteredName" -> Seq("XYZ"),
    "taxType" -> Seq("01"),
    "agentsBusinessStructure" -> Seq("02"))

  val inputData3 = Map("agentsRegisteredName" -> Seq("CTU"),
    "taxType" -> Seq("02"),
    "agentsBusinessStructure"-> Seq("05"))


  "Form Validation" must {

    "successfully validate given an enum value" in {
      YourAgent.formRule.validate(inputData1) must be(Success(yourAgent1))
    }

//    "write correct data from enum value" in {
//      YourAgent.formWrites.writes(yourAgent1) must be(inputData1)
//      YourAgent.formWrites.writes(yourAgent2) must be(inputData2)
//    }
  }

//  "JSON validation" must {
//
//    "successfully validate given an enum value" in {
//
//      val agentJObj = Json.obj("agentsRegisteredName" -> "STUDENT", "taxType" -> "01", "agentsBusinessStructure"-> "01")
//
//
//      println("=====------======----- " +agentJObj)
//
//      val jVal =  Json.fromJson[YourAgent](agentJObj)
//
//      println("------------------- " +jVal)
//
//      Json.fromJson[YourAgent](agentJObj) must
//        be(JsSuccess(YourAgent, JsPath \ "taxType"))
//
//    }
//  }

  }
