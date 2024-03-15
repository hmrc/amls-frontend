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

package views.registrationprogress

import generators.businesscustomer.AddressGenerator
import models.registrationprogress._
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc.Call
import play.api.test.FakeRequest
import utils.AmlsViewSpec
import views.Fixture
import views.html.registrationamendment.RegistrationAmendmentView

class RegistrationAmendmentViewSpec extends AmlsViewSpec with MockitoSugar with AddressGenerator {

  lazy val amendmentView = inject[RegistrationAmendmentView]

  implicit val request = FakeRequest()

  val taskList = TaskList(
    Seq(
      TaskRow("section1", "/foo", true, Completed, TaskRow.updatedTag),
      TaskRow("section2", "/bar", true, Started, TaskRow.incompleteTag),
    )
  )

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addTokenForView()

    val sections = Seq(
      Section("section1", Completed, true, mock[Call])
    )
  }

  "The registration progress view" must {
    "display the correct visual content for incomplete sections" when {
      "making an amendment" in new ViewFixture {
        def view =
          amendmentView(
            taskList,
            true,
            "businessName",
            Seq.empty,
            true
          )

        val expectedFullCompletedString = s"This section is ${messages("progress.visuallyhidden.view.updated")}"
        doc.getElementById("progress.section1.name-status").text must include(expectedFullCompletedString)

        val expectedFullIncompleteString = s"This section is ${messages("progress.visuallyhidden.view.started")}"
        doc.getElementById("progress.section2.name-status").text must include(expectedFullIncompleteString)
      }
    }

    "show the Nominated officer box with correct title, name and link" in new ViewFixture {

      val officerName = "FirstName LastName"

      def view = amendmentView(
        taskList,
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
      def view = amendmentView(
        taskList,
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
