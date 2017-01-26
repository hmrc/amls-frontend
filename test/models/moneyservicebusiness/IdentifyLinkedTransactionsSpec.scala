package models.moneyservicebusiness

import org.scalatestplus.play.PlaySpec
import jto.validation.{Invalid, Path, Valid}
import jto.validation.ValidationError
import play.api.libs.json.{JsPath, JsSuccess}

class IdentifyLinkedTransactionsSpec extends PlaySpec {

  "IdentifyLinkedTransactions" should {

    "Form Validation" must {

      "Successfully read form data for option yes" in {

        val map = Map("linkedTxn" -> Seq("true"))

        IdentifyLinkedTransactions.formRule.validate(map) must be(Valid(IdentifyLinkedTransactions(true)))
      }

      "Successfully read form data for option no" in {

        val map = Map("linkedTxn" -> Seq("false"))

        IdentifyLinkedTransactions.formRule.validate(map) must be(Valid(IdentifyLinkedTransactions(false)))
      }

      "fail validation on missing field" in {

        IdentifyLinkedTransactions.formRule.validate(Map.empty) must be(Invalid(
          Seq( Path \ "linkedTxn" -> Seq(ValidationError("error.required.msb.linked.txn")))))
      }

      "successfully write form data" in {

        IdentifyLinkedTransactions.formWrites.writes(IdentifyLinkedTransactions(false)) must be(Map("linkedTxn" -> Seq("false")))
      }
    }

    "Json Validation" must {

      "Successfully read/write Json data" in {

        IdentifyLinkedTransactions.format.reads(IdentifyLinkedTransactions.format.writes(
          IdentifyLinkedTransactions(false))) must be(JsSuccess(IdentifyLinkedTransactions(false), JsPath \ "linkedTxn"))

      }
    }
  }
}
