/*
 * Copyright 2022 HM Revenue & Customs
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
import config.ApplicationConfig
import connectors.KeystoreConnector
import controllers.CommonPlayDependencies
import org.mockito.Mockito.when
import org.scalatest.MustMatchers
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n.{Lang, MessagesApi}
import play.api.inject.bind
import play.api.inject.guice.{GuiceApplicationBuilder, GuiceableModule}
import play.api.mvc.MessagesControllerComponents
import play.api.test.FakeRequest
import play.api.{Application, Mode}
import uk.gov.hmrc.http.HeaderCarrier

trait AmlsViewSpec extends PlaySpec with GuiceOneAppPerSuite with MockitoSugar with MustMatchers with AuthorisedFixture {

  protected val bindModules: Seq[GuiceableModule] = Seq(bind[KeystoreConnector].to(mock[KeystoreConnector]))

  implicit override lazy val app: Application = new GuiceApplicationBuilder()
    .disable[com.kenshoo.play.metrics.PlayModule]
    .bindings(bindModules:_*).in(Mode.Test)
    .build()

  val commonDependencies = app.injector.instanceOf(classOf[CommonPlayDependencies])

  implicit lazy val messagesApi = app.injector.instanceOf(classOf[MessagesApi])
  implicit lazy  val messages = messagesApi.preferred(FakeRequest())

  implicit val headerCarrier = HeaderCarrier()

  implicit val lang = mock[Lang]
  implicit val appConfig = mock[ApplicationConfig]
  implicit val mat = mock[Materializer]

  val mockMcc = mock[MessagesControllerComponents]

  def addTokenForView[T]() = {
    import play.api.test.CSRFTokenHelper._

    CSRFRequest(authRequest).withCSRFToken
  }

  when(appConfig.logoutUrl).thenReturn("some url")
}
