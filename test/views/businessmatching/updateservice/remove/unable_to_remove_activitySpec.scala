/*
 * Copyright 2020 HM Revenue & Customs
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


import play.api.i18n.Messages
import utils.AmlsSpec
import views.Fixture
import views.html.businessmatching.updateservice.remove.unable_to_remove_activity

class unable_to_remove_activitySpec extends AmlsSpec {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)

    def view = unable_to_remove_activity("test")
  }

  "The unable_to_remove_activity view" must {

    "have the correct title" in new ViewFixture {
      doc.title must startWith(Messages("businessmatching.updateservice.removeactivitiesinformation.title") + " - " + Messages("summary.updateservice"))
    }

    "have correct heading" in new ViewFixture {
      heading.html must be(Messages("businessmatching.updateservice.removeactivitiesinformation.heading", "test", Messages("summary.updateinformation")))
    }

    "have the back link button" in new ViewFixture {
      doc.getElementsByAttributeValue("class", "link-back") must not be empty
    }

    "show the correct content" in new ViewFixture {
      doc.body().text() must include(Messages("businessmatching.updateservice.removeactivitiesinformation.info.3"))
      doc.body().text() must include(Messages("businessmatching.updateservice.removeactivitiesinformation.info.2"))
    }
  }

}
