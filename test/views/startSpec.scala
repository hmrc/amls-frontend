/*
 * Copyright 2018 HM Revenue & Customs
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

package views

import org.scalatest.{MustMatchers}
import  utils.GenericTestHelper
import play.api.i18n.Messages

class startSpec extends GenericTestHelper with MustMatchers {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)
  }

  "Landing Page View" must {

    "Have the correct title" in new ViewFixture {
      def view = views.html.start()
      doc.title must startWith(Messages("start.title"))
    }

    "Have the correct Headings" in new ViewFixture{
      def view = views.html.start()

      heading.html must be (Messages("start.title"))

    }

    "contain the expected content elements" in new ViewFixture{
      def view = views.html.start()

      html must include(Messages("start.line1"))
      html must include(Messages("start.line2"))
      html must include(Messages("start.line3"))
      html must include(Messages("start.before.heading"))
      html must include(Messages("start.utr.line1"))
      html must include(Messages("start.before.line1"))

    }
  }
}