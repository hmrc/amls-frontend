package models.renewal

import models.Country
import models.moneyservicebusiness.ExpectedThroughput
import play.api.libs.json.{JsSuccess, Json}
import utils.GenericTestHelper

class RenewalSpec extends GenericTestHelper {

  "The Renewal model" must {

    "serialize to and from JSON" in {

      val model = Renewal()

      Json.fromJson[Renewal](Json.toJson(model)) mustBe JsSuccess(model)

    }

    "be complete" when {

      "involvedInOther is yes" in {

        val model = Renewal(
          Some(InvolvedInOtherYes("test")),
          Some(BusinessTurnover.First),
          Some(AMLSTurnover.First),
          Some(CustomersOutsideUK(Some(Seq(Country("United Kingdom", "GB"))))),
          Some(PercentageOfCashPaymentOver15000.First),
          Some(MsbThroughput("01")),
          Some(CETransactions("123")),
          hasChanged = true
        )

        model.isComplete mustBe true

      }

      "involvedInOther is no" in {

        val model = Renewal(
          Some(InvolvedInOtherNo),
          None,
          Some(AMLSTurnover.First),
          Some(CustomersOutsideUK(Some(Seq(Country("United Kingdom", "GB"))))),
          Some(PercentageOfCashPaymentOver15000.First),
          Some(MsbThroughput("01")),
          Some(CETransactions("123")),
          hasChanged = true
        )

        model.isComplete mustBe true

      }

    }

    "be incomplete" when {

      "any of the sub models are not specified" in {

        val model = Renewal(None, hasChanged = true)

        model.isComplete mustBe false

      }

      "involvedinOther is yes, but there is nothing in businessTurnover" in {

        val model = Renewal(
          Some(InvolvedInOtherYes("test")),
          None,
          Some(AMLSTurnover.First),
          Some(CustomersOutsideUK(Some(Seq(Country("United Kingdom", "GB"))))),
          hasChanged = true)

        model.isComplete mustBe false

      }
    }
  }
}
