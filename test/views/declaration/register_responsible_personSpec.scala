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

package views.declaration

import org.scalatest.MustMatchers
import play.api.i18n.Messages
import utils.AmlsSpec
import views.Fixture

class register_responsible_personSpec extends AmlsSpec with MustMatchers {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)
  }

  "What you need View" must {
    "Have the correct title" in new ViewFixture {
      def view = views.html.declaration.register_responsible_person("subheading")

      doc.title must startWith(Messages("declaration.register.responsible.person.title"))
    }

    "Have the correct Headings" in new ViewFixture{
      def view = views.html.declaration.register_responsible_person("subheading")

      heading.html must be (Messages("declaration.register.responsible.person.title"))
      subHeading.html must include (Messages("subheading"))
    }

    "have a back link" in new ViewFixture {

      def view = views.html.declaration.register_responsible_person("subheading")

      doc.getElementsByAttributeValue("class", "link-back") must not be empty
    }

    "contain the expected content elements" in new ViewFixture{
      def view = views.html.declaration.register_responsible_person("subheading")

      html must include(Messages("declaration.register.responsible.person.description"))
      html must include(Messages("declaration.register.responsible.person.title"))

      html must include(Messages("subheading"))

      html must include(Messages("declaration.register.responsible.person.text"))
      html must include(Messages("declaration.register.responsible.person.line_1"))
      html must include(Messages("declaration.register.responsible.person.line_2"))
      html must include(Messages("declaration.register.responsible.person.line_3"))
      html must include(Messages("declaration.register.responsible.person.line_4"))
      html must include(Messages("declaration.register.responsible.person.line_5"))
    }
  }
}