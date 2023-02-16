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

package views.tcsp

import org.scalatest.MustMatchers
import play.api.i18n.Messages
import utils.AmlsViewSpec
import views.Fixture
import views.html.tcsp.what_you_need


class what_you_needSpec extends AmlsViewSpec with MustMatchers {

  trait ViewFixture extends Fixture {
    lazy val what_you_need = app.injector.instanceOf[what_you_need]
    implicit val requestWithToken = addTokenForView()
  }

  "what_you_need view" must {
    "have correct title" in new ViewFixture {

      def view = what_you_need()

      doc.getElementsByAttributeValue("class", "link-back") must not be empty

      val title = Messages("title.wyn") + " - " +
        Messages("summary.tcsp") + " - " +
        Messages("title.amls") + " - " + Messages("title.gov")

      doc.title must be(title)
    }

    "have correct headings" in new ViewFixture {

      def view = what_you_need()

      heading.html must be(Messages("title.wyn"))
      subHeading.html must include(Messages("summary.tcsp"))

    }

    "contain the expected content elements" in new ViewFixture {
      def view = what_you_need()

      html must include(Messages("the type of trust or company service provider you are"))
      html must include(Messages("if you use the services of another trust or company service provider"))
      html must include(Messages("your trust or company service provider’s Money Laundering Regulations number, if you use another provider"))
      html must include(Messages("You may also need to tell us:"))
      html must include(Messages("if you sell off-the-shelf companies"))
      html must include(Messages("which services your business provides"))
    }
  }
}