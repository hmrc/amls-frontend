/*
 * Copyright 2020 HM Revenue & Customs
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

import forms.EmptyForm
import generators.businesscustomer.AddressGenerator
import models.registrationprogress.{Completed, Section}
import org.scalatest.mockito.MockitoSugar
import play.api.i18n.Messages
import play.api.mvc.Call
import utils.AmlsViewSpec
import views.Fixture

class registration_progressSpec extends AmlsViewSpec with MockitoSugar with AddressGenerator {

  val businessName = "BusinessName"
  val serviceNames = Seq("Service 1", "Service 2", "Service 3")

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addTokenForView()

    val sections = Seq(
        Section("section1", Completed, true, mock[Call])
    )
  }

  "The registration progress view" must {
    "have correct title, headings and form fields" in new ViewFixture {
      val form2 = EmptyForm

      def view = views.html.registrationprogress.registration_progress(sections, true, "biz name", Seq.empty[String], true)

      doc.title must be(Messages("progress.title") + " - " +
        Messages("title.amls") + " - " + Messages("title.gov"))
      heading.html must be(Messages("progress.title"))

      doc.select("h2.heading-small").first().ownText() must be("Your business")
    }

    "show the business name and services" in new ViewFixture {
      def view = views.html.registrationprogress.registration_progress(sections, true, businessName, serviceNames, true)
      val element = doc.getElementsByClass("grid-layout")
      serviceNames.foreach(name => element.text() must include { name } )
    }

    "show the view details link under services section" in new ViewFixture {
      def view = views.html.registrationprogress.registration_progress(sections, true, businessName, serviceNames, true)
      val element = Option(doc.getElementById("view-details"))
      element mustNot be(None)
    }
  }
}
