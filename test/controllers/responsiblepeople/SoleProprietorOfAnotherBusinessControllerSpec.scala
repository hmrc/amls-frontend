import connectors.DataCacheConnector
import controllers.responsiblepeople.{routes, SoleProprietorOfAnotherBusinessController, VATRegisteredController}
import models.responsiblepeople.{ResponsiblePeople, PersonName}
import org.jsoup.Jsoup
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import play.api.i18n.Messages
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.{AuthorisedFixture, GenericTestHelper}

import scala.concurrent.Future

class SoleProprietorOfAnotherBusinessControllerSpec extends GenericTestHelper with MockitoSugar with ScalaFutures {

  trait Fixture extends AuthorisedFixture {
    self =>
    val request = addToken(authRequest)

    lazy val mockDataCacheConnector = mock[DataCacheConnector]

    val controller = new SoleProprietorOfAnotherBusinessController(dataCacheConnector = mockDataCacheConnector, authConnector = self.authConnector) {
    }
  }


  val emptyCache = CacheMap("", Map.empty)

  val pageTitle = Messages("responsiblepeople.registeredforvat.title", "firstname lastname") + " - " +
    Messages("summary.responsiblepeople") + " - " +
    Messages("title.amls") + " - " + Messages("title.gov")
  val personName = Some(PersonName("firstname", None, "lastname", None, None))

  "SoleProprietorOfAnotherBusinessController" when {

    "get is called" must {
      "display page" in new Fixture {

        when(mockDataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
          .thenReturn(Future.successful(Some(Seq(ResponsiblePeople(personName)))))

        val result = controller.get(1)(request)

        status(result) must be(OK)
      }
    }
  }
}