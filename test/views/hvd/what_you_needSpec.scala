/*
 * Copyright 2020 HM Revenue & Customs
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

import org.scalatest.WordSpec
import org.scalatest.MustMatchers
import utils.AmlsViewSpec
import play.api.i18n.Messages
import views.Fixture
import views.html.hvd.what_you_need

class what_you_needSpec extends AmlsViewSpec with MustMatchers {

  trait ViewFixture extends Fixture {
    lazy val what_you_need = app.injector.instanceOf[what_you_need]
    implicit val requestWithToken = addTokenForView()
  }

  "What you need View" must {

    "have the back link button" in new ViewFixture {
      def view = what_you_need()

      doc.getElementsByAttributeValue("class", "link-back") must not be empty
    }

    "Have the correct title" in new ViewFixture {
      def view = what_you_need()

      doc.title must startWith(Messages("title.wyn"))
    }

    "Have the correct Headings" in new ViewFixture{
      def view = what_you_need()

      heading.html must be (Messages("title.wyn"))
      subHeading.html must include (Messages("summary.hvd"))
    }

    "contain the expected content elements" in new ViewFixture{
      def view = what_you_need()

      html must include(Messages("what your business will buy or sell"))
      html must include(Messages("how you’ll sell your goods, for example auction"))
      html must include(Messages("the date of the first cash payment of €10,000 or more, if you have made or accepted any"))
      html must include(Messages("if you can identify linked cash payments of €10,000 or more"))
      html must include(Messages("how you’ll receive cash payments of €10,000 or more from customers you have not met in person, if you receive any"))
      html must include(Messages("the percentage of your turnover you expect to come from cash payments of €10,000 or more"))
      html must include(Messages("You may need to tell us:"))
      html must include(Messages("if you’ll be buying or selling duty-suspended excise goods"))

    }
  }
}
