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

package views.businessmatching.updateservice

import forms.businessmatching.updateservice.ChangeBusinessTypesFormProvider
import org.jsoup.nodes.Element
import org.scalatest.matchers.must.Matchers
import play.api.mvc.{AnyContentAsEmpty, Request}
import utils.AmlsViewSpec
import views.Fixture
import views.html.businessmatching.updateservice.ChangeServicesView

class ChangeServicesViewSpec extends AmlsViewSpec with Matchers {

  val allowAdd = true

  lazy val change_services                                       = inject[ChangeServicesView]
  lazy val fp                                                    = inject[ChangeBusinessTypesFormProvider]
  implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView()

  trait ViewFixture extends Fixture {
    def view = change_services(fp(), allowAdd)
  }

  "ChangeServicesView" must {

    "have the correct title" in new ViewFixture {
      doc.title must startWith(
        messages("businessmatching.updateservice.changeservices.title") + " - " + messages("summary.updateservice")
      )
    }

    "have correct heading" in new ViewFixture {
      heading.html must be(messages("businessmatching.updateservice.changeservices.heading"))
    }

    "have correct subHeading" in new ViewFixture {
      subHeading.html must include(messages("summary.updateservice"))
    }

    "show the correct content" in new ViewFixture {
      doc.body().text() must include(messages("businessmatching.updateservice.changeservices.choice.add"))
      doc.getElementById("add").isInstanceOf[Element] mustBe true
      doc.getElementById("remove").isInstanceOf[Element] mustBe true
      doc.body().text() must include(messages("link.return.registration.progress"))
      doc.getElementById("button-continue").isInstanceOf[Element] mustBe true
    }

    behave like pageWithErrors(
      change_services(
        fp().withError("changeServices", "error.businessmatching.updateservice.changeservices"),
        allowAdd
      ),
      "changeServices",
      "error.businessmatching.updateservice.changeservices"
    )

    behave like pageWithBackLink(change_services(fp(), allowAdd))
  }
}
