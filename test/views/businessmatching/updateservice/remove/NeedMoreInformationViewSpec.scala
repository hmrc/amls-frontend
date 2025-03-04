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

import models.businessmatching.BusinessActivity.{AccountancyServices, MoneyServiceBusiness}
import org.scalatest.matchers.must.Matchers
import play.api.mvc.{AnyContentAsEmpty, Request}
import utils.AmlsViewSpec
import views.Fixture
import views.html.businessmatching.updateservice.remove.NeedMoreInformationView

class NeedMoreInformationViewSpec extends AmlsViewSpec with Matchers {

  lazy val informationView                                       = app.injector.instanceOf[NeedMoreInformationView]
  implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView()

  trait ViewFixture extends Fixture {

    def view = informationView(Set(AccountancyServices))
  }

  "The NeedMoreInformationView view" must {

    "have the correct title" in new ViewFixture {
      doc.title must startWith(
        messages("businessmatching.updateservice.updateotherinformation.title") + " - " + messages(
          "summary.updateservice"
        )
      )
    }

    "have correct heading" in new ViewFixture {
      heading.html must be(messages("businessmatching.updateservice.updateotherinformation.heading"))
    }

    "have correct subHeading" in new ViewFixture {
      subHeading.html must include(messages("summary.updateservice"))
    }

    "show the correct content when asp is selected" in new ViewFixture {
      doc.body().text() must include(messages("businessmatching.updateservice.updateotherinformation.information.2"))
      doc.body().text() must include(messages("businessmatching.updateservice.updateotherinformation.information.3"))
    }

    "show the correct content when asp and msb is selected" in new ViewFixture {
      override def view = informationView(Set(AccountancyServices, MoneyServiceBusiness))

      doc.body().text() must include(messages("businessmatching.updateservice.updateotherinformation.information.0"))
      doc.body().text() must include(messages("businessmatching.updateservice.updateotherinformation.information.2"))
      doc.body().text() must include(
        messages("businessmatching.updateservice.updateotherinformation.information.3.multiple.services")
      )
    }

    "have the correct button" in new ViewFixture {
      doc.getElementById("removeserviceinfo-submit").text() mustBe messages("Continue")
    }

    behave like pageWithBackLink(informationView(Set(AccountancyServices)))

  }
}
