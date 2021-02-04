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

package views.businessmatching.updateservice.remove

import models.businessmatching.{AccountancyServices, MoneyServiceBusiness}
import org.scalatest.MustMatchers
import play.api.i18n.Messages
import utils.AmlsViewSpec
import views.Fixture
import views.html.businessmatching.updateservice.remove.need_more_information


class need_more_informationSpec extends AmlsViewSpec with MustMatchers {

  trait ViewFixture extends Fixture {
    lazy val need_more_information = app.injector.instanceOf[need_more_information]
    implicit val requestWithToken = addTokenForView()
    def view = need_more_information(Set(AccountancyServices.getMessage()))
  }

  "The need_more_information view" must {

    "have the correct title" in new ViewFixture {
      doc.title must startWith(Messages("businessmatching.updateservice.updateotherinformation.title") + " - " + Messages("summary.updateservice"))
    }

    "have correct heading" in new ViewFixture {
      heading.html must be(Messages("businessmatching.updateservice.updateotherinformation.heading"))
    }

    "have correct subHeading" in new ViewFixture {
      subHeading.html must include(Messages("summary.updateservice"))
    }

    "have the back link button" in new ViewFixture {
      doc.getElementsByAttributeValue("class", "link-back") must not be empty
    }

    "show the correct content when asp is selected" in new ViewFixture {
      doc.body().text() must include(Messages("businessmatching.updateservice.updateotherinformation.information.2"))
      doc.body().text() must include(Messages("businessmatching.updateservice.updateotherinformation.information.3"))
    }

    "show the correct content when asp and msb is selected" in new ViewFixture {
      override def view = need_more_information(Set(AccountancyServices.getMessage(), MoneyServiceBusiness.getMessage()))

      doc.body().text() must include(Messages("businessmatching.updateservice.updateotherinformation.information.0"))
      doc.body().text() must include(Messages("businessmatching.updateservice.updateotherinformation.information.2"))
      doc.body().text() must include(Messages("businessmatching.updateservice.updateotherinformation.information.3.multiple.services"))
    }

    "have the correct button" in new ViewFixture {
      doc.getElementById("removeserviceinfo-submit").text() mustBe Messages("Continue")
    }
  }

}
