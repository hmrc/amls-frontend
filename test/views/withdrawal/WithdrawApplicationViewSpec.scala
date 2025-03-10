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

package views.withdrawal

import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc.{AnyContentAsEmpty, Request}
import utils.AmlsViewSpec
import views.Fixture
import views.html.withdrawal.WithdrawApplicationView

import java.time.LocalDateTime

class WithdrawApplicationViewSpec extends AmlsViewSpec with MockitoSugar {

  trait ViewFixture extends Fixture {
    lazy val withdraw_application                                  = inject[WithdrawApplicationView]
    implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView()

    // noinspection ScalaStyle
    val date = LocalDateTime.of(2001, 1, 1, 12, 0, 0)

    def view = withdraw_application("The Business", date)
  }

  "The withdraw application view" must {
    "show the correct titles and headings" in new ViewFixture {
      doc.title       must be(
        s"${messages("status.withdraw.empty.title")} - ${messages("title.amls")} - ${messages("title.gov")}"
      )
      heading.html    must be(messages("Withdraw your application for The Business"))
      subHeading.html must include(messages("summary.status"))
    }

    "show the correct informational content" in new ViewFixture {
      doc.text() must include(messages("status.withdraw.body-content"))
      doc.text() must include(
        "If you carry out activities covered by the Money Laundering Regulations, you need to be registered with an appropriate supervisory body."
      )
    }
  }
}
