package models.governmentgateway

import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json

class EnrolmentRequestSpec extends PlaySpec {

  "EnrolmentRequest" must {

    "serialise correctly" in {

      val model = EnrolmentRequest("foo", "bar")
      val json = Json.obj(
          "portalId" -> "Default",
          "serviceName" -> "HMRC-MLR-ORG",
          "friendlyName" -> "AMLS Enrolment",
          "knownFacts" -> Seq(
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
