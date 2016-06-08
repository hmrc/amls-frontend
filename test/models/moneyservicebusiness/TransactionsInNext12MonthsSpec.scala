package models.moneyservicebusiness

import org.scalatestplus.play.PlaySpec
import play.api.data.mapping.{Failure, Path, Success}
import play.api.data.validation.ValidationError
import play.api.libs.json.{JsPath, JsSuccess}

class TransactionsInNext12MonthsSpec extends PlaySpec {

  "TransactionsInNext12Months" should {

    "Form Validation" must {

      "Successfully read form data for option yes" in {

        val map = Map("txnAmount" -> Seq("12345678963"))

        TransactionsInNext12Months.formRule.validate(map) must be(Success(TransactionsInNext12Months("12345678963")))
      }

      "fail validation on missing field" in {

        val map = Map("txnAmount" -> Seq(""))

        TransactionsInNext12Months.formRule.validate(map) must be(Failure(
          Seq( Path \ "txnAmount" -> Seq(ValidationError("error.required.msb.transactions.in.12months")))))
      }

      "fail validation on invalid field" in {

        val map = Map("txnAmount" -> Seq("asas"))
        TransactionsInNext12Months.formRule.validate(map) must be(Failure(
          Seq( Path \ "txnAmount" -> Seq(ValidationError("error.invalid.msb.transactions.in.12months")))))
      }

      "fail validation on invalid field when it exceeds the max length" in {

        val map = Map("txnAmount" -> Seq("123"*10))
        TransactionsInNext12Months.formRule.validate(map) must be(Failure(
          Seq( Path \ "txnAmount" -> Seq(ValidationError("error.invalid.msb.transactions.in.12months")))))
      }

      "fail validation on invalid field1" in {

        val map = Map("txnAmount" -> Seq("123456"))
        TransactionsInNext12Months.formRule.validate(map) must be(Success(TransactionsInNext12Months("123456")))
      }


      "successfully write form data" in {

        TransactionsInNext12Months.formWrites.writes(TransactionsInNext12Months("12345678963")) must be(Map("txnAmount" -> Seq("12345678963")))
      }
    }

    "Json Validation" must {

      "Successfully read/write Json data" in {

        TransactionsInNext12Months.format.reads(TransactionsInNext12Months.format.writes(
          TransactionsInNext12Months("12345678963"))) must be(JsSuccess(TransactionsInNext12Months("12345678963"), JsPath \ "txnAmount"))

      }
    }
  }
}
