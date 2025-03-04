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

import forms.businessactivities.NCARegisteredFormProvider
import org.scalatest.matchers.must.Matchers
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.twirl.api.HtmlFormat
import utils.AmlsViewSpec
import views.Fixture
import views.html.businessactivities.NCARegisteredView

class NCARegisteredViewSpec extends AmlsViewSpec with Matchers {

  lazy val registered: NCARegisteredView           = inject[NCARegisteredView]
  lazy val formProvider: NCARegisteredFormProvider = inject[NCARegisteredFormProvider]

  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  "The NCA registered view" must {

    "have the correct title" in new Fixture {

      val view: HtmlFormat.Appendable = registered(formProvider(), edit = true)

      doc.title mustBe "Reporting suspicious activity to the National Crime Agency (NCA) - Business activities" +
        " - Manage your anti-money laundering supervision - GOV.UK"
    }

    "have the correct heading" in new Fixture {

      val view: HtmlFormat.Appendable = registered(formProvider(), edit = true)

      heading.text() mustBe "Reporting suspicious activity to the National Crime Agency (NCA)"
    }

    "have the correct caption heading" in new Fixture {

      val view: HtmlFormat.Appendable = registered(formProvider(), edit = true)

      subHeading.text() mustBe "Business activities"
    }

    "have the correct legend" in new Fixture {

      val view: HtmlFormat.Appendable = registered(formProvider(), edit = true)

      doc.getElementsByTag("legend").text() mustBe "Has your business registered with the NCA?"
    }

    "have the correct form field" in new Fixture {

      val view: HtmlFormat.Appendable = registered(formProvider(), edit = true)
      doc.getElementsByAttributeValue("name", "ncaRegistered") must not be empty
    }

    behave like pageWithErrors(
      registered(formProvider().bind(Map("ncaRegistered" -> "")), edit = true),
      "ncaRegistered",
      "error.required.ba.select.nca"
    )

    behave like pageWithBackLink(registered(formProvider(), edit = false))
  }
}
