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

package views.businessmatching.updateservice.add

import forms.{EmptyForm, InvalidForm}
import jto.validation.{Path, ValidationError}
import org.scalatest.MustMatchers
import play.api.i18n.Messages
import utils.GenericTestHelper
import views.Fixture

class add_more_activitiesSpec extends GenericTestHelper with MustMatchers  {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)
    def view = views.html.businessmatching.updateservice.add.add_more_activities(EmptyForm,Set.empty[String])
  }

  "The add_more_activities view" must {

    "have the correct title" in new ViewFixture {
      doc.title must startWith(Messages("businessmatching.updateservice.addmoreactivities.title") + " - " + Messages("summary.updateservice"))
    }

    "have correct heading" in new ViewFixture {
      heading.html must be(Messages("businessmatching.updateservice.addmoreactivities.heading"))
    }

    "have correct subHeading" in new ViewFixture {
      subHeading.html must include(Messages("summary.updateservice"))
    }

    "show the correct content" in new ViewFixture {
      doc.body().text() must include(Messages("lbl.yes"))
      doc.body().text() must include(Messages("lbl.no"))
    }

    "not show the return link when specified" in new ViewFixture {
      override def view = views.html.businessmatching.updateservice.add.add_more_activities(EmptyForm, Set.empty[String], showReturnLink = false)

      doc.body().text() must not include Messages("link.return.registration.progress")
    }

    " show the return link when specified" in new ViewFixture {
      override def view = views.html.businessmatching.updateservice.add.add_more_activities(EmptyForm, Set.empty[String], showReturnLink = true)

      doc.body().text() must include(Messages("link.return.registration.progress"))
    }

    "show errors in the correct locations" in new ViewFixture {

      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq((Path \ "businessmatching.updateservice.addmoreactivities") -> Seq(ValidationError("not a message Key"))))

      override def view = views.html.businessmatching.updateservice.add.add_more_activities(form2, Set.empty[String])

      errorSummary.html() must include("not a message Key")
    }
  }
}