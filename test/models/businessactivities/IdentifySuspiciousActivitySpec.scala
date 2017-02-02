package models.businessactivities

import org.scalatestplus.play.PlaySpec
import jto.validation.{Invalid, Path, Valid}
import jto.validation.ValidationError

class IdentifySuspiciousActivitySpec extends PlaySpec {

  "Form Validation" must {
    "Fail if neither option is picked" in {
      IdentifySuspiciousActivity.formRule.validate(Map()) must be(Invalid(Seq(
        (Path \ "hasWrittenGuidance") -> Seq(ValidationError("error.required.ba.suspicious.activity")))))
    }

    "Succeed if yes option is picked" in {
      IdentifySuspiciousActivity.formRule.validate(Map("hasWrittenGuidance" -> Seq("true"))) must be(Valid(IdentifySuspiciousActivity(true)))
    }

    "Succeed if no option is picked" in {
      IdentifySuspiciousActivity.formRule.validate(Map("hasWrittenGuidance" -> Seq("false"))) must be(Valid(IdentifySuspiciousActivity(false)))
    }

    "Fail if an invalid value is passed" in {
      IdentifySuspiciousActivity.formRule.validate(Map("hasWrittenGuidance" -> Seq("random"))) must be(Invalid(Seq(
        (Path \ "hasWrittenGuidance") -> Seq(ValidationError("error.required.ba.suspicious.activity")))))
    }
  }
  "Form Writes" must {
    "Write true into form" in {
      IdentifySuspiciousActivity.formWrites.writes(IdentifySuspiciousActivity(true)) must be(Map("hasWrittenGuidance" -> Seq("true")))
    }

    "Write false into form" in {
      IdentifySuspiciousActivity.formWrites.writes(IdentifySuspiciousActivity(false)) must be(Map("hasWrittenGuidance" -> Seq("false")))
    }

  }
}
