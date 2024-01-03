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

package views.submission

import org.scalatest.MustMatchers
import utils.AmlsViewSpec
import views.Fixture
import views.html.submission.WrongCredentialTypeView

class WrongCredentialTypeViewSpec extends AmlsViewSpec with MustMatchers {

  trait ViewFixture extends Fixture {
    lazy val wrongCredentialTypeView = app.injector.instanceOf[WrongCredentialTypeView]
    implicit val requestWithToken = addTokenForView()

    val url = "/foo"
    override def view = wrongCredentialTypeView(url)
  }

  "WrongCredentialTypeView" must {
    "have the correct title, headings and content" in new ViewFixture {
      doc.title mustBe s"${messages("error.submission.problem.title")} - ${messages("title.amls")} - ${messages("title.gov")}"
      doc.select("h1").text mustBe messages("error.submission.problem.title")

      val link = doc.getElementById("report-link")

      link.text() mustBe messages("error.submission.duplicate_submission.link.text")
      link.attr("href") mustBe url

      val paragraphText = doc.getElementsByClass("govuk-body").text()

      List("1", "2", "3") foreach { l =>
        paragraphText must include(messages(s"error.submission.wrong_credentials.content.line$l"))
      }
    }
  }

}
