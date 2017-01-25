package models.businessactivities

import org.scalatestplus.play.PlaySpec
import jto.validation.{Failure, Path, Success}
import jto.validation.ValidationError

class TaxMattersSpec extends PlaySpec {

  "Form Validation" must {
    "Fail if neither option is picked" in {
      TaxMatters.formRule.validate(Map()) must be(Failure(Seq(
        (Path \ "manageYourTaxAffairs") -> Seq(ValidationError("error.required.ba.tax.matters")))))
    }
    "Succeed if yes option is picked" in {
      TaxMatters.formRule.validate(Map("manageYourTaxAffairs" -> Seq("true"))) must be(Success(TaxMatters(true)))
    }
    "Succeed if no option is picked" in {
      TaxMatters.formRule.validate(Map("manageYourTaxAffairs" -> Seq("false"))) must be(Success(TaxMatters(false)))
    }
    "Fail if an invalid value is passed" in {
      TaxMatters.formRule.validate(Map("manageYourTaxAffairs" -> Seq("random"))) must be(Failure(Seq(
        (Path \ "manageYourTaxAffairs") -> Seq(ValidationError("error.required.ba.tax.matters")))))
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
