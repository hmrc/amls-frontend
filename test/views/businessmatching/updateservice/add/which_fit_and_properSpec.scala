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
import models.responsiblepeople.ResponsiblePeople
import org.scalatest.MustMatchers
import play.api.i18n.Messages
import utils.GenericTestHelper
import views.Fixture
import views.html.businessmatching.updateservice.add._

class which_fit_and_properSpec extends GenericTestHelper with MustMatchers {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)
    def view = which_fit_and_proper(EmptyForm, Seq.empty[(ResponsiblePeople, Int)])
  }

  "The which_fit_and_proper view" must {

      "have the correct title" in new ViewFixture {
        doc.title must startWith(Messages("businessmatching.updateservice.whichfitandproper.title") + " - " + Messages("summary.updateservice"))
      }

      "have correct heading" in new ViewFixture {
        heading.html must be(Messages("businessmatching.updateservice.whichfitandproper.heading"))
      }

      "have correct subHeading" in new ViewFixture {
        subHeading.html must include(Messages("summary.updateservice"))
      }

      "not show the return link" in new ViewFixture {
        doc.body().text() must not include Messages("link.return.registration.progress")
      }

    "show errors in the correct locations" in new ViewFixture {
      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq((Path \ "responsiblePeople") -> Seq(ValidationError("not a message Key"))))

      override def view = fit_and_proper(form2, true)

      errorSummary.html() must include("not a message Key")

      doc.getElementById("responsiblePeople")
        .getElementsByClass("error-notification").first().html() must include("not a message Key")
    }
  }

}
