/*
 * Copyright 2018 HM Revenue & Customs
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

import play.api.i18n.Messages
import play.twirl.api.Html
import utils.GenericTestHelper

class DuplicateSubmissionViewSpec extends GenericTestHelper {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)

    val getHelpView = Html("<p>Get help here</p>")

    def view = views.html.duplicate_submission()

  }

  "The html content" must {
    "have the correct title and subtitles" in new ViewFixture {

      doc.title mustBe s"""${Messages("error.submission.problem.title")} - ${Messages("title.amls")} - ${Messages("title.gov")}"""

    }

    "have the correct content" in new ViewFixture {
      import utils.Strings._

      doc.getElementById("page-content").html.replace("\n","") mustBe Messages("error.submission.duplicate.content").paragraphize
    }
  }

}
