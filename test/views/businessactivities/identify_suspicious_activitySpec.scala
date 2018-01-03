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

package views.businessactivities

import forms.{InvalidForm, EmptyForm}
import jto.validation.{Path, ValidationError}
import org.scalatest.{MustMatchers}
import utils.GenericTestHelper
import play.api.i18n.Messages
import views.Fixture


class identify_suspicious_activitySpec extends GenericTestHelper with MustMatchers {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)
  }

  "Spec view" must {
    "have correct title" in new ViewFixture {

      val form2 = EmptyForm

      def view = views.html.businessactivities.identify_suspicious_activity(form2, true)

      doc.title must be(Messages("businessactivities.identify-suspicious-activity.title") + " - " +
        Messages("summary.businessactivities") +
        " - " + Messages("title.amls") +
        " - " + Messages("title.gov"))
    }

    "have correct headings" in new ViewFixture {

      val form2 = EmptyForm

      def view = views.html.businessactivities.identify_suspicious_activity(form2, true)

      heading.html must be(Messages("businessactivities.identify-suspicious-activity.title"))
      subHeading.html must include(Messages("summary.businessactivities"))

    }

    "have correct form fields" in new ViewFixture {

      val form2 = EmptyForm

      def view = views.html.businessactivities.identify_suspicious_activity(form2, true)

      doc.getElementsByAttributeValue("name", "hasWrittenGuidance") must not be empty

    }

    "show errors in the correct locations" in new ViewFixture {

      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq(
          (Path \ "hasWrittenGuidance") -> Seq(ValidationError("not a message Key"))
        ))

      def view = views.html.businessactivities.identify_suspicious_activity(form2, true)

      errorSummary.html() must include("not a message Key")

    }
  }
}