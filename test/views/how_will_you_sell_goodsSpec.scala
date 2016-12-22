package views


import java.net.URI

import forms.Form2
import models.hvd.{Retail, Hvd, HowWillYouSellGoods}
import org.jsoup.Jsoup
import org.scalatest.{fixture, WordSpec, MustMatchers}
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneAppPerSuite}
import play.api.mvc.{Headers, RequestHeader, Request}
import play.api.test.FakeRequest



trait HowWillYouSellGoodsViewFixture {
  implicit val request : Request[_] = FakeRequest()

  val view = views.html.hvd.how_will_you_sell_goods(forms.Form2[HowWillYouSellGoods](HowWillYouSellGoods(Seq(Retail))), false)
  lazy val html = view.body
  lazy val doc = Jsoup.parse(html)
  lazy val form = doc.getElementsByTag("form").first()
}

class how_will_you_sell_goodsSpec extends WordSpec with MustMatchers with MockitoSugar with OneAppPerSuite{
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
          override val view = views.html.hvd.how_will_you_sell_goods(forms.Form2[HowWillYouSellGoods](HowWillYouSellGoods(Seq(Retail))), true)
          new URI(form.attr("action")).getQuery must include ("edit=true")
      }
    }

    "contain 3 checkboxes" in new HowWillYouSellGoodsViewFixture {
      form.select("input[type=checkbox]").size() must be (3)
    }
  }
}
