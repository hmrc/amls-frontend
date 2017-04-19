package models.businessactivities

import org.scalatestplus.play.PlaySpec
import jto.validation.{Invalid, Path, Valid}
import jto.validation.ValidationError

class TaxMattersSpec extends PlaySpec {

  "Form Validation" must {
    "pass" when {
      "yes option is picked" in {
        TaxMatters.formRule.validate(Map("manageYourTaxAffairs" -> Seq("true"))) must be(Valid(TaxMatters(true)))
      }
      "no option is picked" in {
        TaxMatters.formRule.validate(Map("manageYourTaxAffairs" -> Seq("false"))) must be(Valid(TaxMatters(false)))
      }
    }
    "fail" when {
      "neither option is picked, represented by an empty map" in {
        TaxMatters.formRule.validate(Map.empty) must be(Invalid(Seq(
          (Path \ "manageYourTaxAffairs") -> Seq(ValidationError("error.required.ba.tax.matters")))))
      }
      "neither option is picked, represented by an empty string" in {
        TaxMatters.formRule.validate(Map("manageYourTaxAffairs" -> Seq(""))) must be(Invalid(Seq(
          (Path \ "manageYourTaxAffairs") -> Seq(ValidationError("error.required.ba.tax.matters")))))
      }
      "an invalid value is passed" in {
        TaxMatters.formRule.validate(Map("manageYourTaxAffairs" -> Seq("random"))) must be(Invalid(Seq(
          (Path \ "manageYourTaxAffairs") -> Seq(ValidationError("error.required.ba.tax.matters")))))
      }
    }
  }

  "Form Writes" must {
    "Write true into form" in {
      TaxMatters.formWrites.writes(TaxMatters(true)) must be(Map("manageYourTaxAffairs" -> Seq("true")))
    }
    "Write false into form" in {
      TaxMatters.formWrites.writes(TaxMatters(false)) must be(Map("manageYourTaxAffairs" -> Seq("false")))
    }
  }

}
