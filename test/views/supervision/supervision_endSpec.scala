/*
 * Copyright 2019 HM Revenue & Customs
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

import forms.EmptyForm
import org.scalatest.MustMatchers
import play.api.i18n.Messages
import utils.AmlsSpec
import views.Fixture


class supervision_endSpec extends AmlsSpec with MustMatchers  {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)
  }

  "supervision_end view" must {

    "have correct title, headings and form fields" in new ViewFixture {
      val form2 = EmptyForm

      def view = views.html.supervision.supervision_end(EmptyForm, edit = false)

      doc.title must startWith(Messages("supervision.supervision_end.title"))
      heading.html must be(Messages("supervision.supervision_end.title"))
      subHeading.html must include(Messages("summary.supervision"))
    }

    "have a back link" in new ViewFixture {

      def view = views.html.supervision.supervision_end(EmptyForm, edit = false)

      doc.getElementsByAttributeValue("class", "link-back") must not be empty
    }
  }
}
