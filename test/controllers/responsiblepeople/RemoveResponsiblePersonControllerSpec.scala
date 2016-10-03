package controllers.responsiblepeople

import connectors.DataCacheConnector
import controllers.responsiblepeople.RemoveResponsiblePersonController
import models.responsiblepeople.{PersonName, ResponsiblePeople}
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.scalatest.prop.PropertyChecks
import org.scalatest.{MustMatchers, WordSpecLike}
import org.scalatestplus.play.OneAppPerSuite
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.AuthorisedFixture
import play.api.test.Helpers._

import scala.concurrent.Future

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
      "respond with OK when the index is valid" in new Fixture {

        when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
          .thenReturn(Future.successful(Some(Seq(ResponsiblePeople(Some(PersonName("firstName", None, "lastName", None, None)))))))

        val result = controller.get(1, false)(request)

        status(result) must be(OK)

      }
      "respond with NOT_FOUND when the index is out of bounds" in new Fixture {

        when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
          .thenReturn(Future.successful(Some(Seq(ResponsiblePeople()))))

        val result = controller.get(100, false)(request)

        status(result) must be(NOT_FOUND)

      }
    }

    "remove is called" must {
      "respond with SEE_OTHER when the index is valid" in new Fixture {

        when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
          .thenReturn(Future.successful(Some(Seq(ResponsiblePeople()))))

        val result = controller.remove(1, false)(request)

        status(result) must be(SEE_OTHER)

      }
    }
  }
}
