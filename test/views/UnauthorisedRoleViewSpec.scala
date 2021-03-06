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

package views

import org.scalatest.MustMatchers
import utils.AmlsViewSpec
import views.html.unauthorised_role

class UnauthorisedRoleViewSpec extends AmlsViewSpec with MustMatchers {

  trait ViewFixture extends Fixture {
    lazy val unauthorised_role = app.injector.instanceOf[unauthorised_role]
    implicit val requestWithToken = addTokenForView()

    def view = unauthorised_role()
  }

  "The 'unauthorised role' view" must {
    "display the correct headings and titles" in new ViewFixture {
      validateTitle("unauthorised.title")
      doc.select("h1.heading-xlarge").text mustBe messages("unauthorised.title")
    }

    "display the correct body content" in new ViewFixture {
      validateParagraphizedContent("unauthorised.role.content")
    }
  }

}
