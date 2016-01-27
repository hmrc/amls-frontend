package models.tradingpremises

import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.data.mapping.{Failure, Path, Success}
import play.api.data.validation.ValidationError
import play.api.libs.json._

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

    import play.api.libs.json._, play.api.data.mapping._
    import play.api.libs.json._
    import play.api.data.mapping._

    "successfully validate given an input data" in {
      YourAgent.formRule.validate(inputData1) must be(Success(yourAgent1))
    }

    "write correct data from object to Form(UrlFormEncoded) object" in {
      YourAgent.formWrites.writes(yourAgent1) must be(inputData1)
      YourAgent.formWrites.writes(yourAgent2) must be(inputData2)
    }
  }

  "JSON validation" must {

    "successfully validate json to object conversion" in {

      val agentJObj = Json.obj("agentsRegisteredName" -> "STUDENT", "taxType" -> "01", "agentsBusinessStructure"-> "01")
      val jVal =  Json.fromJson[YourAgent](agentJObj)
      jVal must be(JsSuccess(yourAgent1, JsPath))
    }

    "successfully validate given an json value" in {

      val rn = Json.obj("agentsRegisteredName" -> "STUDENT")
      val tt = Json.obj("taxType" -> "01")
      val bs = Json.obj("agentsBusinessStructure"-> "01")

      val jVal1 =  Json.fromJson[AgentsRegisteredName](rn)
      jVal1 must be(JsSuccess(AgentsRegisteredName("STUDENT"), JsPath \ "agentsRegisteredName"))

      val jVal2 =  Json.fromJson[TaxType](tt)
      jVal2 must be(JsSuccess(TaxTypeSelfAssesment, JsPath \ "taxType"))

      val jVal3 =  Json.fromJson[BusinessStructure](bs)
      jVal3 must be(JsSuccess(SoleProprietor, JsPath \ "agentsBusinessStructure"))

    }
  }

  "successfully convert object to json " in {
    val agentJObj = Json.obj("agentsRegisteredName" -> "STUDENT", "taxType" -> "01", "agentsBusinessStructure"-> "01")
    Json.toJson[YourAgent](yourAgent1) must be(agentJObj)
  }

  }
