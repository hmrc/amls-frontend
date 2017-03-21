package models.renewal

import models.renewal.AMLSTurnover.First
import play.api.libs.json.{JsSuccess, Json}
import utils.GenericTestHelper

class RenewalSpec extends GenericTestHelper {

  "The Renewal model" must {

    "serialize to and from JSON" in {

      val model = Renewal()

      Json.fromJson[Renewal](Json.toJson(model)) mustBe JsSuccess(model)

    }

    "be complete" when {

      "involved in other activities was specified" in {

        val model = Renewal(Some(InvolvedInOtherNo), Some(First), hasChanged = true)

        model.isComplete mustBe true

      }

    }

    "be incomplete" when {

      "any of the sub models are not specified" in {

        val model = Renewal(None, hasChanged = true)

        model.isComplete mustBe false

      }

    }

  }

}
