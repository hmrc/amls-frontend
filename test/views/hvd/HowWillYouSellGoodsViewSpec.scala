/*
 * Copyright 2024 HM Revenue & Customs
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

import forms.hvd.SalesChannelFormProvider
import models.hvd.HowWillYouSellGoods
import models.hvd.SalesChannel.{Retail, Wholesale}
import org.scalatest.matchers.must.Matchers
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.FakeRequest
import utils.AmlsViewSpec
import views.Fixture
import views.html.hvd.HowWillYouSellGoodsView

class HowWillYouSellGoodsViewSpec extends AmlsViewSpec with Matchers {

  lazy val goodsView = inject[HowWillYouSellGoodsView]
  lazy val fp        = inject[SalesChannelFormProvider]

  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  trait ViewFixture extends Fixture {
    implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView()
  }

  "HowWillYouSellGoodsView view" must {

    "have correct title" in new ViewFixture {

      def view = goodsView(fp().fill(HowWillYouSellGoods(Set(Retail))), true)

      doc.title must startWith(messages("hvd.how-will-you-sell-goods.title") + " - " + messages("summary.hvd"))
    }

    "have correct headings" in new ViewFixture {

      def view = goodsView(fp().fill(HowWillYouSellGoods(Set(Wholesale))), true)

      heading.html    must be(messages(messages("hvd.how-will-you-sell-goods.title")))
      subHeading.html must include(messages(messages("summary.hvd")))

    }

    behave like pageWithErrors(
      goodsView(fp().withError("salesChannels", "error.required.hvd.how-will-you-sell-goods"), false),
      "salesChannels",
      "error.required.hvd.how-will-you-sell-goods"
    )

    behave like pageWithBackLink(goodsView(fp(), false))
  }
}
