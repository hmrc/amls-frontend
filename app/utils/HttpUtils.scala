package utils

import uk.gov.hmrc.play.http.HttpResponse
import play.api.http.Status

object HttpUtils {
  implicit class HttpResponseUtils(response: HttpResponse) {

    def headerValue(header: String): Option[String] = response.allHeaders match {
      case headers if headers.get(header).isDefined =>
        headers.get(header) match {
          case Some(s) if s.nonEmpty => Some(s.head)
          case _ => None
        }
      case _ => None
    }

    def redirectLocation: Option[String] = headerValue("Location")
  }
}

