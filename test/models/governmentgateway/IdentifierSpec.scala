package models.governmentgateway

import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsSuccess, Json}

class IdentifierSpec extends PlaySpec with MockitoSugar {

  val model = Identifier("foo", "bar")

  val json = Json.obj(
    "type" -> "foo",
    "value" -> "bar"
  )

  "Identifier" must {

    "correctly serialise" in {
      Json.toJson(model) must
        equal (json)
    }

    "correctly deserialise" in {
      Json.fromJson[Identifier](json) must
        equal (JsSuccess(model))
    }
  }
}
