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

import forms.EmptyForm
import models.confirmation.{BreakdownRow, Currency}
import models.registrationprogress.{Completed, Section}
import org.scalatest.mock.MockitoSugar
import play.api.i18n.Messages
import play.api.mvc.Call
import utils.AmlsSpec

class fee_guidanceSpec extends AmlsSpec with MockitoSugar {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)

    val totalAmount = Currency(600)

    val breakdownRows = Seq(
      BreakdownRow("Item 1", 5, Currency(100), Currency(200)),
      BreakdownRow("Item 2", 6, Currency(300), Currency(400))
    )

    def view = views.html.fee_guidance(totalAmount, breakdownRows)
  }

  "The registration progress view" must {
    "have correct title, headings and content" in new ViewFixture {
      val form2 = EmptyForm

      doc.title must be(Messages("fee.guidance") + " - " +
        Messages("submit.registration") + " - " +
        Messages("title.amls") + " - " + Messages("title.gov"))

      heading.html must be(Messages("fee.guidance"))
      subHeading.html must include(Messages("submit.registration"))
      doc.text() must include(Messages("fee.guidance.intro"))
      doc.getElementsByClass("panel-indent").text must include(Messages("fee.guidance.notice"))

    }

    "include a link to edit the application" in new ViewFixture {
      val editLink = doc.getElementById("edit-application")
      editLink.text() mustBe Messages("fee.guidance.edit-application.text")
      editLink.attr("href") mustBe controllers.routes.RegistrationProgressController.get().url
    }

    "include a breakdown of the fees" in new ViewFixture {
      for((row, index) <- breakdownRows.zipWithIndex) {
        val tableRow = doc.select("#fee-breakdown tbody tr").get(index)
        val stringsToTest = Seq(row.label, row.perItm, row.total, row.quantity) map {_.toString}

        stringsToTest foreach { s => tableRow.text must include(s) }
      }

      doc.select("#fee-breakdown tfoot").text must include(totalAmount.toString)
    }

  }

}
