package controllers.responsiblepeople

import connectors.DataCacheConnector
import controllers.responsiblepeople.RemoveResponsiblePersonController
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.scalatest.prop.PropertyChecks
import org.scalatest.{MustMatchers, WordSpecLike}
import org.scalatestplus.play.OneAppPerSuite
import utils.AuthorisedFixture
import play.api.test.Helpers._

class RemoveResponsiblePersonControllerSpec extends WordSpecLike
  with MustMatchers with MockitoSugar with ScalaFutures with OneAppPerSuite with PropertyChecks {

  trait Fixture extends AuthorisedFixture {
    self =>

    val controller = new RemoveResponsiblePersonController {
      override val dataCacheConnector = mock[DataCacheConnector]
      override val authConnector = self.authConnector
    }
  }

  "RemoveResponsiblePersonController" when {

    "get is called" must {

      "respond with OK" in new Fixture {

        val result = controller.get(1, false)(request)

        status(result) must be(OK)

      }
    }

//    "remove is called" must {
//      "respond with ???" in new Fixture {
//
//        val result = controller.remove()(request)
//      }
//    }
  }

}
