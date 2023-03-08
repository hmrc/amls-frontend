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

package views.businessmatching.updateservice.remove

import forms.{EmptyForm, InvalidForm, RemoveBusinessActivitiesFormProvider}
import jto.validation.{Path, ValidationError}
import models.businessmatching.BusinessActivities
import models.businessmatching.BusinessActivity.{AccountancyServices, BillPaymentServices, MoneyServiceBusiness}
import org.scalatest.MustMatchers
import play.api.data.FormError
import play.api.i18n.Messages
import utils.AmlsViewSpec
import views.Fixture
import views.html.businessmatching.updateservice.remove._

class remove_activitiesSpec extends AmlsViewSpec with MustMatchers {

  trait ViewFixture extends Fixture {
    lazy val formProvider = app.injector.instanceOf[RemoveBusinessActivitiesFormProvider]
    lazy val remove_activities = app.injector.instanceOf[remove_activities]
    implicit val requestWithToken = addTokenForView()

    override def view = remove_activities(formProvider(Seq.empty),
      edit = true,
      Seq.empty[String]
    )
  }

  "The select activities to remove view" must {

    "have the correct title" in new ViewFixture {
      doc.title must startWith(messages("businessmatching.updateservice.removeactivities.title.multibusinesses"))
    }

    "have correct heading" in new ViewFixture {
      heading.html must be(messages("businessmatching.updateservice.removeactivities.title.multibusinesses"))
    }

    "have correct subHeading" in new ViewFixture {
      subHeading.html must include(messages("summary.updateservice"))
    }

    "have the back link button" in new ViewFixture {
      doc.getElementById("back-link").text() mustBe "Back"
    }

    "show the correct content" in new ViewFixture {

      val submittedActivities = Seq(MoneyServiceBusiness)


      override def view = remove_activities(formProvider(Nil),
        edit = true,
        submittedActivities map BusinessActivities.getValue)

      submittedActivities foreach { a =>
        doc.body().text must include(messages(a.getMessage()))
        doc.body().html() must include(BusinessActivities.getValue(a))
      }

      doc.body().text() must include (messages("businessmatching.updateservice.removeactivities.title.multibusinesses"))
      doc.getElementById("button").text() must include (messages("businessmatching.updateservice.removeactivities.button"))

    }

    "show errors in the correct locations" in new ViewFixture {

      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq((Path \ "businessmatching.updateservice.removeactivities") -> Seq(ValidationError("not a message Key"))))

      override def view = remove_activities(
        formProvider(Nil).withError(FormError("businessmatching.updateservice.removeactivities", "not a message Key")),
        edit = true,
        Seq.empty[String]
      )

      doc.getElementsByClass("govuk-error-summary").html() must include("not a message Key")
    }
  }
}