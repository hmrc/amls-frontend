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

import forms.EmptyForm
import models.registrationprogress.NotStarted
import org.scalatest.mock.MockitoSugar
import play.api.mvc.Call
import utils.AmlsSpec

class SectionSpec extends AmlsSpec with MockitoSugar {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)

  }

  "The section view" must {
    "have correct text displayed for NotStarted status" in new ViewFixture {

      def view = views.html.registrationprogress.section("hvd", NotStarted, mock[Call])

      doc.select("#hvd-status").first().ownText() must be ("Add High Value Dealer")
    }
  }
}
