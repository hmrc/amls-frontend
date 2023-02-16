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

package views.businessmatching.updateservice

import forms.{EmptyForm, InvalidForm}
import jto.validation.{Path, ValidationError}
import org.scalatest.MustMatchers
import play.api.i18n.Messages
import utils.AmlsViewSpec
import views.Fixture
import views.html.businessmatching.updateservice.change_services

class change_servicesSpec extends AmlsViewSpec with MustMatchers {

  val allowAdd = true
  val allowRemove = true

  trait ViewFixture extends Fixture {
    lazy val change_services = app.injector.instanceOf[change_services]
    implicit val requestWithToken = addTokenForView()
    def view = change_services(EmptyForm, Set("ServiceOne"), allowAdd)
  }

  trait MultipleViewFixture extends Fixture {
    lazy val change_services = app.injector.instanceOf[change_services]
    implicit val requestWithToken = addTokenForView()
    def view = change_services(EmptyForm, Set("ServiceOne", "ServiceTwo"), allowAdd)
  }

  "The change_services view" must {

    "have the correct title" in new ViewFixture {
      doc.title must startWith(Messages("businessmatching.updateservice.changeservices.title") + " - " + Messages("summary.updateservice"))
    }

    "have correct heading" in new ViewFixture {
      heading.html must be(Messages("businessmatching.updateservice.changeservices.heading"))
    }

    "have correct subHeading" in new ViewFixture {
      subHeading.html must include(Messages("summary.updateservice"))
    }
    
    "have a back link" in new ViewFixture {
      doc.getElementsByAttributeValue("class", "link-back") must not be empty
    }

    "show the correct content" in new ViewFixture {
      doc.body().text() must include(Messages("businessmatching.updateservice.changeservices.choice.add"))
      doc.body().html() must include("changeServices-add")
      doc.body().text() must include(Messages("link.return.registration.progress"))
      Option(doc.getElementById("button-continue")).isDefined mustBe true
    }

    "show errors in the correct locations" in new ViewFixture {

      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq((Path \ "businessmatching.updateservice.changeServices") -> Seq(ValidationError("not a message Key"))))

      override def view = change_services(form2, Set.empty[String], allowAdd)

      errorSummary.html() must include("not a message Key")
    }
  }
}