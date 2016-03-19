package models.governmentgateway

import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json

class EnrolmentRequestSpec extends PlaySpec {

  "EnrolmentRequest" must {

    "serialise correctly" in {

      val model = EnrolmentRequest("foo", "bar")
      val json = Json.obj(
          "portalIdentifier" -> "Default",
          "serviceName" -> "HMRC-MLR-ORG",
          "friendlyName" -> "AMLS Enrolment",
          "knownFact" -> Seq(
            "foo",
            "",
            "",
            "bar"
          )
        )

      Json.toJson(model) must
        equal (json)
    }
  }
}
