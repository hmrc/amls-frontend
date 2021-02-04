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

package views.responsiblepeople

import org.scalatest.MustMatchers
import play.api.i18n.Messages
import utils.AmlsViewSpec
import views.Fixture
import views.html.responsiblepeople.who_must_register


class who_must_registerSpec extends AmlsViewSpec with MustMatchers {

  trait ViewFixture extends Fixture {
    lazy val who_must_register = app.injector.instanceOf[who_must_register]
    implicit val requestWithToken = addTokenForView()
  }

  "who_must_register View" must {

    "have a back link" in new ViewFixture {
      def view = who_must_register(1)
      doc.getElementsByAttributeValue("class", "link-back") must not be empty
    }

    "Have the correct title" in new ViewFixture {
      def view = who_must_register(1)

      doc.title must be(Messages("responsiblepeople.whomustregister.ymr") +
        " - " + Messages("summary.responsiblepeople") +
        " - " + Messages("title.amls") +
        " - " + Messages("title.gov"))
    }

    "Have the correct Headings" in new ViewFixture{
      def view = who_must_register(1)

      heading.html must be (Messages("responsiblepeople.whomustregister.ymr"))
      subHeading.html must include (Messages("summary.responsiblepeople"))
    }

    "contain the expected content elements" in new ViewFixture{
      def view = who_must_register(1)


      html must include(Messages("owners, partners, directors, shadow directors, and designated members"))
      html must include(Messages("the nominated officer for your business"))
      html must include(Messages("beneficial owners or shareholders who own or control more than 25% of the business"))
      html must include(Messages("other officers of the business, like the company secretary"))
      html must include(Messages("senior managers of activities covered by the Money Laundering Regulations"))
    }
  }
}
