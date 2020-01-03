/*
 * Copyright 2020 HM Revenue & Customs
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
import forms.EmptyForm
import generators.ResponsiblePersonGenerator
import jto.validation.{Path, ValidationError}
import org.scalatest.MustMatchers
import play.api.i18n.Messages
import utils.AmlsViewSpec
import views.Fixture
import views.html.businessmatching.updateservice.add._

class which_fit_and_properSpec extends AmlsViewSpec with MustMatchers with ResponsiblePersonGenerator{

  val rp = responsiblePersonGen.sample.get

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addTokenForView()

    def view = which_fit_and_proper(EmptyForm, false, Seq((rp, 0)))
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

    "have the back link button" in new ViewFixture {
      doc.getElementsByAttributeValue("class", "link-back") must not be empty
    }

    "not show the return link" in new ViewFixture {
      doc.body().text() must not include Messages("link.return.registration.progress")
    }

    "show errors in the correct locations" in new ViewFixture {
      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq((Path \ "responsiblePeople") -> Seq(ValidationError("not a message Key"))))

      override def view = which_fit_and_proper(form2, false, Seq((rp, 0)))

      errorSummary.html() must include("not a message Key")

      doc.getElementById("responsiblePeople")
        .getElementsByClass("error-notification").first().html() must include("not a message Key")
    }
  }

}
