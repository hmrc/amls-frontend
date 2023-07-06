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

package views.responsiblepeople

import forms.responsiblepeople.RemoveResponsiblePersonFormProvider
import models.responsiblepeople.ResponsiblePersonEndDate
import org.joda.time.LocalDate
import org.scalatest.MustMatchers
import play.api.test.FakeRequest
import utils.AmlsViewSpec
import views.Fixture
import views.html.responsiblepeople.RemoveResponsiblePersonView

class RemoveResponsiblePersonViewSpec extends AmlsViewSpec with MustMatchers {

  lazy val personView = inject[RemoveResponsiblePersonView]
  lazy val fp = inject[RemoveResponsiblePersonFormProvider]

  implicit val request = FakeRequest()

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addTokenForView()
  }

  "RemoveResponsiblePersonView" must {

    "have correct title" in new ViewFixture {

      def view = personView(fp().fill(Right(ResponsiblePersonEndDate(LocalDate.now()))), 1, "Gary", false)

      doc.title() must startWith(messages("responsiblepeople.remove.responsible.person.title") + " - " + messages("summary.responsiblepeople"))
    }

    "show none named person heading" in new ViewFixture {

      def view = personView(fp().fill(Left(false)), 1, "Gary", showDateField = false)

      heading.html() must be(messages("responsiblepeople.remove.responsible.person.title"))
    }

    "show named person heading" in new ViewFixture {

      def view = personView(fp(), 1, "Gary", showDateField = true)

      heading.html() must be(messages("responsiblepeople.remove.named.responsible.person", "Gary"))
      doc.getElementsByAttributeValue("id", "endDate") must not be empty
    }

    behave like pageWithErrors(
      personView(fp().withError("endDate", "error.future.date"), 1, "Gary", showDateField = true),
      "endDate", "error.future.date"
    )

    behave like pageWithBackLink(personView(fp(), 1, "Gary", showDateField = true))
  }
}
