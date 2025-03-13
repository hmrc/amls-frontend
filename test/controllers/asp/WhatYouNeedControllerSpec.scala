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

package controllers.asp

import controllers.actions.SuccessfulAuthAction
import play.api.mvc.{AnyContentAsEmpty, Request, Result}
import play.api.test.Helpers._
import utils.{AmlsSpec, DependencyMocks}
import views.html.asp.WhatYouNeedView

import scala.concurrent.Future

class WhatYouNeedControllerSpec extends AmlsSpec {

  trait Fixture extends DependencyMocks {
    self =>
    val request: Request[AnyContentAsEmpty.type] = addToken(authRequest)

    lazy val whatYouNeed: WhatYouNeedView = app.injector.instanceOf[WhatYouNeedView]

    val controller = new WhatYouNeedController(SuccessfulAuthAction, ds = commonDependencies, cc = mockMcc, whatYouNeed)
  }

  "WhatYouNeedController" must {

    "get" must {

      "load the page" in new Fixture {

        val pageTitle: String = messages("title.wyn") + " - " +
          messages("summary.asp") + " - " +
          messages("title.amls") + " - " + messages("title.gov")

        val result: Future[Result] = controller.get()(request)
        status(result)          must be(OK)
        contentAsString(result) must include(pageTitle)
      }
    }
  }
}
