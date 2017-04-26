package views.responsiblepeople

import forms.{Form2, InvalidForm, ValidForm}
import models.businessmatching.{AccountancyServices, BusinessActivities, BusinessActivity}
import models.responsiblepeople.{ExperienceTraining, ExperienceTrainingYes}
import org.scalatest.{MustMatchers}
import  utils.GenericTestHelper
import jto.validation.Path
import jto.validation.ValidationError
import play.api.i18n.Messages
import views.Fixture

class experience_trainingSpec extends GenericTestHelper with MustMatchers  {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)
  }

  "experience_training view" must {

    "have correct title" in new ViewFixture {

      val businessActivities = BusinessActivities(Set(AccountancyServices))
      val form2: ValidForm[ExperienceTraining] = Form2(ExperienceTrainingYes("info"))

      def view: _root_.play.twirl.api.HtmlFormat.Appendable =
        views.html.responsiblepeople.experience_training(form2, businessActivities, false, 0, false, "FirstName LastName")

      doc.title must be(Messages("responsiblepeople.experiencetraining.title") +
        " - " + Messages("summary.responsiblepeople") +
        " - " + Messages("title.amls") +
        " - " + Messages("title.gov"))
    }

    "have correct heading" in new ViewFixture {

      val businessActivities = BusinessActivities(Set(AccountancyServices))
      val form2: ValidForm[ExperienceTraining] = Form2(ExperienceTrainingYes("info"))

      def view: _root_.play.twirl.api.HtmlFormat.Appendable =
        views.html.responsiblepeople.experience_training(form2, businessActivities, false, 0, false, "FirstName LastName")

      heading.html() must be(Messages("responsiblepeople.experiencetraining.heading", "FirstName LastName"))
    }

    "show errors in correct places when validation fails" in new ViewFixture {
      val businessActivities = BusinessActivities(Set(AccountancyServices))
      val messageKey1 = "definitely not a message key"
      val messageKey2 = "also not a message key"

      val form2: InvalidForm = InvalidForm(Map("thing" -> Seq("thing")),
        Seq((Path \ "experienceTraining", Seq(ValidationError(messageKey1))),
          (Path \ "experienceInformation", Seq(ValidationError(messageKey2)))))

      def view: _root_.play.twirl.api.HtmlFormat.Appendable =
        views.html.responsiblepeople.experience_training(form2, businessActivities, false, 0, false, "FirstName LastName")

      errorSummary.html() must include(messageKey1)
      errorSummary.html() must include(messageKey2)
    }
  }
}
