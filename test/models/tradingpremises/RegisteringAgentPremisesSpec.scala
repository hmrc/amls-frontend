package models.tradingpremises

import org.scalatestplus.play.PlaySpec
import jto.validation.{Invalid, Path, Valid}
import jto.validation.ValidationError
import play.api.libs.json.{JsPath, JsSuccess}

class RegisteringAgentPremisesSpec extends PlaySpec {

  "Form Validation" must {
    "Fail if neither option is picked" in {
      RegisteringAgentPremises.formRule.validate(Map()) must be(Invalid(Seq(
        (Path \ "agentPremises") -> Seq(ValidationError("error.required.tp.agent.premises")))))
    }

    "Succeed if yes option is picked" in {
      RegisteringAgentPremises.formRule.validate(Map("agentPremises" -> Seq("true"))) must be(Valid(RegisteringAgentPremises(true)))
    }

    "Succeed if no option is picked" in {
      RegisteringAgentPremises.formRule.validate(Map("agentPremises" -> Seq("false"))) must be(Valid(RegisteringAgentPremises(false)))
    }

    "Fail if an invalid value is passed" in {
      RegisteringAgentPremises.formRule.validate(Map("agentPremises" -> Seq("random"))) must be(Invalid(Seq(
        (Path \ "agentPremises") -> Seq(ValidationError("error.required.tp.agent.premises")))))
    }
  }
  "Form Writes" must {
    "Write true into form" in {
      RegisteringAgentPremises.formWrites.writes(RegisteringAgentPremises(true)) must be(Map("agentPremises" -> Seq("true")))
    }

    "Write false into form" in {
      RegisteringAgentPremises.formWrites.writes(RegisteringAgentPremises(false)) must be(Map("agentPremises" -> Seq("false")))
    }
  }

  "JSON" should {
    "Read and write successfully" in {
      RegisteringAgentPremises.formats.reads(RegisteringAgentPremises.formats.writes(RegisteringAgentPremises(true))) must be (
        JsSuccess(RegisteringAgentPremises(true), JsPath \ "agentPremises"))
    }
  }
}
