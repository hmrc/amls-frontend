package views.responsiblepeople

import forms.{Form2, InvalidForm, ValidForm}
import models.responsiblepeople.{ExperienceTraining, ExperienceTrainingYes}
import org.scalatest.MustMatchers
import utils.GenericTestHelper
import jto.validation.Path
import jto.validation.ValidationError
import play.api.i18n.Messages
import views.Fixture

class remove_responsible_personSpec extends GenericTestHelper with MustMatchers  {

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

    }

    "hide date form if boolean is set to false" in new ViewFixture {

      val form2: ValidForm[ExperienceTraining] = Form2(ExperienceTrainingYes("info"))

      def view = views.html.responsiblepeople.remove_responsible_person(form2, 1, "Gary", true, false)

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
