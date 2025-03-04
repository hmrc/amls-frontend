/*
 * Copyright 2024 HM Revenue & Customs
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

import org.scalatest.matchers.must.Matchers
import play.api.mvc.{AnyContentAsEmpty, Call, Request}
import utils.AmlsViewSpec
import views.html.LoginEventView

class LoginEventViewSpec extends AmlsViewSpec with Matchers {

  trait ViewFixture extends Fixture {
    lazy val login_event                                           = inject[LoginEventView]
    implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView()
    implicit val redirectCall: Call                                = Call("GET", "someurl")
  }

  "Login Event Page View" must {
    "Have the correct title" in new ViewFixture {
      def view = login_event(redirectCall)

      doc.title must startWith(messages("login-event.title"))
    }

    "have correct headings" in new ViewFixture {
      def view = login_event(redirectCall)

      heading.html must be(messages("login-event.heading"))
    }

    "contain the expected link elements" in new ViewFixture {
      def view = login_event(redirectCall)

      html must include(messages("login-event.event-messages-header"))
      html must include(messages("login-event.property-redress"))
      html must include(messages("login-event.property-ombudsman"))
      html must include(messages("login-event.update-now"))
      html must include(messages("login-event.skip-for-now"))
    }
  }
}
