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

package views.businessactivities

import forms.businessactivities.InvolvedInOtherFormProvider
import org.scalatest.MustMatchers
import play.api.data.FormError
import play.api.test.FakeRequest
import utils.AmlsViewSpec
import views.Fixture
import views.html.businessactivities.InvolvedInOtherNameView


class InvolvedInOtherNameViewSpec extends AmlsViewSpec with MustMatchers {


  lazy val name = inject[InvolvedInOtherNameView]
  lazy val formProvider = inject[InvolvedInOtherFormProvider]

  implicit val request = FakeRequest()

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addTokenForView()
  }

  "involved_in_other_name view" must {
    "have correct title" in new ViewFixture {

      def view = name(formProvider(), true, None, formProvider.length)

      doc.title must be(messages("businessactivities.involved.other.title") + " - " +
        messages("summary.businessactivities") +
      " - " + messages("title.amls") +
        " - " + messages("title.gov"))
    }

    "have correct headings" in new ViewFixture {

      def view = name(formProvider(), true, None, formProvider.length)

      heading.html must be(messages("businessactivities.involved.other.title"))
      subHeading.html must include(messages("summary.businessactivities"))

    }

    "have correct form fields" in new ViewFixture {

      def view = name(formProvider(), true, None, formProvider.length)

      doc.getElementsByAttributeValue("name", "involvedInOther") must not be empty
      doc.getElementsByAttributeValue("name", "details") must not be empty

    }

    behave like pageWithErrors(
      name(
        formProvider().withError(
          FormError("details", "error.invalid.maxlength.255.renewal.ba.involved.in.other", Seq(formProvider.length))
        ),
        true, None, formProvider.length
      ),
      "details",
      "error.invalid.maxlength.255.renewal.ba.involved.in.other"
    )

    behave like pageWithBackLink(name(formProvider(), true, None, formProvider.length))
  }
}
