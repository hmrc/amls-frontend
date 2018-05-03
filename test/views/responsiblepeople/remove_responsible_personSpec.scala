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

package views.responsiblepeople

import forms.{Form2, InvalidForm, ValidForm}
import models.responsiblepeople.{ExperienceTraining, ExperienceTrainingYes}
import org.scalatest.MustMatchers
import utils.AmlsSpec
import jto.validation.Path
import jto.validation.ValidationError
import play.api.i18n.Messages
import views.Fixture

class remove_responsible_personSpec extends AmlsSpec with MustMatchers  {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)
  }

  "remove_responsible_person view" must {

    "have correct title" in new ViewFixture {

      val form2: ValidForm[ExperienceTraining] = Form2(ExperienceTrainingYes("info"))

      def view = views.html.responsiblepeople.remove_responsible_person(form2, 1, "Gary", true, false)

      doc.title() must startWith(Messages("responsiblepeople.remove.responsible.person.title") + " - " + Messages("summary.responsiblepeople"))

    }

    "have correct heading" in new ViewFixture {

      val form2: ValidForm[ExperienceTraining] = Form2(ExperienceTrainingYes("info"))

      def view = views.html.responsiblepeople.remove_responsible_person(form2, 1, "Gary", true, false)

      heading.html() must be(Messages("responsiblepeople.remove.responsible.person.title"))
    }

    "show date form if boolean is set to true" in new ViewFixture {

      val form2: ValidForm[ExperienceTraining] = Form2(ExperienceTrainingYes("info"))

      def view = views.html.responsiblepeople.remove_responsible_person(form2, 1, "Gary", true, true)

      form.html() must include (Messages("responsiblepeople.remove.responsible.person.enddate.lbl"))

    }

    "hide date form if boolean is set to false" in new ViewFixture {

      val form2: ValidForm[ExperienceTraining] = Form2(ExperienceTrainingYes("info"))

      def view = views.html.responsiblepeople.remove_responsible_person(form2, 1, "Gary", true, false)

      form.html() must not include Messages("responsiblepeople.remove.responsible.person.enddate.lbl")

    }

    "show errors in correct places when validation fails" in new ViewFixture {

      val messageKey1 = "definitely not a message key"
      val endDateField = "endDate"

      val form2: InvalidForm = InvalidForm(
        Map("thing" -> Seq("thing")),
        Seq((Path \ endDateField, Seq(ValidationError(messageKey1))))
      )

      def view = views.html.responsiblepeople.remove_responsible_person(form2, 1, "Gary", true, true)

      errorSummary.html() must include(messageKey1)

      doc.getElementById(endDateField).html() must include(messageKey1)
    }
  }
}
