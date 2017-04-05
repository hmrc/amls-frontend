package models.renewal

import jto.validation.{Invalid, Path, Valid, ValidationError}
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsPath, JsSuccess}

class CETransactionsSpec extends PlaySpec {

  "CETransactionIn" should {

    "Form Validation" must {

      "Successfully read form data for option yes" in {

        val map = Map("ceTransaction" -> Seq("12345678963"))

        CETransactions.formRule.validate(map) must be(Valid(CETransactions("12345678963")))
      }

      "fail validation on missing field" in {

        val map = Map("ceTransaction" -> Seq(""))

        CETransactions.formRule.validate(map) must be(Invalid(
          Seq( Path \ "ceTransaction" -> Seq(ValidationError("error.required.renewal.transactions.in.12months")))))
      }

      "fail validation on invalid field" in {

        val map = Map("ceTransaction" -> Seq("asas"))
        CETransactions.formRule.validate(map) must be(Invalid(
          Seq( Path \ "ceTransaction" -> Seq(ValidationError("error.invalid.msb.transactions.in.12months")))))
      }

      "fail validation on invalid field when it exceeds the max length" in {

        val map = Map("ceTransaction" -> Seq("123"*10))
        CETransactions.formRule.validate(map) must be(Invalid(
          Seq( Path \ "ceTransaction" -> Seq(ValidationError("error.invalid.msb.transactions.in.12months")))))
      }

      "fail validation on invalid field1" in {

        val map = Map("ceTransaction" -> Seq("123456"))
        CETransactions.formRule.validate(map) must be(Valid(CETransactions("123456")))
      }


      "successfully write form data" in {

        CETransactions.formWrites.writes(CETransactions("12345678963")) must be(Map("ceTransaction" -> Seq("12345678963")))
      }
    }

    "Json Validation" must {

      "Successfully read/write Json data" in {

        CETransactions.format.reads(CETransactions.format.writes(
          CETransactions("12345678963"))) must be(JsSuccess(CETransactions("12345678963"), JsPath \ "ceTransaction"))

      }
    }
  }
}
