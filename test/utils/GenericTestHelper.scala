/*
 * Copyright 2018 HM Revenue & Customs
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

import connectors.DataCacheConnector
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.i18n.MessagesApi
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.{Application, Mode}
import play.api.inject.guice.{GuiceApplicationBuilder, GuiceableModule}
import play.filters.csrf.CSRF.Token
import play.filters.csrf.{CSRFConfigProvider, CSRFFilter}

trait GenericTestHelper extends PlaySpec with OneAppPerSuite with MockitoSugar {

  protected val bindModules: Seq[GuiceableModule] = Seq()


  implicit override lazy val app: Application = new GuiceApplicationBuilder()
    .bindings(bindModules:_*).in(Mode.Test)
    .build()

  implicit val messagesApi = app.injector.instanceOf[MessagesApi]
  implicit val messages = messagesApi.preferred(FakeRequest())


  def addToken[T](fakeRequest: FakeRequest[T]) = {
    val csrfConfig     = app.injector.instanceOf[CSRFConfigProvider].get
    val csrfFilter     = app.injector.instanceOf[CSRFFilter]
    val token          = csrfFilter.tokenProvider.generateToken

    fakeRequest.copyFakeRequest(tags = fakeRequest.tags ++ Map(
      Token.NameRequestTag  -> csrfConfig.tokenName,
      Token.RequestTag      -> token
    )).withHeaders((csrfConfig.headerName, token))
  }

}
