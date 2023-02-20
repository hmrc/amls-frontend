/*
 * Copyright 2023 HM Revenue & Customs
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

import org.scalatest.MustMatchers
import utils.AmlsViewSpec
import play.api.i18n.Messages
import play.api.mvc.Call
import views.html.login_event

class LoginEventSpec extends AmlsViewSpec with MustMatchers {

  trait ViewFixture extends Fixture {
    lazy val login_event = app.injector.instanceOf[login_event]
    implicit val requestWithToken = addTokenForView()
    implicit val redirectCall = Call("GET", "someurl")
  }

  "Login Event Page View" must {
    "Have the correct title" in new ViewFixture {
      def view = login_event(redirectCall)

      doc.title must startWith(Messages("login-event.title"))
    }

    "have correct headings" in new ViewFixture {
      def view = login_event(redirectCall)

      heading.html must be(Messages("login-event.heading"))
    }

    "contain the expected link elements" in new ViewFixture {
      def view = login_event(redirectCall)

      html must include(Messages("login-event.event-messages-header"))
      html must include(Messages("login-event.property-redress"))
      html must include(Messages("login-event.property-ombudsman"))
      html must include(Messages("login-event.update-now"))
      html must include(Messages("login-event.skip-for-now"))
    }
  }
}