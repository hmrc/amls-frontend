/*
 * Copyright 2017 HM Revenue & Customs
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

package views

import org.joda.time.LocalDateTime
import org.scalatest.mock.MockitoSugar
import play.api.i18n.Messages
import utils.{DateHelper, GenericTestHelper}

class withdraw_applicationSpec extends GenericTestHelper with MockitoSugar {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)

    //noinspection ScalaStyle
    val date = new LocalDateTime(2001, 1, 1, 12, 0, 0)

    def view = views.html.withdraw_application("The Business", date)
  }

  "The withdraw application view" must {
    "show the correct titles and headings" in new ViewFixture {
      doc.title must be(s"${Messages("status.withdraw.title")} - ${Messages("title.amls")} - ${Messages("title.gov")}")
      heading.html must be(Messages("status.withdraw.title"))
      subHeading.html must include(Messages("summary.status"))
    }

    "show the correct informational content" in new ViewFixture {
      validateParagraphizedContent("status.withdraw.body-content")

      doc.body().html must include(Messages("status.withdraw.registration-date.label", DateHelper.formatDate(date)))
    }
  }
}
