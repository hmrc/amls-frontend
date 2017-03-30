package models.renewal

import org.scalatest.MustMatchers
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsSuccess, Json}

class MsbThroughputSpec extends PlaySpec with MustMatchers {

  "The MsbThroughput model" must {

    "be able to be serialized and deserialized" in {
      val model = MsbThroughput("01")

      Json.fromJson[MsbThroughput](Json.toJson(model)).asOpt mustBe Some(model)
    }

  }
}
