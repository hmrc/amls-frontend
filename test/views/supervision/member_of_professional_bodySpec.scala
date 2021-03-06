/*
 * Copyright 2021 HM Revenue & Customs
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
import utils.AmlsViewSpec
import views.Fixture
import views.html.supervision.member_of_professional_body


class member_of_professional_bodySpec extends AmlsViewSpec with MustMatchers  {

  trait ViewFixture extends Fixture {
    lazy val member_of_professional_body = app.injector.instanceOf[member_of_professional_body]
    implicit val requestWithToken = addTokenForView()
  }

  "what_you_need view" must {

    "have a back link" in new ViewFixture {

      def view = member_of_professional_body(EmptyForm, edit = false)

      doc.getElementsByAttributeValue("class", "link-back") must not be empty
    }
  }
}
