/*
 * Copyright 2024 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package utils

import config.ApplicationConfig
import controllers.CommonPlayDependencies
import org.apache.pekko.stream.Materializer
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.Mockito.when
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.i18n.{Lang, Messages, MessagesApi}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.MessagesControllerComponents
import play.api.test.{FakeRequest, Injecting}
import play.twirl.api.Html
import uk.gov.hmrc.http.HeaderCarrier

trait AmlsViewSpec
    extends PlaySpec
    with GuiceOneAppPerSuite
    with MockitoSugar
    with Matchers
    with AuthorisedFixture
    with Injecting {

  implicit override lazy val app: Application = new GuiceApplicationBuilder()
    .configure(
      "play.filters.disabled" -> List("uk.gov.hmrc.play.bootstrap.frontend.filters.crypto.SessionCookieCryptoFilter")
    )
    .build()

  val commonDependencies = app.injector.instanceOf(classOf[CommonPlayDependencies])

  implicit lazy val messagesApi: MessagesApi = app.injector.instanceOf(classOf[MessagesApi])
  implicit lazy val messages: Messages       = messagesApi.preferred(FakeRequest())

  implicit val headerCarrier: HeaderCarrier = HeaderCarrier()

  implicit val lang: Lang                   = mock[Lang]
  implicit val appConfig: ApplicationConfig = mock[ApplicationConfig]
  implicit val mat: Materializer            = mock[Materializer]

  val mockMcc = mock[MessagesControllerComponents]

  def addTokenForView() = {
    import play.api.test.CSRFTokenHelper._

    CSRFRequest(authRequest).withCSRFToken
  }

  when(appConfig.logoutUrl).thenReturn("some url")

  // Assertion helper methods
  def pageWithBackLink(html: Html): Unit =
    "have a back link" in {
      def doc: Document = Jsoup.parse(html.body)
      assert(
        doc.getElementsByClass("govuk-back-link") != null,
        "\n\nElement " + "govuk-back-link" + " was not rendered on the page.\n"
      )
    }

  def pageWithErrors(view: Html, idSelectorPrefix: String, errorMessage: String): Unit =
    s"show errors correctly for $idSelectorPrefix field" in {

      def doc: Document = Jsoup.parse(view.body)

      doc.getElementsByClass("govuk-error-summary__list").first().text() must include(messages(errorMessage))

      doc.getElementById(s"$idSelectorPrefix-error").text() must include(messages(errorMessage))

    }

}
