package controllers.aboutyou

import connectors.DataCacheConnector
import models.aboutyou.{SoleProprietor, AboutYou, YourDetails}
import org.jsoup.Jsoup
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.AuthorisedFixture

import scala.concurrent.Future

class SummaryControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar {

  trait Fixture extends AuthorisedFixture {
    self =>

    val controller = new SummaryController {
      override val dataCache = mock[DataCacheConnector]
      override val authConnector = self.authConnector
    }
  }

  val emptyCache = CacheMap("", Map.empty)

  "SummaryController" must {

    "Get" must {

      "redirect to the main summary controller if no data" in new Fixture {

        when(controller.dataCache.fetch[AboutYou](any())
          (any(), any(), any())).thenReturn(Future.successful(None))

        val result = controller.get()(request)
        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be (Some(controllers.routes.RegistrationProgressController.get().url))
      }

      "load summary page with pre populated data" in new Fixture {

        val yourDetails = YourDetails("foo", None, "bar")
        val roleWithinBusiness = SoleProprietor
        val aboutYou = Some(AboutYou(Some(yourDetails), Some(roleWithinBusiness)))

        when(controller.dataCache.fetch[AboutYou](any())
          (any(), any(), any())).thenReturn(Future.successful(aboutYou))

        val result = controller.get()(request)
        status(result) must be(OK)

        val document = Jsoup.parse(contentAsString(result))
        document.body.toString must include ("foo bar")
        document.body.toString must include ("Sole proprietor")

      }
    }

  }
}
