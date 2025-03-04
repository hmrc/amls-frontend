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

package controllers

import connectors.DataCacheConnector
import controllers.actions.SuccessfulAuthAction
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.Result
import play.api.test.Helpers.{OK, status, _}
import utils.{AmlsSpec, AuthAction, DependencyMocks}

import scala.concurrent.Future

class LoginEventControllerSpec extends AmlsSpec with MockitoSugar with ScalaFutures {

  trait Fixture extends DependencyMocks { self =>

    val request = addToken(authRequest)

    lazy val defaultBuilder: GuiceApplicationBuilder = new GuiceApplicationBuilder()
      .overrides(bind[AuthAction].to(SuccessfulAuthAction))
      .configure(
        "play.filters.disabled" -> List("uk.gov.hmrc.play.bootstrap.frontend.filters.crypto.SessionCookieCryptoFilter")
      )
      .overrides(bind[DataCacheConnector].to(mockCacheConnector))

    val builder: GuiceApplicationBuilder      = defaultBuilder
    lazy val app: Application                 = builder.build()
    lazy val controller: LoginEventController = app.injector.instanceOf[LoginEventController]
  }

  "LoginEventController" must {
    "show login event page" in new Fixture {
      val result: Future[Result] = controller.get()(request)

      status(result) must be(OK)
    }
  }
}
