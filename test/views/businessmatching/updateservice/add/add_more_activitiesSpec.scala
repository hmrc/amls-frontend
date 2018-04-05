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

import forms.EmptyForm
import org.scalatest.MustMatchers
import play.api.i18n.Messages
import utils.GenericTestHelper
import views.Fixture

class add_more_activitiesSpec extends GenericTestHelper with MustMatchers  {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)
    def view = views.html.businessmatching.updateservice.add_more_activities(EmptyForm, Set.empty[String])
  }

  "add_more_activities view" must {
    "have correct content" in new ViewFixture {


      doc.title must startWith(Messages("businessmatching.updateservice.addmoreactivities.title"))
      heading.html must be(Messages("businessmatching.updateservice.addmoreactivities.title"))
      subHeading.html must include(Messages("summary.updateinformation"))
      //doc.body().text() must include(Messages("link.return.registration.progress"))
    }

    "show errors in the correct locations" in new ViewFixture {
      fail()
//      val forPm2: InvalidForm = InvalidForm(Map.empty,
//        Seq(A
//          (Path \ "changeServices") -> Seq(ValidationError("not a message Key"))
//        ))
//
//      def view = views.html.businessmatching.updateservice.change_services(form2, Set.empty[String])
//
//      errorSummary.html() must include("not a message Key")
//
//      doc.getElementById("changeServices")
//        .getElementsByClass("error-notification").first().html() must include("not a message Key")
    }

    "show two radio buttons, yes and no" in new ViewFixture {

      doc.body().text() must include(Messages("lbl.yes"))
      doc.body().text() must include(Messages("lbl.no"))
    }
  }
}