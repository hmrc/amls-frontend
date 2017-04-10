package models.renewal

import cats.implicits._
import jto.validation.{Invalid, Path, Valid, ValidationError}
import play.api.libs.json.Json
import utils.GenericTestHelper

class MsbMoneyTransfersSpec extends GenericTestHelper {

  "The form serialiser returns the correct model" when {

    "given valid form data" in {
      val form = Map(
        "transfers" -> Seq("2000")
      )

      MsbMoneyTransfers.formReader.validate(form) mustBe Valid(MsbMoneyTransfers("2000"))
    }

    "given missing form data" in {
      val form = Map("transfers" -> Seq(" "))
      MsbMoneyTransfers.formReader.validate(form) mustBe Invalid(Seq(Path \ "transfers" -> Seq(ValidationError("renewal.msb.transfers.value.invalid"))))
    }

    "given something that's not a number" in {
      val form = Map("transfers" -> Seq("not a number"))
      MsbMoneyTransfers.formReader.validate(form) mustBe Invalid(Seq(Path \ "transfers" -> Seq(ValidationError("error.invalid.msb.transactions.in.12months"))))
    }

    "given a number over 11 in length" in {
      val form = Map("transfers" -> Seq("12345678900987654321"))
      MsbMoneyTransfers.formReader.validate(form) mustBe Invalid(Seq(Path \ "transfers" -> Seq(ValidationError("error.invalid.msb.transactions.in.12months"))))
    }

  }

  "The model serialiser returns the correct form" when {
    "given a valid model" in {
      val model = MsbMoneyTransfers("1575")
      MsbMoneyTransfers.formWriter.writes(model) mustBe Map("transfers" -> Seq("1575"))
    }
  }

  "The json serialiser" must {
    "round-trip through json serialisation" in {
      val model = MsbMoneyTransfers("1200")
      Json.fromJson[MsbMoneyTransfers](Json.toJson(model)).asOpt mustBe model.some
    }
  }

}
