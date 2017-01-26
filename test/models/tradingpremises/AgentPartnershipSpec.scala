package models.tradingpremises

import org.scalatestplus.play.PlaySpec
import jto.validation.{Invalid, Path, Valid}
import jto.validation.ValidationError
import play.api.libs.json.{JsPath, JsSuccess}

class AgentPartnershipSpec extends PlaySpec {

  "AgentPartnership" must {

    "validate form Read" in {
      val formInput = Map("agentPartnership" -> Seq("sometext"))
      AgentPartnership.formReads.validate(formInput) must be(Valid(AgentPartnership("sometext")))
    }

    "throw error when required field is missing" in {
      val formInput = Map("agentPartnership" -> Seq(""))
      AgentPartnership.formReads.validate(formInput) must be(Invalid(Seq((Path \ "agentPartnership", Seq(ValidationError("error.required.tp.agent.partnership"))))))
    }

    "throw error when input exceeds max length" in {
      val formInput = Map("agentPartnership" -> Seq("sometesttexttest"*11))
      AgentPartnership.formReads.validate(formInput) must be(Invalid(Seq((Path \ "agentPartnership") -> Seq(ValidationError("error.invalid.tp.agent.partnership")))))
    }

    "validate form write" in {
      AgentPartnership.formWrites.writes(AgentPartnership("sometext")) must be(Map("agentPartnership" -> Seq("sometext")))
    }

  }
  "Json Validation" must {
    "Successfully read/write Json data" in {
      AgentPartnership.format.reads(AgentPartnership.format.writes(
        AgentPartnership("test"))) must be(JsSuccess(AgentPartnership("test"), JsPath \ "agentPartnership"))
    }
  }
}
