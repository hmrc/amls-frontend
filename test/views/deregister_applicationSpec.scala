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

import forms.EmptyForm
import org.joda.time.LocalDateTime
import org.scalatest.MustMatchers
import play.api.i18n.Messages
import utils.GenericTestHelper

class deregister_applicationSpec extends GenericTestHelper with MustMatchers {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)

    val businessName = "Test Business"
    val processingDate = LocalDateTime.now()
    val regNumber = "IUYSF894739847"

    def view = views.html.deregister_application(businessName, processingDate, regNumber)
  }

  "deregister_application view" must {
    "have correct title and headings" in new ViewFixture {

      val title = s"${Messages("status.deregister.title")} - ${Messages("title.amls")} - ${Messages("title.gov")}"

      doc.title mustBe title
      heading.html must be(Messages("status.deregister.title"))
      subHeading.html must include(Messages("summary.status"))
      //code to check existance of form fields			
    }

    "have correct body content" in new ViewFixture {
      validateParagraphizedContent("status.deregister.body-content")
    }
  }
}
