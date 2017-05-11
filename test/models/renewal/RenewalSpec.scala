package models.renewal

import models.Country
import play.api.libs.json.{JsSuccess, Json}
import utils.GenericTestHelper

class RenewalSpec extends GenericTestHelper {

  "The Renewal model" must {

    "serialize to and from JSON" in {

      val model = Renewal()

      Json.fromJson[Renewal](Json.toJson(model)) mustBe JsSuccess(model)

    }

  }
}