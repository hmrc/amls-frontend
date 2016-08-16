package models.tradingpremises

import org.scalatestplus.play.PlaySpec
import play.api.data.mapping.{Failure, Path, Success}
import play.api.data.validation.ValidationError

class AgentPartnershipSpec extends PlaySpec {

  "AgentPartnership" must {

    "validate form Read" in {
      val formInput = Map("agentPartnership" -> Seq("sometext"))
      AgentPartnership.formReads.validate(formInput) must be(Success(AgentPartnership("sometext")))
    }

    "throw error when required field is missing" in {
      val formInput = Map("agentPartnership" -> Seq(""))
      AgentPartnership.formReads.validate(formInput) must be(Failure(Seq((Path \ "agentPartnership", Seq(ValidationError("error.required.tp.agent.partnership"))))))
    }

    "throw error when input exceeds max length" in {
      val formInput = Map("agentPartnership" -> Seq("sometesttexttest"*11))
      AgentPartnership.formReads.validate(formInput) must be(Failure(Seq((Path \ "agentPartnership") -> Seq(ValidationError("error.invalid.tp.agent.partnership")))))
    }

    "validate form write" in {
      AgentPartnership.formWrites.writes(AgentPartnership("sometext")) must be(Map("agentPartnership" -> Seq("sometext")))
    }


  }
}
