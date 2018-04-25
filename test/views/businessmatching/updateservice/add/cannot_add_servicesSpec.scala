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

package views.businessmatching.updateservice.add

import org.scalatest.MustMatchers
import play.api.i18n.Messages
import utils.GenericTestHelper
import views.Fixture
import views.html.businessmatching.updateservice.add.cannot_add_services

class cannot_add_servicesSpec extends GenericTestHelper with MustMatchers {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)

    def view = cannot_add_services()
  }

  "The cannot_add_services view" must {

    "have the correct title" in new ViewFixture {
      doc.title must startWith(Messages("businessmatching.updateservice.nopsr.cannotcontinuewiththeapplication.title") + " - " + Messages("summary.businessmatching"))
    }

    "have correct heading" in new ViewFixture {
      heading.html must be(Messages("businessmatching.updateservice.nopsr.cannotcontinuewiththeapplication.heading"))
    }

    "have correct subHeading" in new ViewFixture {
      subHeading.html must include(Messages("summary.businessmatching"))
    }

    "show the correct content" in new ViewFixture {
      doc.body().text() must include(Messages("businessmatching.updateservice.nopsr.cannotcontinuewiththeapplication.requiredinfo"))
    }
  }

}
