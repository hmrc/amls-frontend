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

package views.renewal

import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import jto.validation.{Path, ValidationError}
import models.renewal.{InvolvedInOther, InvolvedInOtherNo}
import org.scalatest.MustMatchers
import play.api.i18n.Messages
import utils.AmlsViewSpec
import views.Fixture
import views.html.renewal.involved_in_other


class involved_in_otherSpec extends AmlsViewSpec with MustMatchers {

  trait ViewFixture extends Fixture {
    lazy val involved_in_other = app.injector.instanceOf[involved_in_other]
    implicit val requestWithToken = addTokenForView()
  }

  "involved_in_other view" must {
    "have correct title" in new ViewFixture {

      val form2: ValidForm[InvolvedInOther] = Form2(InvolvedInOtherNo)

      def view = involved_in_other(form2, true, None)

      doc.title must startWith(Messages("renewal.involvedinother.title"))
    }

    "have correct headings" in new ViewFixture {

      val form2: ValidForm[InvolvedInOther] = Form2(InvolvedInOtherNo)

      def view = involved_in_other(form2, true, None)

      heading.html must be(Messages("renewal.involvedinother.title"))
      subHeading.html must include(Messages("summary.renewal"))

    }

    "correctly list business activities" in new ViewFixture {
      val form2: ValidForm[InvolvedInOther] = Form2(InvolvedInOtherNo)

      def view = involved_in_other(form2, true, Some(List("test activities string")))

      html must include(Messages("businessactivities.confirm-activities.subtitle_4"))
      html must include("test activities string")
    }

    "show errors in the correct locations" in new ViewFixture {

      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq(
          (Path \ "involvedInOther") -> Seq(ValidationError("not a message Key")),
          (Path \ "details") -> Seq(ValidationError("second not a message Key"))
        ))

      def view = involved_in_other(form2, true, None)

      errorSummary.html() must include("not a message Key")
      errorSummary.html() must include("second not a message Key")

      doc.getElementById("involvedInOther").parent()
        .getElementsByClass("error-notification").first().html() must include("not a message Key")

      doc.getElementById("details").parent()
        .getElementsByClass("error-notification").first().html() must include("second not a message Key")

    }

    "have a back link" in new ViewFixture {

      def view = involved_in_other(EmptyForm, true, None)

      doc.getElementsByAttributeValue("class", "link-back") must not be empty
    }
  }
}