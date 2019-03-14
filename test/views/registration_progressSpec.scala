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
import models.registrationprogress.{Completed, Section, Started}
import org.scalatest.mock.MockitoSugar
import play.api.i18n.Messages
import play.api.mvc.Call
import utils.AmlsSpec
import generators.businesscustomer.AddressGenerator

class registration_progressSpec extends AmlsSpec with MockitoSugar with AddressGenerator {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)

    val sections = Seq(
        Section("section1", Completed, true, mock[Call])
    )
  }

  "The registration progress view" must {
    "have correct title, headings and form fields" in new ViewFixture {
      val form2 = EmptyForm

      def view = views.html.registrationprogress.registration_progress(sections, true, addressGen.sample.get, Seq.empty[String], true)

      doc.title must be(Messages("progress.title") + " - " +
        Messages("title.yapp") + " - " +
        Messages("title.amls") + " - " + Messages("title.gov"))
      heading.html must be(Messages("progress.title"))

      // the first h2.heading-small should be info about progress section
      doc.select("h2.heading-small").first().ownText() must be("Your business")
    }

    "display the correct visual content for incomplete sections" when {
      "making an amendment" in new ViewFixture {
        def view =
          views.html.registrationamendment.registration_amendment(
            Seq(Section("section1", Completed, true, mock[Call]),
              Section("section2", Started, true, mock[Call])),
            true,
            addressGen.sample.get,
            Seq.empty,
            true
          )

        doc.select("#section1-status").text mustBe Messages("progress.visuallyhidden.view.amend")
        doc.select("#section2-status").text mustBe Messages("progress.visuallyhidden.view.started")

      }
    }

  }

}
