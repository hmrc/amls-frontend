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

package views.businessdetails

import forms.{EmptyForm, Form2}
import org.scalatest.MustMatchers
import utils.AmlsViewSpec
import play.api.i18n.Messages
import views.Fixture
import views.html.businessdetails.what_you_need


class what_you_needSpec extends AmlsViewSpec with MustMatchers {

  trait ViewFixture extends Fixture {
    lazy val what_you_need = app.injector.instanceOf[what_you_need]
    implicit val requestWithToken = addTokenForView()
  }

  "What you need View" must {
    "Have the correct title" in new ViewFixture {
      def view = what_you_need()

      doc.title must startWith(Messages("title.wyn"))
    }

    "Have the correct Headings" in new ViewFixture{
      def view = what_you_need()

      heading.html must be (Messages("title.wyn"))
      subHeading.html must include (Messages("summary.businessdetails"))
    }

    "contain the expected content elements" in new ViewFixture{
      def view = what_you_need()

      html must include(Messages("if your business is currently registered with HMRC under the Money Laundering Regulations"))
      html must include(Messages("the date your business started or will start activities that need to be registered under the Money Laundering Regulations"))
      html must include(Messages("your VAT registration number, if you’re registered for VAT in the UK"))
      html must include(Messages("the address for your registered office or main place of business"))
      html must include(Messages("a contact email address, telephone number, and postal address"))
    }

    "have a back link" in new ViewFixture {
      val form2: Form2[_] = EmptyForm

      def view = what_you_need()

      doc.getElementsByAttributeValue("class", "link-back") must not be empty
    }
  }
}
