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

package views.businessmatching.updateservice.remove

import forms.{EmptyForm, InvalidForm}
import jto.validation.{Path, ValidationError}
import models.businessmatching.{AccountancyServices, BillPaymentServices, BusinessActivities, MoneyServiceBusiness}
import org.scalatest.MustMatchers
import play.api.i18n.Messages
import utils.AmlsSpec
import views.Fixture
import views.html.businessmatching.updateservice.remove._

class remove_activitiesSpec extends AmlsSpec with MustMatchers {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)

    override def view = remove_activities(EmptyForm,
      edit = true,
      Seq.empty[String],
      Seq.empty[String]
    )
  }

  "The select activities to remove view" must {

    "have the correct title" in new ViewFixture {
      doc.title must startWith(Messages("businessmatching.updateservice.removeactivities.title") + " - " + Messages("summary.updateservice"))
    }

    "have correct heading" in new ViewFixture {
      heading.html must be(Messages("businessmatching.updateservice.removeactivities.heading"))
    }

    "have correct subHeading" in new ViewFixture {
      subHeading.html must include(Messages("summary.updateservice"))
    }

    "show the correct content" in new ViewFixture {

      val addedActivities = Seq(AccountancyServices, BillPaymentServices)
      val submittedActivities = Seq(MoneyServiceBusiness)


      override def view = remove_activities(EmptyForm,
        edit = true,
        addedActivities map BusinessActivities.getValue,
        submittedActivities map (_.getMessage)
      )

      addedActivities foreach { a =>
        doc.body().text must include(Messages(a.getMessage))
        doc.body().html() must include(BusinessActivities.getValue(a))
      }

      submittedActivities foreach { a =>
        doc.body().text() must include(Messages(a.getMessage))
      }

      doc.body().text() must include (Messages("businessmatching.updateservice.removeactivities.summary"))
      doc.getElementById("removeactivities-submit").text() must include (Messages("businessmatching.updateservice.removeactivities.button"))

    }

    "show errors in the correct locations" in new ViewFixture {

      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq((Path \ "businessmatching.updateservice.removeactivities") -> Seq(ValidationError("not a message Key"))))

      override def view = remove_activities(form2, edit = true, Seq.empty[String], Seq.empty[String])

      errorSummary.html() must include("not a message Key")
    }
  }
}