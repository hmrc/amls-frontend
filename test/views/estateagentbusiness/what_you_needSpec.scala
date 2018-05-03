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

package views.estateagentbusiness

import org.scalatest.{MustMatchers}
import  utils.AmlsSpec
import play.api.i18n.Messages
import views.Fixture

class what_you_needSpec extends AmlsSpec with MustMatchers  {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)
  }

  "what_you_need view" must {
    "have correct title" in new ViewFixture {

      def view = views.html.estateagentbusiness.what_you_need()

      doc.title must startWith(Messages("title.wyn") + " - " + Messages("summary.estateagentbusiness"))
    }

    "have correct headings" in new ViewFixture {

      def view = views.html.estateagentbusiness.what_you_need()

      heading.html must be(Messages("title.wyn"))
      subHeading.html must include(Messages("summary.estateagentbusiness"))

    }

    "contain the expected content elements" in new ViewFixture{
      def view = views.html.estateagentbusiness.what_you_need()

      html must include(Messages("estateagentbusiness.whatyouneed.subheading"))
      html must include(Messages("estateagentbusiness.whatyouneed.line_1"))
      html must include(Messages("estateagentbusiness.whatyouneed.line_2"))
      html must include(Messages("estateagentbusiness.whatyouneed.line_3"))
    }
  }
}
