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

package views


import java.net.URI

import models.hvd.{HowWillYouSellGoods, Retail}
import org.jsoup.Jsoup
import org.scalatest.MustMatchers
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc.Request
import utils.AmlsViewSpec
import views.html.hvd.how_will_you_sell_goods

class how_will_you_sell_goodsSpec extends AmlsViewSpec with MustMatchers with MockitoSugar {

  trait HowWillYouSellGoodsViewFixture {
    lazy val how_will_you_sell_goods = app.injector.instanceOf[how_will_you_sell_goods]
    implicit val request : Request[_] = addTokenForView()
    val view = how_will_you_sell_goods(forms.Form2[HowWillYouSellGoods](HowWillYouSellGoods(Set(Retail))), false)
    lazy val html = view.body
    lazy val doc = Jsoup.parse(html)
    lazy val form = doc.getElementsByTag("form").first()
  }

  "how will you sell goods view" should {
    "contain a form" which {
      "posts it's data" in new HowWillYouSellGoodsViewFixture {
        form.attr("method") must be("POST")
      }

      "targets the correct route" in new HowWillYouSellGoodsViewFixture {
        val x = new URI(form.attr("action")).getPath
        x must endWith ("how-will-you-sell")
      }

      "maintains the edit flag" in new HowWillYouSellGoodsViewFixture {
          override val view = how_will_you_sell_goods(forms.Form2[HowWillYouSellGoods](HowWillYouSellGoods(Set(Retail))), true)
          new URI(form.attr("action")).getQuery must include ("edit=true")
      }
    }

    "contain 3 checkboxes" in new HowWillYouSellGoodsViewFixture {
      form.select("input[type=checkbox]").size() must be (3)
    }
  }
}
