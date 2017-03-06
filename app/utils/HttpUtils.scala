package utils

import uk.gov.hmrc.play.http.HttpResponse
import play.api.http.Status

object HttpUtils {

  implicit class HttpResponseUtils(response: HttpResponse) {

    def redirectLocation: Option[String] = (response.status, response.allHeaders) match {
      case (Status.SEE_OTHER, headers) if headers.get("Location").isDefined =>
        headers.get("Location") match {
          case Some(location :: _) => Some(location)
          case _ => None
        }
      case _ => None
    }

  }

}
