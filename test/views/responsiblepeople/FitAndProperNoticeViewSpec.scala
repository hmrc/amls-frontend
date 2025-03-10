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

package views.responsiblepeople

import org.scalatest.matchers.must.Matchers
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.FakeRequest
import utils.AmlsViewSpec
import views.Fixture
import views.html.responsiblepeople.FitAndProperNoticeView

class FitAndProperNoticeViewSpec extends AmlsViewSpec with Matchers {

  lazy val noticeView = inject[FitAndProperNoticeView]

  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  trait ViewFixture extends Fixture {
    implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView()
  }

  "FitAndProperNoticeView" must {

    "have the correct title" in new ViewFixture {
      def view = noticeView(true, 1, None)

      doc.title must startWith(messages("responsiblepeople.fit_and_proper.notice.title"))
    }

    "have the correct headings" in new ViewFixture {
      def view = noticeView(true, 1, None)

      heading.html    must be(messages("responsiblepeople.fit_and_proper.notice.title"))
      subHeading.html must include(messages("summary.responsiblepeople"))
    }

    "have the correct content" in new ViewFixture {
      def view = noticeView(true, 1, None)

      doc.text() must include(messages("responsiblepeople.fit_and_proper.notice.text1"))
      doc.text() must include(messages("responsiblepeople.fit_and_proper.notice.text2"))
      doc.text() must include(messages("responsiblepeople.fit_and_proper.notice.text3"))

      doc.text() must include(messages("responsiblepeople.fit_and_proper.notice.heading1"))
      doc.text() must include(messages("responsiblepeople.fit_and_proper.notice.heading2"))
    }

    behave like pageWithBackLink(noticeView(false, 1, None))
  }
}
