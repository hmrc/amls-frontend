package utils

import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.ws.WSResponse
import play.api.test.Helpers._
import utils.HttpUtils._

class HttpUtilsSpec extends PlaySpec with MockitoSugar {

  def createResponse(status: Int, headers: Map[String, Seq[String]] = Map.empty[String, Seq[String]]) = {
    val m = mock[WSResponse]

    when(m.status) thenReturn status
    when(m.allHeaders) thenReturn headers
    when(m.header("Location")) thenReturn headers.get("Location").fold[Option[String]](None) {
      case location :: _ => Some(location)
    }

    m
  }

  "The Http Response utilities" must {

    "return the redirect url" when {
      "given a redirect response" in {

        val response = createResponse(SEE_OTHER, Map("Location" -> Seq("http://google.co.uk")))

        response.redirectLocation mustBe Some("http://google.co.uk")

      }
    }

    "return None" when {
      "some other response is returned" in {
        val response = createResponse(OK)

        response.redirectLocation mustBe None
      }

      "a redirect response is returned without a Location header" in {
        val response = createResponse(SEE_OTHER)

        response.redirectLocation mustBe None
      }
    }

  }

}
