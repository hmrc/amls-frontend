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
import org.scalatest.matchers.must.Matchers
import play.api.data.FormError
import play.api.mvc.Request
import play.api.test.FakeRequest
import play.twirl.api.HtmlFormat
import utils.AmlsViewSpec
import views.Fixture
import views.html.businessactivities.InvolvedInOtherNameView

class InvolvedInOtherNameViewSpec extends AmlsViewSpec with Matchers {

  lazy val name         = inject[InvolvedInOtherNameView]
  lazy val formProvider = inject[InvolvedInOtherFormProvider]

  trait ViewFixture extends Fixture {
    implicit val requestWithToken: Request[_] = addTokenForView()
  }

  "InvolvedInOtherNameView" must {
    "have correct title" in new ViewFixture {

      def view = name(formProvider(), true, None, formProvider.length)

      doc.title must be(
        messages("businessactivities.involved.other.title") + " - " +
          messages("summary.businessactivities") +
          " - " + messages("title.amls") +
          " - " + messages("title.gov")
      )
    }

    "have correct headings" in new ViewFixture {

      def view = name(formProvider(), true, None, formProvider.length)

      heading.html    must be(messages("businessactivities.involved.other.title"))
      subHeading.html must include(messages("summary.businessactivities"))

    }

    "render the correct guidance" when {

      "one business activity has been selected" in new ViewFixture {

        val msg           = s"an ${messages("businessactivities.registerservices.servicename.lbl.01")}"
        override def view = name(formProvider(), false, Some(List(msg)), formProvider.length)

        val pTags = doc.select("p.govuk-body")

        pTags.first().text() mustBe s"${messages("businessactivities.confirm-activities.subtitle_4")} $msg"
        pTags.get(1).text() mustBe messages("businessactivities.involved.other.hint")

        val bulletList = doc.getElementsByClass("govuk-list--bullet").first().text()

        List(1, 2, 3) foreach { line =>
          bulletList must include(messages(s"businessactivities.involved.listline$line"))
        }
      }

      "multiple business activities have been selected" in new ViewFixture {

        val msg1 = s"an ${messages("businessactivities.registerservices.servicename.lbl.01")}"
        val msg2 = s"a ${messages("businessactivities.registerservices.servicename.lbl.03")}"

        override def view = name(formProvider(), false, Some(List(msg1, msg2)), formProvider.length)

        val pTags = doc.select("p.govuk-body")

        pTags.first().text() mustBe s"${messages("businessactivities.confirm-activities.subtitle_4")}:"

        val bulletLists = doc.getElementsByClass("govuk-list--bullet")
        bulletLists.first().text() must include(msg1)
        bulletLists.first().text() must include(msg2)

        pTags.get(1).text() mustBe messages("businessactivities.involved.other.hint")

        List(1, 2, 3) foreach { line =>
          bulletLists.get(1).text() must include(messages(s"businessactivities.involved.listline$line"))
        }
      }
    }

    "render the correct legend" in new ViewFixture {

      override def view: HtmlFormat.Appendable = name(formProvider(), true, None, formProvider.length)

      doc.getElementsByTag("legend").first().text() mustBe messages("businessactivities.involved.other.legend")
    }

    "have correct form fields" in new ViewFixture {

      def view = name(formProvider(), true, None, formProvider.length)

      doc.getElementsByAttributeValue("name", "involvedInOther") must not be empty
      doc.getElementsByAttributeValue("name", "details")         must not be empty

    }

    behave like pageWithErrors(
      name(
        formProvider().withError(
          FormError("details", "error.invalid.maxlength.255.renewal.ba.involved.in.other", Seq(formProvider.length))
        ),
        true,
        None,
        formProvider.length
      )(FakeRequest(), messages, appConfig),
      "details",
      "error.invalid.maxlength.255.renewal.ba.involved.in.other"
    )

    behave like pageWithBackLink(
      name(formProvider(), true, None, formProvider.length)(FakeRequest(), messages, appConfig)
    )
  }
}
