package models

import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json

class AboutYouSpec extends PlaySpec with MockitoSugar {

  val yourDetails = YourDetails(
    "firstname", None, "lastname"
  )

  val role = RoleWithinBusiness(
    "01",
    ""
  )

  "AboutYou" must {

    val completeJson = Json.obj(
      "firstName" -> "firstname",
      "lastName" -> "lastname",
      "roleWithinBusiness" -> "01",
      "other" -> ""
    )

    val completeModel = AboutYou(Some(yourDetails), Some(role))

    "Serialise as expected" in {

      Json.toJson(completeModel) must
        be(completeJson)
    }

    "Deserialise as expected" in {

      completeJson.as[AboutYou] must
        be(completeModel)
    }
  }

  "Partially complete AboutYou" must {

    val partialJson = Json.obj(
      "firstName" -> "firstname",
      "lastName" -> "lastname"
    )

    val partialModel = AboutYou(Some(yourDetails), None)

    "Serialise as expected" in {

      Json.toJson(partialModel) must
        be(partialJson)
    }

    "Deserialise as expected" in {

      partialJson.as[AboutYou] must
        be(partialModel)
    }
  }
}
