/*
 * Copyright 2021 HM Revenue & Customs
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

package views.hvd

import forms.{Form2, InvalidForm, ValidForm}
import models.hvd.ExciseGoods
import org.scalatest.MustMatchers
import utils.AmlsViewSpec
import jto.validation.Path
import jto.validation.ValidationError
import play.api.i18n.Messages
import views.Fixture
import views.html.hvd.excise_goods


class excise_goodsSpec extends AmlsViewSpec with MustMatchers  {

  trait ViewFixture extends Fixture {
    lazy val excise_goods = app.injector.instanceOf[excise_goods]
    implicit val requestWithToken = addTokenForView()
  }

  "excise_goods view" must {

    "have the back link button" in new ViewFixture {

      val form2: ValidForm[ExciseGoods] = Form2(ExciseGoods(true))

      def view = excise_goods(form2, true)

      doc.getElementsByAttributeValue("class", "link-back") must not be empty
    }

    "have correct title" in new ViewFixture {

      val form2: ValidForm[ExciseGoods] = Form2(ExciseGoods(true))

      def view = excise_goods(form2, true)

      doc.title must startWith(Messages("hvd.excise.goods.title") + " - " + Messages("summary.hvd"))
    }

    "have correct headings" in new ViewFixture {

      val form2: ValidForm[ExciseGoods] = Form2(ExciseGoods(false))

      def view = excise_goods(form2, true)

      heading.html must be(Messages("hvd.excise.goods.title"))
      subHeading.html must include(Messages("summary.hvd"))

    }

    "show errors in the correct locations" in new ViewFixture {

      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq(
          (Path \ "exciseGoods") -> Seq(ValidationError("not a message Key"))
        ))

      def view = excise_goods(form2, true)

      errorSummary.html() must include("not a message Key")

      doc.getElementById("exciseGoods")
        .getElementsByClass("error-notification").first().html() must include("not a message Key")
    }
  }
}
