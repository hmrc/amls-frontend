package models.tcsp

import org.scalatestplus.play.PlaySpec
import play.api.data.mapping.{Path, Failure, Success}
import play.api.data.validation.ValidationError
import play.api.libs.json.{JsPath, JsError, Json}

class TrustOrCompanyServiceProvidersSpec extends PlaySpec {

  "TrustOrCompanyServiceProviders" must {

    val Services = TrustOrCompanyServiceProviders(Set(NomineeShareholdersProvider, TrusteeProvider, CompanyDirectorEtc(true, false)))

    "Form Validation" must {

      "read valid form data and return success" in {
        val model = Map(
          "serviceProviders[]" -> Seq("01", "02" ,"04"),
          "onlyOffTheShelfCompsSold" -> Seq("true"),
          "complexCorpStructureCreation" -> Seq("false")
        )

        TrustOrCompanyServiceProviders.formReads.validate(model) mustBe
        Success(Services)
      }

      "read invalid form data and return failure message for required fields" in {
        val model = Map(
          "serviceProviders[]" -> Seq("04"),
          "onlyOffTheShelfCompsSold" -> Seq(""),
          "complexCorpStructureCreation" -> Seq("")
        )

        TrustOrCompanyServiceProviders.formReads.validate(model) mustBe
          Failure(Seq((Path \ "onlyOffTheShelfCompsSold") -> Seq(ValidationError("error.required.tcsp.off.the.shelf.companies")),
            (Path \ "complexCorpStructureCreation") -> Seq(ValidationError("error.required.tcsp.complex.corporate.structures"))))
      }

      "return failure message when user has not selected any of the services" in {

        TrustOrCompanyServiceProviders.formReads.validate(Map.empty) mustBe
          Failure(Seq((Path \ "serviceProviders") -> Seq(ValidationError("error.required.tcsp.service.providers"))))
      }

      "return failure message when user has filled invalid data" in {

        val model = Map(
          "serviceProviders[]" -> Seq("01", "10")
        )

        TrustOrCompanyServiceProviders.formReads.validate(model) mustBe
          Failure(Seq((Path \ "serviceProviders") -> Seq(ValidationError("error.invalid"))))
      }

      "write correct data" in {

        TrustOrCompanyServiceProviders.formWrites.writes(Services) must be (Map("serviceProviders[]" -> Seq("01", "02" ,"04"),
          "onlyOffTheShelfCompsSold" -> Seq("true"),
          "complexCorpStructureCreation" -> Seq("false")))
      }
    }

    "Json Validation" must {

      "Read and Write Json valid data successfully" in {

        TrustOrCompanyServiceProviders.jsonReads.reads(Json.toJson(Services))
      }

      "throw error message on reading invalid data" in {

        Json.fromJson[TrustOrCompanyServiceProviders](Json.obj("serviceProviders" -> Seq("40"))) must
          be(JsError((JsPath \ "serviceProviders") \ "serviceProviders" -> ValidationError("error.invalid")))

      }
    }
  }
}



