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
import org.mockito.Mockito.{reset, when}
import controllers.{AmlsBaseController, CommonPlayDependencies}
import org.scalatest.{BeforeAndAfter, MustMatchers}
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.i18n.{Lang, MessagesApi, MessagesProvider}
import play.api.inject.bind
import play.api.inject.guice.{GuiceApplicationBuilder, GuiceableModule}
import play.api.libs.typedmap.TypedKey
import play.api.mvc.MessagesControllerComponents
import play.api.test.FakeRequest
import play.api.{Application, Mode}
import play.filters.csrf.CSRF.{Token, TokenInfo, TokenProvider}
import play.filters.csrf.{CSRFConfigProvider, CSRFFilter}
import uk.gov.hmrc.http.{HeaderCarrier, SessionKeys}
import uk.gov.hmrc.play.bootstrap.http.HttpClient

trait AmlsSpec extends PlaySpec with GuiceOneAppPerSuite with MockitoSugar with MustMatchers with AuthorisedFixture with  BeforeAndAfter {

  protected val bindModules: Seq[GuiceableModule] = Seq(bind[KeystoreConnector].to(mock[KeystoreConnector]))

  implicit override lazy val app: Application = new GuiceApplicationBuilder()
    .disable[com.kenshoo.play.metrics.PlayModule]
    .bindings(bindModules:_*).in(Mode.Test)
    .build()

  val commonDependencies = app.injector.instanceOf(classOf[CommonPlayDependencies])

  implicit lazy val messagesApi = app.injector.instanceOf(classOf[MessagesApi])
  implicit lazy val messages = messagesApi.preferred(FakeRequest())

  implicit val headerCarrier = HeaderCarrier()

  implicit val partialsProvider = app.injector.instanceOf(classOf[CachedStaticHtmlPartialProvider])

  implicit val lang = mock[Lang]
  implicit val appConfig = mock[ApplicationConfig]
  implicit val mat = mock[Materializer]
  implicit val messagesProvider = mock[MessagesProvider]

  val mockMcc = mock[MessagesControllerComponents]

  before {
    reset {
      commonDependencies
      messagesApi
      messages
      headerCarrier
      partialsProvider
      lang
      appConfig
      mat
      messagesProvider
      mockMcc
    }
  }

  when(appConfig.mongoEncryptionEnabled).thenReturn(false)


  def addToken[T](fakeRequest: FakeRequest[T]) = {
    import play.api.test.CSRFTokenHelper._

    val csrfConfig     = app.injector.instanceOf[CSRFConfigProvider].get
    val csrfFilter     = app.injector.instanceOf[CSRFFilter]
    val token          = csrfFilter.tokenProvider.generateToken

    //fakeRequest.withHeaders((csrfConfig.headerName, token)).withCSRFToken
    CSRFRequest(fakeRequest.withHeaders((csrfConfig.headerName, token))).withCSRFToken
  }

  def addTokenForView[T]() = {
    import play.api.test.CSRFTokenHelper._

    CSRFRequest(authRequest).withCSRFToken
  }

  def addTokenWithUrlEncodedBody[T](fakeRequest: FakeRequest[T])(data: (String,String)*) = {
    import play.api.test.CSRFTokenHelper._

    val csrfConfig     = app.injector.instanceOf[CSRFConfigProvider].get
    val csrfFilter     = app.injector.instanceOf[CSRFFilter]
    val token          = csrfFilter.tokenProvider.generateToken

    fakeRequest.withSession(SessionKeys.sessionId -> "fakesessionid")
      .withHeaders((csrfConfig.headerName, token)).withFormUrlEncodedBody(data:_*).withCSRFToken
  }

  def requestWithUrlEncodedBody(data: (String, String)*) = addTokenWithUrlEncodedBody(authRequest)(data:_*)

  def addTokenWithHeaders[T](fakeRequest: FakeRequest[T])(data: (String,String)*) = {
    import play.api.test.CSRFTokenHelper._

    val csrfConfig     = app.injector.instanceOf[CSRFConfigProvider].get
    val csrfFilter     = app.injector.instanceOf[CSRFFilter]
    val token          = csrfFilter.tokenProvider.generateToken

    fakeRequest.withSession(SessionKeys.sessionId -> "fakesessionid")
      .withHeaders((csrfConfig.headerName, token)).withHeaders(data:_*).withCSRFToken
  }

  def requestWithHeaders(data: (String, String)*) = addTokenWithHeaders(authRequest)(data:_*)
}
