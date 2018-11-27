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
import  utils.AmlsSpec
import play.api.i18n.Messages

class LoginEventSpec extends AmlsSpec with MustMatchers {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)
  }

  "Login Event Page View" must {
    "Have the correct title" in new ViewFixture {
      def view = views.html.login_event()

      doc.title must startWith(Messages("login-event.title"))
    }

    "have correct headings" in new ViewFixture {
      def view = views.html.login_event()

      heading.html must be(Messages("login-event.heading"))
    }

    "contain the expected link elements" in new ViewFixture {
      def view = views.html.login_event()

      html must include(Messages("login-event.date-of-birth-message"))
      html must include(Messages("login-event.fit-and-proper-message"))
      html must include(Messages("login-event.approval-check-charge-message"))
      html must include(Messages("login-event.update-responsible-people"))
      html must include(Messages("login-event.skip-for-now"))
    }
  }
}