package models.tradingpremises

import play.api.libs.json.Json
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import jto.validation.{Invalid, Path, Valid}
import jto.validation.ValidationError
import play.api.libs.json.JsSuccess

class AgentCompanyDetailsSpec extends PlaySpec with OneAppPerSuite{

  "AgentCompanyDetails" must {

    "validate form Read" in {
      val formInput = Map("agentCompanyName" -> Seq("sometext"), "companyRegistrationNumber" -> Seq("12345678"))
      AgentCompanyDetails.formReads.validate(formInput) must be(Valid(AgentCompanyDetails("sometext", Some("12345678"))))
    }

    "throw error" when {

      "name field is missing" in {
        val formInput = Map("agentCompanyName" -> Seq(""), "companyRegistrationNumber" -> Seq("12345678"))
        AgentCompanyDetails.formReads.validate(formInput) must be(Invalid(Seq((
          Path \ "agentCompanyName",
          Seq(ValidationError("error.required.tp.agent.registered.company.name"))
        ))))
      }

      "crn field is missing" in {
        val formInput = Map("agentCompanyName" -> Seq("sometext"), "companyRegistrationNumber" -> Seq(""))
        AgentCompanyDetails.formReads.validate(formInput) must be(Invalid(Seq((
          Path \ "companyRegistrationNumber",
          Seq(ValidationError("error.required.bm.registration.number"))
        ))))
      }

      "given a value with length greater than 8" in {
        val data = Map("agentCompanyName" -> Seq("sometext"), "companyRegistrationNumber" -> Seq("1" * 9))
        val result = AgentCompanyDetails.formReads.validate(data)
        result mustBe Invalid(Seq((Path \ "companyRegistrationNumber") -> Seq(ValidationError("error.invalid.bm.registration.number"))))
      }

      "given a value with length less than 8" in {
        val data = Map("agentCompanyName" -> Seq("sometext"), "companyRegistrationNumber" -> Seq("1" * 7))
        val result = AgentCompanyDetails.formReads.validate(data)
        result mustBe Invalid(Seq((Path \ "companyRegistrationNumber") -> Seq(ValidationError("error.invalid.bm.registration.number"))))
      }

      "given a value containing non-alphanumeric characters" in {
        val data = Map("agentCompanyName" -> Seq("sometext"), "companyRegistrationNumber" -> Seq("1234567!"))
        val result = AgentCompanyDetails.formReads.validate(data)
        result mustBe Invalid(Seq((Path \ "companyRegistrationNumber") -> Seq(ValidationError("error.invalid.bm.registration.number"))))
      }

      "both fields missing" in {
        val formInput = Map("agentCompanyName" -> Seq(""))
        AgentCompanyDetails.formReads.validate(formInput) must be(Invalid(Seq((
          Path \ "agentCompanyName",
          Seq(ValidationError("error.required.tp.agent.registered.company.name"))
        ), (
          Path \ "companyRegistrationNumber",
          Seq(ValidationError("error.required"))
        )
        )))
      }

      "input exceeds max length" in {
        val formInput = Map("agentCompanyName" -> Seq("sometesttexttest" * 11), "companyRegistrationNumber" -> Seq("12345678"))
        AgentCompanyDetails.formReads.validate(formInput) must be(Invalid(Seq((
          Path \ "agentCompanyName") -> Seq(ValidationError("error.invalid.tp.agent.registered.company.name")
        ))))
      }

    }

    "validate form write" in {
      AgentCompanyDetails.formWrites.writes(AgentCompanyDetails("sometext", Some("12345678"))) must be(
        Map("agentCompanyName" -> Seq("sometext"), "companyRegistrationNumber" -> Seq("12345678")))
    }


  }

  "Json Validation" must {
    "Successfully read/write Json data" in {
      Json.fromJson[AgentCompanyDetails](Json.toJson[AgentCompanyDetails](
        AgentCompanyDetails("test", "12345678"))) must be(JsSuccess(AgentCompanyDetails("test", Some("12345678"))))
    }
  }
}
