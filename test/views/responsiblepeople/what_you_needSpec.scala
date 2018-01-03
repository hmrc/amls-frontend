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

package views.responsiblepeople

import org.scalatest.MustMatchers
import play.api.i18n.Messages
import utils.GenericTestHelper
import views.Fixture


class what_you_needSpec extends GenericTestHelper with MustMatchers {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)
  }

  "What you need View" must {
    "Have the correct title" in new ViewFixture {
      def view = views.html.responsiblepeople.what_you_need(1)

      doc.title must startWith(Messages("title.wyn"))
    }

    "Have the correct Headings" in new ViewFixture{
      def view = views.html.responsiblepeople.what_you_need(1)

      heading.html must be (Messages("title.wyn"))
      subHeading.html must include (Messages("summary.responsiblepeople"))
    }

    "contain the expected content elements" in new ViewFixture{
      def view = views.html.responsiblepeople.what_you_need(1)

      html must include(Messages("responsiblepeople.whatyouneed.requiredinfo"))

      html must include(Messages("responsiblepeople.whatyouneed.possiblerequiredinfo"))

      html must include(Messages("responsiblepeople.whatyouneed.line_1"))
      html must include(Messages("responsiblepeople.whatyouneed.line_2"))
      html must include(Messages("responsiblepeople.whatyouneed.line_3"))
      html must include(Messages("responsiblepeople.whatyouneed.line_4"))
      html must include(Messages("responsiblepeople.whatyouneed.line_5"))
    }
  }
}
