package views

import org.jsoup.Jsoup
import org.scalatest.MustMatchers
import play.api.mvc.Request
import play.api.test.FakeRequest
import play.twirl.api.HtmlFormat


trait ViewFixture extends MustMatchers{
  implicit val request : Request[_] = FakeRequest()

  def view: HtmlFormat.Appendable
  lazy val html = view.body
  lazy val doc = Jsoup.parse(html)
  lazy val form = doc.getElementsByTag("form").first()
  lazy val heading = doc.getElementsByTag("h1").first()
  lazy val subHeading = doc.getElementsByClass("heading-secondary").first()
  lazy val errorSummary = doc.getElementsByClass("amls-error-summary").first()
}
