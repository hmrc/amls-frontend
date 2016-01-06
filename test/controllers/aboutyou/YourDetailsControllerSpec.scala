package controllers.aboutyou

import connectors.DataCacheConnector
import models.YourDetails
import org.jsoup.Jsoup
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.i18n.Messages
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector

import scala.concurrent.Future

class YourDetailsControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar {

  implicit val request = FakeRequest()

  object Controller extends YourDetailsController {
    override val authConnector = mock[AuthConnector]
    override val dataCacheConnector = mock[DataCacheConnector]
  }

  "AboutYouController" must {

    "Get" must {

      "load Your Name page" in {

        when(Controller.dataCacheConnector.fetchDataShortLivedCache[YourDetails](any())
          (any(), any(), any())).thenReturn(Future.successful(None))

        val result = Controller.get(mock[AuthContext], request)

        status(result) must be(OK)
        contentAsString(result) must include(Messages("lbl.first_name"))
      }

      "load Your Name page with pre populated data" in {

        val yourDetails = YourDetails("foo", None, "bar")

        when(Controller.dataCacheConnector.fetchDataShortLivedCache[YourDetails](any())
          (any(), any(), any())).thenReturn(Future.successful(Some(yourDetails)))

        val result = Controller.get(mock[AuthContext], request)
        status(result) must be(OK)

        val document = Jsoup.parse(contentAsString(result))
        document.select("input[name=firstname]").`val` must be("foo")
        document.select("input[name=lastname]").`val` must be("bar")
      }
    }

    "Post" must {

      "on post valid data" in {

        val request = FakeRequest().withFormUrlEncodedBody(
          "firstname" -> "foo",
          "lastname" -> "bar"
        )

        when(Controller.dataCacheConnector.saveDataShortLivedCache[YourDetails](any(), any())
          (any(), any(), any())).thenReturn(Future.successful(None))

        val result = Controller.post(mock[AuthContext], request)
        status(result) must be(SEE_OTHER)
//        redirectLocation(result) must be(Some(controllers.aboutyou.routes.YourDetailsController.get().url))
      }

      "on post invalid data" in {

        val request = FakeRequest().withFormUrlEncodedBody(
          "firstname" -> "foo"
        )

        val result = Controller.post(mock[AuthContext], request)
        status(result) must be(BAD_REQUEST)

        val document = Jsoup.parse(contentAsString(result))
        document.select("input[name=firstname]").`val`() must be("foo")
      }
    }
  }
}
