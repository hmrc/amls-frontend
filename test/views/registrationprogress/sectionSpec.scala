/*
 * Copyright 2019 HM Revenue & Customs
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

import models.registrationprogress.{Completed, NotStarted, Started}
import org.scalatest.mock.MockitoSugar
import play.api.mvc.Call
import play.twirl.api.HtmlFormat
import utils.AmlsSpec

class SectionSpec extends AmlsSpec with MockitoSugar {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)

  }

  "The section view" when {

    "status is NotStarted" must {
      "show Add [SectionName] link text" in new ViewFixture {
        override def view: HtmlFormat.Appendable = views.html.registrationprogress.section("hvd", NotStarted, mock[Call])

        doc.select("#hvd-status").first().ownText() must be("Add High Value Dealer")
      }

      "show Not started info text" in new ViewFixture {
        override def view: HtmlFormat.Appendable = views.html.registrationprogress.section("hvd", NotStarted, mock[Call])

        doc.select("div").first().ownText() must be("Not started")
      }
    }

    "status is Started" must {
      "show Add [SectionName] link text" in new ViewFixture {
        override def view: HtmlFormat.Appendable = views.html.registrationprogress.section("hvd", Started, mock[Call])

        doc.select("#hvd-status").first().ownText() must be("Add High Value Dealer")
      }

      "show Incomplete info text" in new ViewFixture {
        override def view: HtmlFormat.Appendable = views.html.registrationprogress.section("hvd", Started, mock[Call])

        doc.select("div").first().ownText() must be ("Incomplete")
      }
    }

    "status is Complete" must {
      "show Edit [SectionName] link text" in new ViewFixture {
        override def view: HtmlFormat.Appendable = views.html.registrationprogress.section("hvd", Completed, mock[Call])

        doc.select("#hvd-status").first().ownText() must be("Edit High Value Dealer")
      }

      "show Complete info text" in new ViewFixture {
        override def view: HtmlFormat.Appendable = views.html.registrationprogress.section("hvd", Completed, mock[Call])

        doc.select("div").first().ownText() must be ("Completed")
      }
    }
  }
}
