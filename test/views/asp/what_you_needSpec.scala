/*
 * Copyright 2023 HM Revenue & Customs
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

import org.scalatest.MustMatchers
import utils.AmlsViewSpec
import play.api.i18n.Messages
import views.Fixture
import views.html.asp.what_you_need

class what_you_needSpec extends AmlsViewSpec with MustMatchers {

  trait ViewFixture extends Fixture {
    lazy val whatYouNeed = app.injector.instanceOf[what_you_need]
    implicit val requestWithToken = addTokenForView()
  }

  "What you need View" must {

    "have a back link" in new ViewFixture {

      def view = whatYouNeed()

      doc.getElementsByAttributeValue("class", "link-back") must not be empty
    }

    "Have the correct title" in new ViewFixture {
      def view = whatYouNeed()

      doc.title must startWith(Messages("title.wyn"))
    }

    "Have the correct Headings" in new ViewFixture{
      def view = whatYouNeed()

      heading.html must be (Messages("title.wyn"))
      subHeading.html must include (Messages("summary.asp"))
    }

    "contain the expected content elements" in new ViewFixture{
      def view = whatYouNeed()

      html must include(Messages("You’ll need to tell us:"))
      html must include(Messages("which accountancy services your business provides"))
      html must include(Messages("if you’re registered with HMRC to handle other businesses’ tax matters"))
    }
  }
}
