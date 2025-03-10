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

package views.businessdetails

import org.scalatest.matchers.must.Matchers
import play.api.mvc.{AnyContentAsEmpty, Request}
import utils.AmlsViewSpec
import views.Fixture
import views.html.businessdetails.WhatYouNeedView

class WhatYouNeedViewSpec extends AmlsViewSpec with Matchers {

  lazy val what_you_need                                         = app.injector.instanceOf[WhatYouNeedView]
  implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView()

  trait ViewFixture extends Fixture

  "What you need View" must {
    "Have the correct title" in new ViewFixture {
      def view = what_you_need()

      doc.title must startWith(messages("title.wyn"))
    }

    "Have the correct Headings" in new ViewFixture {
      def view = what_you_need()

      heading.html    must be(messages("title.wyn"))
      subHeading.html must include(messages("summary.businessdetails"))
    }

    "contain the expected content elements" in new ViewFixture {
      def view = what_you_need()

      html must include(
        messages("if your business is currently registered with HMRC under the Money Laundering Regulations")
      )
      html must include(
        messages(
          "the date your business started or will start activities that need to be registered under the Money Laundering Regulations"
        )
      )
      html must include(messages("your VAT registration number, if you’re registered for VAT in the UK"))
      html must include(messages("the address for your registered office or main place of business"))
      html must include(messages("a contact email address, telephone number, and postal address"))
    }

    behave like pageWithBackLink(what_you_need())

  }
}
