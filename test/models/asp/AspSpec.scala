package models.asp

import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json

class AspSpec extends PlaySpec with MockitoSugar {

  "Asp" must {

    "correctly convert between json formats" must {

        val completeJson = Json.obj()
        val completeModel = Asp()

        "Serialise as expected" in {
          Json.toJson(completeModel) must be(completeJson)
        }

        "Deserialise as expected" in {
          completeJson.as[Asp] must be(completeModel)
        }

    }

  }

}
