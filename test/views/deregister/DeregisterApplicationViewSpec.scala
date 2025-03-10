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

package views.deregister

import generators.AmlsReferenceNumberGenerator
import org.scalatest.matchers.must.Matchers
import play.api.mvc.{AnyContentAsEmpty, Request}
import utils.AmlsViewSpec
import views.Fixture
import views.html.deregister.DeregisterApplicationView

class DeregisterApplicationViewSpec extends AmlsViewSpec with Matchers with AmlsReferenceNumberGenerator {

  trait ViewFixture extends Fixture {
    lazy val deregisterView                                        = inject[DeregisterApplicationView]
    implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView()

    val businessName = "Test Business"

    def view = deregisterView(businessName)
  }

  "DeregisterApplicationView" must {
    "have correct title and headings" in new ViewFixture {

      val title = s"${messages("status.deregister.empty.title")} - ${messages("title.amls")} - ${messages("title.gov")}"

      doc.title mustBe title
      heading.html    must be(messages("status.deregister.title", businessName))
      subHeading.html must include(messages("summary.status"))
    }

    "have correct body content" in new ViewFixture {
      doc.text() must include(messages("status.deregister.body-content"))
    }
  }
}
