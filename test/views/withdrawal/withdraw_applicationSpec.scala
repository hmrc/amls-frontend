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

package views.withdrawal

import org.joda.time.LocalDateTime
import org.scalatestplus.mockito.MockitoSugar
import play.api.i18n.Messages
import utils.{AmlsViewSpec, DateHelper}
import views.Fixture
import views.html.withdrawal.withdraw_application

class withdraw_applicationSpec extends AmlsViewSpec with MockitoSugar {

  trait ViewFixture extends Fixture {
    lazy val withdraw_application = app.injector.instanceOf[withdraw_application]
    implicit val requestWithToken = addTokenForView()

    //noinspection ScalaStyle
    val date = new LocalDateTime(2001, 1, 1, 12, 0, 0)

    def view = withdraw_application("The Business", date)
  }

  "The withdraw application view" must {
    "show the correct titles and headings" in new ViewFixture {
      doc.title must be(s"${Messages("status.withdraw.empty.title")} - ${Messages("title.amls")} - ${Messages("title.gov")}")
      heading.html must be(Messages("Withdraw your application for The Business"))
      subHeading.html must include(Messages("summary.status"))
    }

    "show the correct informational content" in new ViewFixture {
      validateParagraphizedContent("status.withdraw.body-content")
      doc.text() must include("If you carry out activities covered by the Money Laundering Regulations, you need to be registered with an appropriate supervisory body.")
    }
  }
}
