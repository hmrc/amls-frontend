package controllers.aboutyou

import connectors.DataCacheConnector
import models.aboutyou.{AboutYou, YourDetails}
import org.jsoup.Jsoup
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.i18n.Messages
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.AuthorisedFixture

import scala.concurrent.Future

class YourDetailsControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar {

  trait Fixture extends AuthorisedFixture {
    self =>

    val controller = new YourDetailsController {
      override val dataCacheConnector = mock[DataCacheConnector]
      override val authConnector = self.authConnector
    }
  }

  val emptyCache = CacheMap("", Map.empty)

  "AboutYouController" must {

    "Get" must {

      "load Your Name page" in new Fixture {

        when(controller.dataCacheConnector.fetch[YourDetails](any())
          (any(), any(), any())).thenReturn(Future.successful(None))

        val result = controller.get()(request)
        status(result) must be(OK)
        contentAsString(result) must include(Messages("lbl.first_name"))
      }

      "load Your Name page with pre populated data" in new Fixture {

        val yourDetails = YourDetails("foo", None, "bar")

        when(controller.dataCacheConnector.fetch[AboutYou](any())
          (any(), any(), any())).thenReturn(Future.successful(Some(AboutYou(Some(yourDetails), None))))

        val result = controller.get()(request)
        status(result) must be(OK)

        val document = Jsoup.parse(contentAsString(result))
        document.select("input[name=firstname]").`val` must be("foo")
        document.select("input[name=lastname]").`val` must be("bar")
      }
    }

    "Post" must {

      "on post valid data" in new Fixture {

        val newRequest = request.withFormUrlEncodedBody(
          "firstName" -> "foo",
          "middleName" -> "asdf",
          "lastName" -> "bar"
        )

        when(controller.dataCacheConnector.fetch[AboutYou](any())
          (any(), any(), any())).thenReturn(Future.successful(None))

        when(controller.dataCacheConnector.save[AboutYou](any(), any())
          (any(), any(), any())).thenReturn(Future.successful(emptyCache))

        val result = controller.post()(newRequest)
        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some(controllers.aboutyou.routes.SummaryController.get().url))
      }

      "on post invalid data" in new Fixture {

        val newRequest = request.withFormUrlEncodedBody(
          "firstName" -> "foo"
        )

        val result = controller.post()(newRequest)
        status(result) must be(BAD_REQUEST)

        val document = Jsoup.parse(contentAsString(result))
        document.select("input[name=firstname]").`val` must be("foo")
      }
    }
  }
}
