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
import models.registrationprogress.{Completed, Section, TaskList, TaskRow}
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc.{AnyContentAsEmpty, Call, Request}
import utils.AmlsViewSpec
import views.Fixture
import views.html.registrationprogress._

class RegistrationProgressViewSpec extends AmlsViewSpec with MockitoSugar with AddressGenerator {

  lazy val registration_progress = app.injector.instanceOf[RegistrationProgressView]
  val businessName               = "BusinessName"
  val serviceNames               = Seq("Service 1", "Service 2", "Service 3")

  trait ViewFixture extends Fixture {
    implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView()

    val sections = Seq(
      Section("section1", Completed, true, mock[Call])
    )

    val taskList = TaskList(
      Seq(
        TaskRow(
          "section1",
          "/foo",
          true,
          Completed,
          TaskRow.completedTag
        )
      )
    )
  }

  "The registration progress view" must {
    "have correct title, headings and form fields" in new ViewFixture {

      def view = registration_progress(taskList, true, "biz name", Seq.empty[String], true)

      doc.title    must be(
        messages("progress.title") + " - " +
          messages("title.amls") + " - " + messages("title.gov")
      )
      heading.html must be(messages("progress.title"))

      doc.getElementsByClass("govuk-summary-list__key").get(0).text() must include("Your business")
    }

    "show the business name and services" in new ViewFixture {
      def view = registration_progress(taskList, true, businessName, serviceNames, true)

      val elementText = doc.getElementsByClass("govuk-summary-list__row").get(1).text()
      serviceNames.foreach(elementText must include(_))
    }

    "show the view details link under services section" in new ViewFixture {
      def view = registration_progress(taskList, true, businessName, serviceNames, true)

      val element = Option(doc.getElementById("view-details"))
      element mustNot be(None)
    }

    "show the Nominated officer box with correct title, name and link" in new ViewFixture {

      val officerName = "FirstName LastName"

      def view = registration_progress(
        taskList,
        declarationAvailable = true,
        businessName = businessName,
        serviceNames = serviceNames,
        canEditPreApplication = true,
        hasCompleteNominatedOfficer = true,
        nominatedOfficerName = Some(officerName)
      )

      val element = doc.getElementsByClass("govuk-summary-list__row").get(1).text()

      element must include("Nominated officer")
      element must include(officerName)
    }

    "do not show the Nominated officer box if NO is not defined" in new ViewFixture {
      def view = registration_progress(
        taskList,
        declarationAvailable = true,
        businessName = businessName,
        serviceNames = serviceNames,
        canEditPreApplication = true,
        hasCompleteNominatedOfficer = false,
        nominatedOfficerName = None
      )

      val element = Option(doc.getElementById("nominated-officer"))

      element mustBe None
    }
  }
}
