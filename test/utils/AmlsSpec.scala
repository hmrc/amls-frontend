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
import org.scalatest.AppendedClues
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.i18n._
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.{AnyContentAsEmpty, MessagesControllerComponents, Request}
import play.api.test.FakeRequest
import play.filters.csrf.{CSRFConfigProvider, CSRFFilter}
import uk.gov.hmrc.crypto.{ApplicationCrypto, Decrypter, Encrypter}
import uk.gov.hmrc.http.{HeaderCarrier, SessionKeys}

import scala.concurrent.ExecutionContext

trait AmlsSpec
    extends PlaySpec
    with GuiceOneAppPerSuite
    with MockitoSugar
    with ScalaFutures
    with Matchers
    with AuthorisedFixture
    with AppendedClues {

  import play.api.test.CSRFTokenHelper._

  override def fakeApplication(): Application =
    new GuiceApplicationBuilder()
      .configure(
        "play.filters.disabled" -> List("uk.gov.hmrc.play.bootstrap.frontend.filters.crypto.SessionCookieCryptoFilter")
      )
      .build()

  implicit val requestWithToken: Request[AnyContentAsEmpty.type] = CSRFRequest(FakeRequest()).withCSRFToken
  val messagesApi: MessagesApi                                   = app.injector.instanceOf[MessagesApi]
  implicit val messages: Messages                                = messagesApi.preferred(requestWithToken)
  implicit val lang: Lang                                        = Lang.defaultLang
  implicit val appConfig: ApplicationConfig                      = app.injector.instanceOf[ApplicationConfig]

  val commonDependencies = new CommonPlayDependencies(appConfig, messagesApi)

  implicit val messagesProvider: MessagesProvider = MessagesImpl(lang, messagesApi)
  implicit val ec: ExecutionContext               = app.injector.instanceOf[ExecutionContext]
  implicit val mat: Materializer                  = mock[Materializer]

  lazy val mockMcc: MessagesControllerComponents = app.injector.instanceOf[MessagesControllerComponents]

  // ================================== Encryption/Decryption ==================================
  val applicationCrypto: ApplicationCrypto                        = app.injector.instanceOf[ApplicationCrypto]
  implicit val compositeSymmetricCrypto: Encrypter with Decrypter = applicationCrypto.JsonCrypto
  // ===========================================================================================

  implicit val headerCarrier: HeaderCarrier = mock[HeaderCarrier]

  lazy val errorView = app.injector.instanceOf[views.html.ErrorView]

  def addToken[T](fakeRequest: FakeRequest[T]) = {
    import play.api.test.CSRFTokenHelper._

    val csrfConfig = app.injector.instanceOf[CSRFConfigProvider].get
    val csrfFilter = app.injector.instanceOf[CSRFFilter]
    val token      = csrfFilter.tokenProvider.generateToken

    CSRFRequest(fakeRequest.withHeaders((csrfConfig.headerName, token))).withCSRFToken
  }

  def addTokenWithUrlEncodedBody[T](fakeRequest: FakeRequest[T])(data: (String, String)*) = {
    import play.api.test.CSRFTokenHelper._

    val csrfConfig = app.injector.instanceOf[CSRFConfigProvider].get
    val csrfFilter = app.injector.instanceOf[CSRFFilter]
    val token      = csrfFilter.tokenProvider.generateToken

    fakeRequest
      .withSession(SessionKeys.sessionId -> "fakesessionid")
      .withHeaders((csrfConfig.headerName, token))
      .withFormUrlEncodedBody(data: _*)
      .withCSRFToken
  }

  def requestWithUrlEncodedBody(data: (String, String)*) = addTokenWithUrlEncodedBody(authRequest)(data: _*)

  def addTokenWithHeaders[T](fakeRequest: FakeRequest[T])(data: (String, String)*) = {
    import play.api.test.CSRFTokenHelper._

    val csrfConfig = app.injector.instanceOf[CSRFConfigProvider].get
    val csrfFilter = app.injector.instanceOf[CSRFFilter]
    val token      = csrfFilter.tokenProvider.generateToken

    fakeRequest
      .withSession(SessionKeys.sessionId -> "fakesessionid")
      .withHeaders((csrfConfig.headerName, token))
      .withHeaders(data: _*)
      .withCSRFToken
  }

  def requestWithHeaders(data: (String, String)*) = addTokenWithHeaders(authRequest)(data: _*)

  def addTokenWithSessionParam[T](fakeRequest: FakeRequest[T])(sessionParameter: (String, String)) = {
    import play.api.test.CSRFTokenHelper._

    val csrfConfig = app.injector.instanceOf[CSRFConfigProvider].get
    val csrfFilter = app.injector.instanceOf[CSRFFilter]
    val token      = csrfFilter.tokenProvider.generateToken

    CSRFRequest(fakeRequest.withSession(sessionParameter).withHeaders((csrfConfig.headerName, token))).withCSRFToken
  }
}
