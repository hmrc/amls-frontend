package connectors

import uk.gov.hmrc.play.http.{HttpReads, HttpResponse}

trait RawResponseReads {
  implicit val httpReads: HttpReads[HttpResponse] = new HttpReads[HttpResponse] {
    override def read(method: String, url: String, response: HttpResponse) = response
  }
}
