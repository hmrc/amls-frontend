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

package views.declaration

import org.scalatest.MustMatchers
import play.api.i18n.Messages
import utils.AmlsViewSpec
import views.Fixture
import views.html.declaration.register_responsible_person

class register_responsible_personSpec extends AmlsViewSpec with MustMatchers {

  trait ViewFixture extends Fixture {
    lazy val register_responsible_person = app.injector.instanceOf[register_responsible_person]
    implicit val requestWithToken = addTokenForView()
  }

  "What you need View" must {
    "Have the correct title" in new ViewFixture {
      def view = register_responsible_person("subheading")

      doc.title must startWith(Messages("declaration.register.responsible.person.title"))
    }

    "Have the correct Headings" in new ViewFixture{
      def view = register_responsible_person("subheading")

      heading.html must be (Messages("declaration.register.responsible.person.title"))
      subHeading.html must include (Messages("subheading"))
    }

    "have a back link" in new ViewFixture {

      def view = register_responsible_person("subheading")

      doc.getElementsByAttributeValue("class", "link-back") must not be empty
    }

    "contain the expected content elements" in new ViewFixture{
      def view = register_responsible_person("subheading")

      html must include(Messages("declaration.register.responsible.person.description"))
      html must include(Messages("declaration.register.responsible.person.title"))

      html must include(Messages("subheading"))

      html must include(Messages("declaration.register.responsible.person.text"))
      html must include(Messages("owners, partners, directors, shadow directors, and designated members"))
      html must include(Messages("the nominated officer for your business"))
      html must include(Messages("beneficial owners or shareholders who own or control more than 25% of the business"))
      html must include(Messages("other officers of the business, like the company secretary"))
      html must include(Messages("senior managers of activities covered by the Money Laundering Regulations"))
    }
  }
}