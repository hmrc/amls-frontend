/*
 * Copyright 2022 HM Revenue & Customs
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

package views.supervision

import org.scalatest.MustMatchers
import play.api.i18n.Messages
import utils.AmlsViewSpec
import views.Fixture
import views.html.supervision.what_you_need


class what_you_needSpec extends AmlsViewSpec with MustMatchers  {

  trait ViewFixture extends Fixture {
    lazy val what_you_need = app.injector.instanceOf[what_you_need]
    implicit val requestWithToken = addTokenForView()
  }

  "what_you_need view" must {

    "have a back link" in new Fixture {
      lazy val what_you_need = app.injector.instanceOf[what_you_need]
      implicit val requestWithToken = addTokenForView()

      def view = what_you_need()

      doc.getElementsByAttributeValue("class", "link-back") must not be empty
    }

    "contain the expected content elements" in new ViewFixture {
      def view = what_you_need()

      html must include(Messages("if your business has been registered with another supervisory body under the Money Laundering Regulations"))
      html must include(Messages("which professional bodies you’re a member of, if any"))
      html must include(Messages("if your business, or anyone in your business, has been penalised for activities covered by the Money Laundering Regulations"))
      html must include(Messages("If you have been registered with another supervisory body, you’ll need to tell us the:"))
      html must include(Messages("name of your previous supervisory body"))
      html must include(Messages("dates your supervision started and ended"))
      html must include(Messages("reason why your supervision ended"))
    }

  }
}
