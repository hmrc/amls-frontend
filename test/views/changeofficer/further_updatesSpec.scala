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

package views.changeofficer

import forms.{EmptyForm, InvalidForm}
import jto.validation.{Path, ValidationError}
import org.scalatest.MustMatchers
import play.api.i18n.Messages
import utils.GenericTestHelper
import views.Fixture

class further_updatesSpec extends GenericTestHelper with MustMatchers{

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)
  }

  "further_updates view" must {
    "have correct title" in new ViewFixture {

      def view = views.html.changeofficer.further_updates(EmptyForm)

      doc.title must startWith(Messages("changeofficer.furtherupdates.title") + " - " + Messages("summary.updateinformation"))
    }

    "have correct headings" in new ViewFixture {

      def view = views.html.changeofficer.further_updates(EmptyForm)

      heading.html must be(Messages("changeofficer.furtherupdates.heading", "testName"))
      subHeading.html must include(Messages("summary.updateinformation"))

    }

    "show errors in the correct locations" in new ViewFixture {

      val messageKey1 = "definitely not a message key"
      val fieldName = "furtherUpdates"

      val form2: InvalidForm = InvalidForm(
        Map("thing" -> Seq("thing")),
        Seq((Path \ fieldName, Seq(ValidationError(messageKey1))))
      )

      def view = views.html.changeofficer.further_updates(form2)

      errorSummary.html() must include(messageKey1)

      doc.getElementById(fieldName).html() must include(messageKey1)

      doc.getElementsByAttributeValue("name", "furtherupdates") must not be empty
    }

  }

}
