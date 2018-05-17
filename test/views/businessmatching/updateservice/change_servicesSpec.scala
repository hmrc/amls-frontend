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

package views.businessmatching.updateservice

import forms.{EmptyForm, InvalidForm}
import jto.validation.{Path, ValidationError}
import org.scalatest.MustMatchers
import play.api.i18n.Messages
import utils.AmlsSpec
import views.Fixture

class change_servicesSpec extends AmlsSpec with MustMatchers {

  val allowAdd = true
  val allowRemove = true

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)
    def view = views.html.businessmatching.updateservice.change_services(EmptyForm, Set("ServiceOne"), allowAdd, allowRemove)
  }

  trait MultipleViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)
    def view = views.html.businessmatching.updateservice.change_services(EmptyForm, Set("ServiceOne", "ServiceTwo"), allowAdd, allowRemove)
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

    "show the correct content" in new ViewFixture {
      doc.body().text() must include(Messages("businessmatching.updateservice.changeservices.choice.add"))
      doc.body().html() must include("changeServices-add")
      doc.body().text() must include(Messages("link.return.registration.progress"))
    }

    "show the correct business type text for only one existing service" in new ViewFixture {
      doc.body().text() must include(Messages("businessmatching.updateservice.changeservices.existing.single"))
    }

    "show the correct business type text for more than one existing service" in new MultipleViewFixture {
      doc.body().text() must include(Messages("businessmatching.updateservice.changeservices.existing.multiple"))
    }

    "show errors in the correct locations" in new ViewFixture {

      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq((Path \ "businessmatching.updateservice.changeServices") -> Seq(ValidationError("not a message Key"))))

      override def view = views.html.businessmatching.updateservice.change_services(form2, Set.empty[String], allowAdd, allowRemove)

      errorSummary.html() must include("not a message Key")
    }
  }
}