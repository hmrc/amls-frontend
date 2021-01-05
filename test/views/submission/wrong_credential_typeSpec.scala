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

package views.submission

import org.scalatest.MustMatchers
import play.api.i18n.Messages
import utils.AmlsViewSpec
import views.Fixture
import views.html.submission.wrong_credential_type

class wrong_credential_typeSpec extends AmlsViewSpec with MustMatchers {

  trait ViewFixture extends Fixture {
    lazy val wrong_credential_type = app.injector.instanceOf[wrong_credential_type]
    implicit val requestWithToken = addTokenForView()

    override def view = wrong_credential_type()
  }

  "The 'wrong credential type' template" must {
    "have the correct title, headings and content" in new ViewFixture {
      doc.title mustBe s"${Messages("error.submission.problem.title")} - ${Messages("title.amls")} - ${Messages("title.gov")}"
      doc.select("h1").text mustBe Messages("error.submission.problem.title")

      validateParagraphizedContent("error.submission.wrong_credentials.content")
    }
  }

}
