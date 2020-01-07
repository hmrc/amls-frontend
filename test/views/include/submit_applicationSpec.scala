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

package views.include

import org.scalatest.MustMatchers
import play.api.i18n.Messages
import utils.AmlsSpec
import views.Fixture

class submit_applicationSpec extends AmlsSpec with MustMatchers{

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)
  }

  "submit application html" must {
    "show update is made  message" when {
      "a completed update is made" in new ViewFixture {
        override def view = views.html.include.submit_application(true, true)

        doc.html must include(Messages("progress.updates.made"))
      }
    }

    "show incomplete update is made message" when {
      "an incomplete update is made" in new ViewFixture {
        override def view = views.html.include.submit_application(false, true)

        doc.html must include(Messages("progress.updates.incomplete"))
      }
    }

    "show no update is made message" when {
      "an sections complete flag is not set" in new ViewFixture {
        override def view = views.html.include.submit_application(false, false)

        doc.html must include(Messages("progress.updates.not.made"))
      }
    }
  }
}