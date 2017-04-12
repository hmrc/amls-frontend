package models.renewal

import jto.validation.{Invalid, Path, Valid, ValidationError}
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsPath, JsSuccess}

class CETransactionsInLast12MonthsSpec extends PlaySpec {

  "CETransactionIn" should {

    "Form Validation" must {

      "Successfully read form data for option yes" in {

        val map = Map("ceTransaction" -> Seq("12345678963"))

        CETransactionsInLast12Months.formRule.validate(map) must be(Valid(CETransactionsInLast12Months("12345678963")))
      }

      "fail validation on missing field" in {

        val map = Map("ceTransaction" -> Seq(""))

        CETransactionsInLast12Months.formRule.validate(map) must be(Invalid(
          Seq( Path \ "ceTransaction" -> Seq(ValidationError("error.required.renewal.ce.transactions.in.12months")))))
      }

      "fail validation on invalid field" in {

        val map = Map("ceTransaction" -> Seq("asas"))
        CETransactionsInLast12Months.formRule.validate(map) must be(Invalid(
          Seq( Path \ "ceTransaction" -> Seq(ValidationError("error.invalid.msb.transactions.in.12months")))))
      }

      "fail validation on invalid field when it exceeds the max length" in {

        val map = Map("ceTransaction" -> Seq("123"*10))
        CETransactionsInLast12Months.formRule.validate(map) must be(Invalid(
          Seq( Path \ "ceTransaction" -> Seq(ValidationError("error.invalid.msb.transactions.in.12months")))))
      }

      "fail validation on invalid field1" in {

        val map = Map("ceTransaction" -> Seq("123456"))
        CETransactionsInLast12Months.formRule.validate(map) must be(Valid(CETransactionsInLast12Months("123456")))
      }


      "successfully write form data" in {

        CETransactionsInLast12Months.formWrites.writes(CETransactionsInLast12Months("12345678963")) must be(Map("ceTransaction" -> Seq("12345678963")))
      }
    }

    "Json Validation" must {

      "Successfully read/write Json data" in {

        CETransactionsInLast12Months.format.reads(CETransactionsInLast12Months.format.writes(
          CETransactionsInLast12Months("12345678963"))) must be(JsSuccess(CETransactionsInLast12Months("12345678963"), JsPath \ "ceTransaction"))

      }
    }
  }
}
