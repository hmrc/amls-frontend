package models.hvd

import org.scalatestplus.play.PlaySpec
import play.api.data.mapping.{Path, Failure, Success}
import play.api.data.validation.ValidationError
import play.api.libs.json.{JsPath, JsSuccess}

class LinkedCashPaymentsSpec extends PlaySpec {

  "LinkedCashPayments" should {

    "Form Validation:" must {

      "successfully validate given an valid 'yes' option" in {
        val map = Map {
          "linkedCashPayments" -> Seq("true")
        }

        LinkedCashPayments.formRule.validate(map) must be(Success(LinkedCashPayments(true)))
      }

      "successfully validate given an valid 'no' option" in {
        val map = Map {
          "linkedCashPayments" -> Seq("false")
        }

        LinkedCashPayments.formRule.validate(map) must be(Success(LinkedCashPayments(false)))
      }

      "throw error message on invalid data" in {

        LinkedCashPayments.formRule.validate(Map.empty) must be(Failure(
          Seq(Path \ "linkedCashPayments" -> Seq(ValidationError("error.required.hvd.linked.cash.payment")))))

      }

      "successfully write form data" in {

        LinkedCashPayments.formWrites.writes(LinkedCashPayments(true)) must be(Map("linkedCashPayments" -> Seq("true")))

      }
    }

    "Json Validation" must {

      "successfully read and write json data" in {

        LinkedCashPayments.format.reads(LinkedCashPayments.format.writes(LinkedCashPayments(true))) must be(JsSuccess(LinkedCashPayments(true),
          JsPath \ "linkedCashPayments"))

      }
    }
  }
}
