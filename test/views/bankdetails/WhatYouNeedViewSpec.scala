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

package views.bankdetails

import org.scalatest.matchers.must.Matchers
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.FakeRequest
import utils.AmlsViewSpec
import views.Fixture
import views.html.bankdetails.WhatYouNeedView

class WhatYouNeedViewSpec extends AmlsViewSpec with Matchers {

  lazy val whatYouNeed = app.injector.instanceOf[WhatYouNeedView]

  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  trait ViewFixture extends Fixture {
    implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView()

    def view = whatYouNeed(controllers.bankdetails.routes.HasBankAccountController.get)
  }

  "What you need View" must {
    "Have the correct title" in new ViewFixture {
      doc.title must startWith(messages("title.wyn"))
    }

    "Have the correct Headings" in new ViewFixture {
      heading.html    must be(messages("title.wyn"))
      subHeading.html must include(messages("summary.bankdetails"))
    }

    "contain the expected content elements" in new ViewFixture {
      html must include(messages("bankdetails.whatyouneed.line_1"))
      html must include(messages("bankdetails.whatyouneed.line_2"))
      html must include(messages("bankdetails.whatyouneed.line_3"))
      html must include(messages("bankdetails.whatyouneed.line_4"))
    }

    "provide the expected paragraphs" in new ViewFixture {
      html must include(messages("bankdetails.whatyouneed.p.01"))
      html must include(messages("bankdetails.whatyouneed.p.02"))
    }

    behave like pageWithBackLink(whatYouNeed(controllers.bankdetails.routes.HasBankAccountController.get))
  }
}
