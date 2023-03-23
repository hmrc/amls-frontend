/*
 * Copyright 2023 HM Revenue & Customs
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

package views.registrationprogress

import generators.businesscustomer.AddressGenerator
import models.registrationprogress.{Completed, Section, Started}
import org.scalatestplus.mockito.MockitoSugar
import play.api.i18n.Messages
import play.api.mvc.Call
import utils.AmlsViewSpec
import views.Fixture
import views.html.registrationamendment.registration_amendment

class registration_amendmentSpec extends AmlsViewSpec with MockitoSugar with AddressGenerator {

  trait ViewFixture extends Fixture {
    lazy val registration_amendment = app.injector.instanceOf[registration_amendment]
    implicit val requestWithToken = addTokenForView()

    val sections = Seq(
      Section("section1", Completed, true, mock[Call])
    )
  }

  "The registration progress view" must {
    "display the correct visual content for incomplete sections" when {
      "making an amendment" in new ViewFixture {
        def view =
          registration_amendment(
            Seq(Section("section1", Completed, true, mock[Call]),
              Section("section2", Started, true, mock[Call])),
            true,
            "businessName",
            Seq.empty,
            true
          )


        val expectedFullCompletedString = s"This section is ${Messages("progress.visuallyhidden.view.updated")}"
        doc.getElementById("progress.section1.name-status").select(".task-status").text mustBe expectedFullCompletedString

        val expectedFullIncompleteString = s"This section is ${Messages("progress.visuallyhidden.view.started")}"
        doc.getElementById("progress.section2.name-status").select(".task-status").text mustBe expectedFullIncompleteString

      }
    }

    "show the Nominated officer box with correct title, name and link" in new ViewFixture {

      val officerName = "FirstName LastName"

      def view = registration_amendment(
        Seq(Section("section1", Completed, true, mock[Call]),
          Section("section2", Started, true, mock[Call])),
        true,
        "businessName",
        Seq.empty,
        true,
        hasCompleteNominatedOfficer = true,
        nominatedOfficerName = Some(officerName))

      val element = doc.getElementById("nominated-officer").text()

      element must include("Nominated officer")
      element must include(officerName)
    }

    "do not show the Nominated officer box if NO is not defined" in new ViewFixture {
      def view = registration_amendment(
        Seq(Section("section1", Completed, true, mock[Call]),
          Section("section2", Started, true, mock[Call])),
        true,
        "businessName",
        Seq.empty,
        true,
        hasCompleteNominatedOfficer = false,
        nominatedOfficerName = None)

      val element = Option(doc.getElementById("nominated-officer"))

      element mustBe None
    }
  }
}
