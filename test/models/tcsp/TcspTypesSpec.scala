package models.tcsp

import org.scalatestplus.play.PlaySpec
import jto.validation.{Path, Failure, Success}
import jto.validation.ValidationError
import play.api.libs.json.{JsSuccess, JsPath, JsError, Json}

class TcspTypesSpec extends PlaySpec {

  "TrustOrCompanyServiceProviders" must {

    val Services = TcspTypes(Set(NomineeShareholdersProvider, TrusteeProvider, RegisteredOfficeEtc, CompanyDirectorEtc, CompanyFormationAgent(true, false)))

    "Form Validation" must {

      "read valid form data and return success" in {
        val model = Map(
          "serviceProviders[]" -> Seq("01", "02","03" ,"04", "05"),
          "onlyOffTheShelfCompsSold" -> Seq("true"),
          "complexCorpStructureCreation" -> Seq("false")
        )

        TcspTypes.formReads.validate(model) mustBe
        Success(Services)
      }

      "read invalid form data and return failure message for required fields" in {
        val model = Map(
          "serviceProviders[]" -> Seq("05"),
          "onlyOffTheShelfCompsSold" -> Seq(""),
          "complexCorpStructureCreation" -> Seq("")
        )

        TcspTypes.formReads.validate(model) mustBe
          Failure(Seq((Path \ "onlyOffTheShelfCompsSold") -> Seq(ValidationError("error.required.tcsp.off.the.shelf.companies")),
            (Path \ "complexCorpStructureCreation") -> Seq(ValidationError("error.required.tcsp.complex.corporate.structures"))))
      }

      "return failure message when user has not selected any of the services" in {

        TcspTypes.formReads.validate(Map.empty) mustBe
          Failure(Seq((Path \ "serviceProviders") -> Seq(ValidationError("error.required.tcsp.service.providers"))))
      }

      "return failure message when user has filled invalid data" in {

        val model = Map(
          "serviceProviders[]" -> Seq("01", "10")
        )

        TcspTypes.formReads.validate(model) mustBe
          Failure(Seq((Path \ "serviceProviders") -> Seq(ValidationError("error.invalid"))))
      }

      "write correct data" in {
        val model = TcspTypes(Set(RegisteredOfficeEtc, CompanyDirectorEtc, CompanyFormationAgent(true, false)))

        TcspTypes.formWrites.writes(model) mustBe Map("serviceProviders[]" -> Seq("03" ,"04", "05"),
          "onlyOffTheShelfCompsSold" -> Seq("true"),
          "complexCorpStructureCreation" -> Seq("false"))
      }

      "write correct data1" in {
        val model = TcspTypes(Set(NomineeShareholdersProvider, TrusteeProvider))
        TcspTypes.formWrites.writes(model) mustBe Map("serviceProviders[]" -> Seq("01" ,"02"))
      }
    }

    "Json Validation" must {

      "successfully validate given values with option CompanyDirectorEtc" in {
        val json =  Json.obj(
          "serviceProviders" -> Seq("01","02","03","04", "05"),
          "onlyOffTheShelfCompsSold" -> true,
          "complexCorpStructureCreation" -> false
        )

        Json.fromJson[TcspTypes](json) must
          be(JsSuccess(Services, JsPath \ "serviceProviders"))
      }

      "Read and Write Json valid data successfully" in {

        TcspTypes.jsonReads.reads(Json.toJson(Services))
      }

      "throw error message on reading invalid data" in {

        Json.fromJson[TcspTypes](Json.obj("serviceProviders" -> Seq("40"))) must
          be(JsError((JsPath \ "serviceProviders") \ "serviceProviders" -> ValidationError("error.invalid")))

      }
    }
  }
}



