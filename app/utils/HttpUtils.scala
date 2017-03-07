package utils

import uk.gov.hmrc.play.http.HttpResponse
import play.api.http.Status

object HttpUtils {
  implicit class HttpResponseUtils(response: HttpResponse) {

    def redirectLocation: Option[String] = response.allHeaders match {
        case headers if headers.get("Location").isDefined =>
          headers.get("Location") match {
            case Some(s) if s.nonEmpty => Some(s.head)
            case _ => None
          }
        case _ => None
      }
    }

}

