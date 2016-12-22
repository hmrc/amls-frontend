package views.responsiblepeople

import forms.{Form2, InvalidForm, ValidForm}
import models.businessmatching.{AccountancyServices, BusinessActivities, BusinessActivity}
import models.responsiblepeople.{ExperienceTraining, ExperienceTrainingYes}
import org.scalatest.{MustMatchers, WordSpec}
import org.scalatestplus.play.OneAppPerSuite
import play.api.data.mapping.Path
import play.api.data.validation.ValidationError
import play.api.i18n.Messages
import views.ViewFixture

class experience_trainingSpec extends WordSpec with MustMatchers with OneAppPerSuite {

  "experience_training view" must {

    "have correct title" in new ViewFixture {

      val businessActivities = BusinessActivities(Set(AccountancyServices))
      val form2: ValidForm[ExperienceTraining] = Form2(ExperienceTrainingYes("info"))

      def view: _root_.play.twirl.api.HtmlFormat.Appendable =
        views.html.responsiblepeople.experience_training(form2, businessActivities, false, 0)

      doc.title() must startWith(Messages("responsiblepeople.experiencetraining.title") + " - " + Messages("summary.responsiblepeople"))

    }

    "have correct heading" in new ViewFixture {

      val businessActivities = BusinessActivities(Set(AccountancyServices))
      val form2: ValidForm[ExperienceTraining] = Form2(ExperienceTrainingYes("info"))

      def view: _root_.play.twirl.api.HtmlFormat.Appendable =
        views.html.responsiblepeople.experience_training(form2, businessActivities, false, 0)

      heading.html() must be(Messages("responsiblepeople.experiencetraining.title"))
    }

    "show errors in correct places when validation fails" in new ViewFixture {

      val businessActivities = BusinessActivities(Set(AccountancyServices))
      val messageKey1 = "definitely not a message key"
      val messageKey2 = "also not a message key"
      val experienceTrainingField = "experienceTraining"
      val experienceInformationField = "experienceInformation"
      val form2: InvalidForm = InvalidForm(Map("thing" -> Seq("thing")),
        Seq((Path \ experienceTrainingField, Seq(ValidationError(messageKey1))),
          (Path \ experienceInformationField, Seq(ValidationError(messageKey2)))))

      def view: _root_.play.twirl.api.HtmlFormat.Appendable =
        views.html.responsiblepeople.experience_training(form2, businessActivities, false, 0)

      errorSummary.html() must include(messageKey1)
      errorSummary.html() must include(messageKey2)

      doc.getElementById(experienceTrainingField).html() must include(messageKey1)
      doc.getElementById(experienceInformationField + "-fieldset").html() must include(messageKey2)
    }
  }
}
