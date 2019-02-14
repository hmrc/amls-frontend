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

package controllers.responsiblepeople

import config.AppConfig
import connectors.{DataCacheConnector, KeystoreConnector}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import play.api.i18n.Messages
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers._
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.{AmlsSpec, AuthorisedFixture, DependencyMocks}

class FitAndProperNoticeControllerSpec extends AmlsSpec with MockitoSugar with ScalaFutures {

  val recordId = 1

  trait Fixture extends AuthorisedFixture with DependencyMocks { self =>
    val request = addToken(authRequest)
    lazy val mockAppConfig = mock[AppConfig]
    lazy val defaultBuilder = new GuiceApplicationBuilder()
      .overrides(bind[KeystoreConnector].to(mock[KeystoreConnector]))
      .disable[com.kenshoo.play.metrics.PlayModule]
      .overrides(bind[AuthConnector].to(self.authConnector))
      .overrides(bind[DataCacheConnector].to(mockCacheConnector))
      .overrides(bind[AppConfig].to(mockAppConfig))

    val builder = defaultBuilder
    lazy val app = builder.build()
    lazy val controller = app.injector.instanceOf[FitAndProperNoticeController]

  }

  "FitAndProperNoticeController" when {
    "get is called" must {
      "display the notice page" in new Fixture {
        val result = controller.get(recordId)(request)
        status(result) must be(OK)
        val page: Document = Jsoup.parse(contentAsString(result))
        page.getElementsByClass("button")
        page.body().html() must include(Messages("responsiblepeople.fit_and_proper.notice.title"))
        page.body().html() must include(Messages("responsiblepeople.fit_and_proper.notice.text1"))
        page.body().html() must include(Messages("responsiblepeople.fit_and_proper.notice.heading1"))
        page.body().html() must include(Messages("responsiblepeople.fit_and_proper.notice.text2"))
        page.body().html() must include(Messages("responsiblepeople.fit_and_proper.notice.heading2"))
        page.body().html() must include(Messages("responsiblepeople.fit_and_proper.notice.text3"))
      }
    }

    "continue button is clicked" must {
      "redirect to Fit and Proper page" in new Fixture {
        val result = controller.get(recordId)(request)
        status(result) must be(OK)
        val page: Document = Jsoup.parse(contentAsString(result))
        page.getElementsMatchingOwnText(Messages("button.continue"))
          .attr("href") mustBe routes.FitAndProperController.get(recordId).url
      }
    }
  }
}
