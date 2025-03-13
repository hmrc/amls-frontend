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

package views.businessmatching.updateservice.remove

import forms.businessmatching.RemoveBusinessActivitiesFormProvider
import models.businessmatching.BusinessActivity.{AccountancyServices, MoneyServiceBusiness}
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.twirl.api.HtmlFormat
import utils.AmlsViewSpec
import views.Fixture
import views.html.businessmatching.updateservice.remove.RemoveActivitiesView

class RemoveActivitiesViewSpec extends AmlsViewSpec {

  lazy val formProvider: RemoveBusinessActivitiesFormProvider    = inject[RemoveBusinessActivitiesFormProvider]
  lazy val remove_activities: RemoveActivitiesView               = inject[RemoveActivitiesView]
  implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView()

  def createView: HtmlFormat.Appendable =
    remove_activities(formProvider(2), edit = true, Seq(AccountancyServices, MoneyServiceBusiness))

  trait ViewFixture extends Fixture {
    override def view: HtmlFormat.Appendable = createView
  }

  "The select activities to remove view" must {

    "have the correct title" in new ViewFixture {
      doc.title must startWith(messages("businessmatching.updateservice.removeactivities.title.multibusinesses"))
    }

    "have correct heading" in new ViewFixture {
      heading.html must be(messages("businessmatching.updateservice.removeactivities.title.multibusinesses"))
    }

    "have correct subHeading" in new ViewFixture {
      subHeading.html must include(messages("summary.updateservice"))
    }

    behave like pageWithBackLink(createView)

    "show the correct content" in new ViewFixture {

      override def view: HtmlFormat.Appendable = remove_activities(
        formProvider(2),
        edit = true,
        Seq(AccountancyServices, MoneyServiceBusiness)
      )

      doc.body().text()                   must include(messages("businessmatching.updateservice.removeactivities.title.multibusinesses"))
      doc.getElementById("button").text() must include(
        messages("businessmatching.updateservice.removeactivities.button")
      )
    }

    "show the correct checkbox values" in new ViewFixture {

      override def view: HtmlFormat.Appendable = remove_activities(
        formProvider(2),
        edit = true,
        Seq(AccountancyServices, MoneyServiceBusiness)
      )

      doc
        .select(
          "#main-content > div > div > div > form > div.govuk-form-group > fieldset > div.govuk-checkboxes > div:nth-child(1) > label"
        )
        .text() mustBe "Accountancy service provider"
      doc
        .select(
          "#main-content > div > div > div > form > div.govuk-form-group > fieldset > div.govuk-checkboxes > div:nth-child(2) > label"
        )
        .text() mustBe "Money service business"
    }

    behave like pageWithErrors(
      remove_activities(
        formProvider(2).withError("value", "error.required.bm.remove.service.multiple"),
        false,
        Seq(AccountancyServices, MoneyServiceBusiness)
      ),
      "value",
      "error.required.bm.remove.service.multiple"
    )
  }
}
