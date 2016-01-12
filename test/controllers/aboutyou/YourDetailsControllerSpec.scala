package controllers.aboutyou

import connectors.DataCacheConnector
import models.aboutyou.{AboutYou, YourDetails}
import org.jsoup.Jsoup
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.i18n.Messages
import play.api.libs.json.Json
import play.api.test.Helpers._
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

  "AboutYouController" must {

    "Get" must {

      "load Your Name page" in new Fixture {

        when(controller.dataCacheConnector.fetchDataShortLivedCache[YourDetails](any())
          (any(), any(), any())).thenReturn(Future.successful(None))

        val result = controller.get()(request)
        status(result) must be(OK)
        contentAsString(result) must include(Messages("lbl.first_name"))
      }

      "load Your Name page with pre populated data" in new Fixture {

        val yourDetails = YourDetails("foo", None, "bar")

        when(controller.dataCacheConnector.fetchDataShortLivedCache[YourDetails](any())
          (any(), any(), any())).thenReturn(Future.successful(Some(yourDetails)))

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
          "firstname" -> "foo",
          "middlename" -> "asdf",
          "lastname" -> "bar"
        )

        when(controller.dataCacheConnector.fetchDataShortLivedCache[AboutYou](any())
          (any(), any(), any())).thenReturn(Future.successful(None))

        when(controller.dataCacheConnector.saveDataShortLivedCache[AboutYou](any(), any())
          (any(), any(), any())).thenReturn(Future.successful(None))

        val result = controller.post()(newRequest)
        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some(controllers.aboutyou.routes.SummaryController.get().url))
      }

      "on post invalid data" in new Fixture {

        val newRequest = request.withFormUrlEncodedBody(
          "firstname" -> "foo"
        )

        val result = controller.post()(newRequest)
        status(result) must be(BAD_REQUEST)

        val document = Jsoup.parse(contentAsString(result))
        document.select("input[name=firstname]").`val` must be("foo")
      }
    }
  }
}
