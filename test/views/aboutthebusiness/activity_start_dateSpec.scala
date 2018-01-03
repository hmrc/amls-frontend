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

package views.aboutthebusiness

import forms.{Form2, InvalidForm, ValidForm}
import jto.validation.{Path, ValidationError}
import models.aboutthebusiness.ActivityStartDate
import org.joda.time.LocalDate
import org.scalatest.MustMatchers
import play.api.i18n.Messages
import utils.GenericTestHelper
import views.Fixture


class activity_start_dateSpec extends GenericTestHelper with MustMatchers {
  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)
  }
  "activity_start_date view" must {
    "have correct title" in new ViewFixture {

      val form2: ValidForm[ActivityStartDate] = Form2(ActivityStartDate(LocalDate.now))

      def view = views.html.aboutthebusiness.activity_start_date(form2, true)

      doc.title must startWith(Messages("aboutthebusiness.activity.start.date.title") + " - " + Messages("summary.aboutbusiness"))
    }

    "have correct headings" in new ViewFixture {

      val form2: ValidForm[ActivityStartDate] = Form2(ActivityStartDate(LocalDate.now))

      def view = views.html.aboutthebusiness.activity_start_date(form2, true)

      heading.html must be(Messages("aboutthebusiness.activity.start.date.title"))
      subHeading.html must include(Messages("summary.aboutbusiness"))

    }

    "show errors in the correct locations" in new ViewFixture {

      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq(
          (Path \ "startDate") -> Seq(ValidationError("not a message Key"))
        ))

      def view = views.html.aboutthebusiness.activity_start_date(form2, true)

      errorSummary.html() must include("not a message Key")

      doc.getElementById("startDate")
        .getElementsByClass("error-notification").first().html() must include("not a message Key")

    }
  }
}