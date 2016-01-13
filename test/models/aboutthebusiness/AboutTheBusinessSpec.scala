package models.aboutthebusiness

import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json

class AboutTheBusinessSpec extends PlaySpec with MockitoSugar {


  "AboutTheBusiness" must {
    val completeJson = Json.obj(
      "previouslyRegistered" -> true,
      "previouslyRegisteredYes" -> "12345678"
    )

    val completeModel = AboutTheBusiness(Some(PreviouslyRegisteredYes("12345678")))

    "Serialise as expected" in {

      Json.toJson(completeModel) must
        be(completeJson)
    }

    "Deserialise as expected" in {

      completeJson.as[AboutTheBusiness] must
        be(completeModel)
    }
  }

}