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

      TransactionsInLast12Months.formReader.validate(form) mustBe Valid(TransactionsInLast12Months("2000"))
    }

    "given an empty string" in {
      val form = Map("transfers" -> Seq(""))
      TransactionsInLast12Months.formReader.validate(form) mustBe Invalid(Seq(Path \ "transfers" -> Seq(ValidationError("renewal.msb.transfers.value.invalid"))))
    }

    "given a string with no value" in {
      val form = Map("transfers" -> Seq(""))
      TransactionsInLast12Months.formReader.validate(form) mustBe Invalid(Seq(Path \ "transfers" -> Seq(ValidationError("renewal.msb.transfers.value.invalid"))))
    }

    "given an empty map" in {
      val form = Map.empty[String, Seq[String]]
      TransactionsInLast12Months.formReader.validate(form) mustBe Invalid(Seq(Path \ "transfers" -> Seq(ValidationError("error.required"))))
    }

    "given something that's not a number" in {
      val form = Map("transfers" -> Seq("not a number"))
      TransactionsInLast12Months.formReader.validate(form) mustBe Invalid(Seq(Path \ "transfers" -> Seq(ValidationError("error.invalid.msb.transactions.in.12months"))))
    }

    "given a number over 11 in length" in {
      val form = Map("transfers" -> Seq("12345678900987654321"))
      TransactionsInLast12Months.formReader.validate(form) mustBe Invalid(Seq(Path \ "transfers" -> Seq(ValidationError("error.invalid.msb.transactions.in.12months"))))
    }

  }

  "The model serialiser returns the correct form" when {
    "given a valid model" in {
      val model = TransactionsInLast12Months("1575")
      TransactionsInLast12Months.formWriter.writes(model) mustBe Map("transfers" -> Seq("1575"))
    }
  }

  "The json serialiser" must {
    "round-trip through json serialisation" in {
      val model = TransactionsInLast12Months("1200")
      Json.fromJson[TransactionsInLast12Months](Json.toJson(model)).asOpt mustBe model.some
    }
  }

}
