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

package views.tcsp

import org.scalatest.matchers.must.Matchers
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.FakeRequest
import utils.AmlsViewSpec
import views.Fixture
import views.html.tcsp.WhatYouNeedView

class WhatYouNeedViewSpec extends AmlsViewSpec with Matchers {

  lazy val what_you_need                                    = inject[WhatYouNeedView]
  val call                                                  = controllers.tcsp.routes.TcspTypesController.get()
  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  trait ViewFixture extends Fixture {
    implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView()
  }

  "WhatYouNeedView" must {
    "have correct title" in new ViewFixture {

      def view = what_you_need(call)

      val title = messages("title.wyn") + " - " +
        messages("summary.tcsp") + " - " +
        messages("title.amls") + " - " + messages("title.gov")

      doc.title must be(title)
    }

    "have correct headings" in new ViewFixture {

      def view = what_you_need(call)

      heading.html    must be(messages("title.wyn"))
      subHeading.html must include(messages("summary.tcsp"))
    }

    "contain the expected content elements" in new ViewFixture {
      def view = what_you_need(call)

      html must include(messages("the type of trust or company service provider you are"))
      html must include(messages("if you use the services of another trust or company service provider"))
      html must include(
        messages(
          "your trust or company service provider’s Money Laundering Regulations number, if you use another provider"
        )
      )
      html must include(messages("You may also need to tell us:"))
      html must include(messages("if you sell off-the-shelf companies"))
      html must include(messages("which services your business provides"))
    }

    behave like pageWithBackLink(what_you_need(call))
  }
}
