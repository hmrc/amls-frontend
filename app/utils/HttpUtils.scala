package utils

import uk.gov.hmrc.play.http.HttpResponse
import play.api.http.Status
import play.api.libs.ws.WSResponse

object HttpUtils {

  implicit class HttpResponseUtils(response: WSResponse) {

    def redirectLocation: Option[String] = (response.status, response.header("Location")) match {
      case (Status.SEE_OTHER, Some(header)) => Some(header)
      case _ => None
    }

  }

}
