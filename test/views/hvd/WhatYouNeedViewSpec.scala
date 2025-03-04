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

package views.hvd

import org.scalatest.matchers.must.Matchers
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.FakeRequest
import utils.AmlsViewSpec
import views.Fixture
import views.html.hvd.WhatYouNeedView

class WhatYouNeedViewSpec extends AmlsViewSpec with Matchers {

  lazy val wyn = app.injector.instanceOf[WhatYouNeedView]

  val call = controllers.hvd.routes.ProductsController.get()

  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  trait ViewFixture extends Fixture {
    implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView()
  }

  "What you need View" must {

    "Have the correct title" in new ViewFixture {
      def view = wyn(call)

      doc.title must startWith(messages("title.wyn"))
    }

    "Have the correct Headings" in new ViewFixture {
      def view = wyn(call)

      heading.html    must be(messages("title.wyn"))
      subHeading.html must include(messages("summary.hvd"))
    }

    "contain the expected content elements" in new ViewFixture {
      def view = wyn(call)

      html must include("what your business will buy or sell")
      html must include("how you’ll sell your goods, for example auction")
      html must include("the date of the first cash payment of €10,000 or more, if you have made or accepted any")
      html must include("if you can identify linked cash payments of €10,000 or more")
      html must include(
        "how you’ll receive cash payments of €10,000 or more from customers you have not met in person, if you receive any"
      )
      html must include("the percentage of your turnover you expect to come from cash payments of €10,000 or more")
      html must include("You may need to tell us:")
      html must include("if you’ll be buying or selling duty-suspended excise goods")

    }

    behave like pageWithBackLink(wyn(call))
  }
}
