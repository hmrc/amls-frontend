package models.tcsp

import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json


class TcspSpec extends PlaySpec with MockitoSugar {

  "Tcsp" must {

    "correctly convert between json formats" must {

      val completeJson = Json.obj()
      val completeModel = Tcsp()

//      "Serialise as expected" ignore {
//        Json.toJson(completeModel) must be(completeJson)
//      }
//
//      "Deserialise as expected" ignore {
//        completeJson.as[Tcsp] must be(completeModel)
//      }

    }

  }

}
