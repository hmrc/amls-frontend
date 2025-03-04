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
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.i18n.{Lang, Messages, MessagesApi}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.{FakeRequest, Injecting}
import uk.gov.hmrc.auth.core.AuthConnector
import views.HtmlAssertions

trait AmlsSummaryViewSpec
    extends PlaySpec
    with GuiceOneAppPerSuite
    with MockitoSugar
    with Matchers
    with HtmlAssertions
    with Injecting {

  override def fakeApplication(): Application =
    new GuiceApplicationBuilder()
      .configure(
        "play.filters.disabled" -> List("uk.gov.hmrc.play.bootstrap.frontend.filters.crypto.SessionCookieCryptoFilter")
      )
      .build()

  var authConnector = mock[AuthConnector]

  def addTokenForView[T](request: Request[T]) = {
    import play.api.test.CSRFTokenHelper._

    CSRFRequest(request).withCSRFToken
  }

  implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView(FakeRequest())
  val messagesApi: MessagesApi                                   = app.injector.instanceOf[MessagesApi]
  implicit val messages: Messages                                = messagesApi.preferred(requestWithToken)
  implicit val lang: Lang                                        = Lang.defaultLang
  implicit val appConfig: ApplicationConfig                      = mock[ApplicationConfig]
}
