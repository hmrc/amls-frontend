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

package controllers.responsiblepeople

import config.ApplicationConfig
import controllers.actions.SuccessfulAuthAction
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.Helpers._
import play.api.test.Injecting
import utils.{AmlsSpec, DependencyMocks}
import views.html.responsiblepeople.FitAndProperNoticeView

class FitAndProperNoticeControllerSpec extends AmlsSpec with MockitoSugar with ScalaFutures with Injecting {

  val recordId = 1

  trait Fixture extends DependencyMocks { self =>
    val request                    = addToken(authRequest)
    lazy val mockApplicationConfig = mock[ApplicationConfig]

    lazy val controller = new FitAndProperNoticeController(
      mockCacheConnector,
      SuccessfulAuthAction,
      commonDependencies,
      mockMcc,
      inject[FitAndProperNoticeView]
    )
  }

  "FitAndProperNoticeController" when {
    "get is called" must {
      "display the notice page" in new Fixture {
        val result = controller.get(recordId)(request)
        status(result) must be(OK)
        val page: Document = Jsoup.parse(contentAsString(result))
        page.getElementsByClass("button")
        page.body().html() must include(messages("responsiblepeople.fit_and_proper.notice.title"))
        page.body().html() must include(messages("responsiblepeople.fit_and_proper.notice.text1"))
        page.body().html() must include(messages("responsiblepeople.fit_and_proper.notice.heading1"))
        page.body().html() must include(messages("responsiblepeople.fit_and_proper.notice.text2"))
        page.body().html() must include(messages("responsiblepeople.fit_and_proper.notice.heading2"))
        page.body().html() must include(messages("responsiblepeople.fit_and_proper.notice.text3"))
      }
    }

    "continue button is clicked" must {
      "redirect to Fit and Proper page" in new Fixture {
        val result = controller.get(recordId)(request)
        status(result) must be(OK)
        val page: Document = Jsoup.parse(contentAsString(result))
        page
          .getElementsMatchingOwnText(messages("button.continue"))
          .attr("href") mustBe routes.FitAndProperController.get(recordId).url
      }
    }
  }
}
