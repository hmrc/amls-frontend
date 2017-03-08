package utils

import org.scalatestplus.play.PlaySpec
import uk.gov.hmrc.play.http.HttpResponse
import play.api.test.Helpers._
import utils.HttpUtils._

class HttpUtilsSpec extends PlaySpec {

  "The Http Response utilities" must {

    "return the redirect url" when {
      "given a redirect response" in {

        val response = HttpResponse(SEE_OTHER, None, Map("Location" -> Seq("http://google.co.uk")))

        response.redirectLocation mustBe Some("http://google.co.uk")

      }
    }

    "return None" when {
      "some other response is returned" in {
        val response = HttpResponse(OK)

        response.redirectLocation mustBe None
      }

      "a redirect response is returned without a Location header" in {
        val response = HttpResponse(SEE_OTHER)

        response.redirectLocation mustBe None
      }
    }

  }

}
