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

import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import jto.validation.{Path, ValidationError}
import models.businessmatching.{MsbServices, TransmittingMoney}
import org.scalatest.MustMatchers
import play.api.i18n.Messages
import utils.GenericTestHelper
import views.Fixture
import views.html.businessmatching.updateservice.add._


class what_do_you_do_hereSpec extends GenericTestHelper with MustMatchers {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)

    def view = what_do_you_do_here(EmptyForm, edit = false)
  }

  " The what_do_you_do_here view" must {

    "have correct title" in new ViewFixture {

      val form2: ValidForm[MsbServices] = Form2(MsbServices(Set(TransmittingMoney)))

      override def view = what_do_you_do_here(form2, edit = false)

      doc.title must startWith(Messages("businessmatching.updateservice.whatdoyoudohere.title") + " - " + Messages("summary.updateservice"))
      heading.html must be(Messages("businessmatching.updateservice.whatdoyoudohere.heading"))

      subHeading.html must include(Messages("summary.updateservice"))

    }

    "show errors in the correct locations" in new ViewFixture {

      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq(
          (Path \ "msbWhatdoyoudohere") -> Seq(ValidationError("not a message Key"))
        ))

      override def view = what_do_you_do_here(form2, edit = false)

      errorSummary.html() must include("not a message Key")

      doc.getElementById("msbWhatdoyoudohere")
        .getElementsByClass("error-notification").first().html() must include("not a message Key")

    }

    "hide the return to progress link" in new ViewFixture {
      val form2: ValidForm[MsbServices] = Form2(MsbServices(Set(TransmittingMoney)))

      override def view = what_do_you_do_here(form2, edit = false)

      doc.body().text() must not include Messages("link.return.registration.progress")
    }
  }
}