package models.tcsp

import models.registrationprogress._
import org.scalatest.mock.MockitoSugar
import org.mockito.Mockito._
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json
import uk.gov.hmrc.http.cache.client.CacheMap


trait TcspValues {

  object DefaultValues {

  }

  object NewValues {

  }

  val completeJson = Json.obj()
  val completeTcsp = Tcsp()

}

class TcspSpec extends PlaySpec with MockitoSugar with TcspValues {

  "Tcsp" must {

    "have a default function that" must {

      "correctly provide a default value when none is provided" in {
        Tcsp.default(None) must be (Tcsp())
      }

      "correctly provide a default value when existing value is provided" in {
        Tcsp.default(Some(completeTcsp)) must be (completeTcsp)
      }

    }

    "have a section function that" must {

      implicit val cache = mock[CacheMap]

      "use the correct section message key" in {
        Tcsp.section.name must be("tcsp")
      }

      //TODO: Change this from ignore once model has a sub-model.
      "have a status of Completed when model is completed" ignore {

//        when {
//          cache.getEntry[Tcsp]("tcsp")
//        } thenReturn Some(completeTcsp)

        Tcsp.section.status must be (Completed)

      }

      //TODO: Change this from ignore once model has a sub-model.
      "have a status of Started when model is incomplete" ignore {

        //TODO: Make the model incomplete once model has two sub-models.
        val incompleteTcsp = completeTcsp

//        when {
//          cache.getEntry[Tcsp]("tcsp")
//        } thenReturn Some(incompleteTcsp)

        Tcsp.section.status must be (Started)

      }

    }

    "have an isComplete function that" must {

      "correctly show if the model is complete" in {
        val complete = Tcsp()
        complete.isComplete must be (true)
      }

      //TODO: Change this from ignore once model has a sub-model.
      "correctly show if the model is not complete" ignore {
        val complete = Tcsp()
        complete.isComplete must be (false)
      }

    }

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
