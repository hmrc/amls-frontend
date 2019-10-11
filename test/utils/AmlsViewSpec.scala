/*
 * Copyright 2019 HM Revenue & Customs
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

import akka.stream.Materializer
import config.{ApplicationConfig, CachedStaticHtmlPartialProvider}
import connectors.KeystoreConnector
import controllers.CommonPlayDependencies
import org.mockito.Mockito.when
import org.scalatest.MustMatchers
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.i18n.{Lang, MessagesApi}
import play.api.inject.bind
import play.api.inject.guice.{GuiceApplicationBuilder, GuiceableModule}
import play.api.mvc.MessagesControllerComponents
import play.api.test.FakeRequest
import play.api.{Application, Mode}
import play.filters.csrf.{CSRFConfigProvider, CSRFFilter}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.{HeaderCarrier, SessionKeys}

trait AmlsViewSpec extends PlaySpec with OneAppPerSuite with MockitoSugar with MustMatchers {

  var authConnector = mock[AuthConnector]

  val authRequest = FakeRequest().withSession(
    SessionKeys.sessionId -> "SessionId",
    SessionKeys.token -> "Token",
    SessionKeys.userId -> "Test User",
    SessionKeys.authToken -> ""
  )

  protected val bindModules: Seq[GuiceableModule] = Seq(bind[KeystoreConnector].to(mock[KeystoreConnector]))

  implicit override lazy val app: Application = new GuiceApplicationBuilder()
    .disable[com.kenshoo.play.metrics.PlayModule]
    .bindings(bindModules:_*).in(Mode.Test)
    .build()

  val commonDependencies = app.injector.instanceOf(classOf[CommonPlayDependencies])

  implicit lazy val messagesApi = app.injector.instanceOf(classOf[MessagesApi])
  implicit lazy  val messages = messagesApi.preferred(FakeRequest())

  implicit val headerCarrier = HeaderCarrier()

  implicit val partialsProvider = app.injector.instanceOf(classOf[CachedStaticHtmlPartialProvider])

  implicit val lang = mock[Lang]
  implicit val appConfig = mock[ApplicationConfig]
  implicit val mat = mock[Materializer]

  val mockMcc = mock[MessagesControllerComponents]

  def addToken[T](fakeRequest: FakeRequest[T]) = {
    import play.api.test.CSRFTokenHelper._

    val csrfConfig     = app.injector.instanceOf[CSRFConfigProvider].get
    val csrfFilter     = app.injector.instanceOf[CSRFFilter]
    val token          = csrfFilter.tokenProvider.generateToken

    fakeRequest.withHeaders((csrfConfig.headerName, token)).withCSRFToken
  }

  def addTokenForView[T]() = {
    import play.api.test.CSRFTokenHelper._

    CSRFRequest(authRequest).withCSRFToken
  }

  when(appConfig.logoutUrl).thenReturn("some url")
}
