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

package views.asp

import org.scalatest.matchers.must.Matchers
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.FakeRequest
import utils.AmlsViewSpec
import views.Fixture
import views.html.asp.WhatYouNeedView

class WhatYouNeedViewSpec extends AmlsViewSpec with Matchers {

  lazy val whatYouNeed                                      = app.injector.instanceOf[WhatYouNeedView]
  val call                                                  = controllers.asp.routes.ServicesOfBusinessController.get()
  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  trait ViewFixture extends Fixture {
    implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView()
  }

  "WhatYouNeedView" must {

    "Have the correct title" in new ViewFixture {
      def view = whatYouNeed(call)

      doc.title must startWith(messages("title.wyn"))
    }

    "Have the correct Headings" in new ViewFixture {
      def view = whatYouNeed(call)

      heading.html    must be(messages("title.wyn"))
      subHeading.html must include(messages("summary.asp"))
    }

    "contain the expected content elements" in new ViewFixture {
      def view = whatYouNeed(call)

      html must include(messages("You’ll need to tell us:"))
      html must include(messages("which accountancy services your business provides"))
      html must include(messages("if you’re registered with HMRC to handle other businesses’ tax matters"))
    }

    behave like pageWithBackLink(whatYouNeed(call))
  }
}
