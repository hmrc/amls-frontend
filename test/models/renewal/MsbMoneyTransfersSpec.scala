package models.renewal

import utils.GenericTestHelper
import jto.validation.{Invalid, Path, Valid, ValidationError}

class MsbMoneyTransfersSpec extends GenericTestHelper {

  "The form serialiser returns the correct model" when {
    "given valid form data" in {
      val form = Map(
        "transfers" -> Seq("2000")
      )

      MsbMoneyTransfers.formReader.validate(form) mustBe Valid(MsbMoneyTransfers(2000))
    }

    "given missing form data" in {
      val form = Map.empty[String, Seq[String]]

      MsbMoneyTransfers.formReader.validate(form) mustBe Invalid(Seq(Path \ "transfers" -> Seq(ValidationError("renewal.msb.transfers.value.required"))))
    }

    "given something that's not a number" in {
      val form = Map("transfers" -> Seq("not a number"))

      MsbMoneyTransfers.formReader.validate(form) mustBe Invalid(Seq(Path \ "transfers" -> Seq(ValidationError("renewal.msb.transfers.value.required"))))
    }
  }

}
