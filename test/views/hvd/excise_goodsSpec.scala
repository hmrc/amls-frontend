package views.hvd

import forms.{InvalidForm, ValidForm, Form2}
import models.hvd.ExciseGoods
import org.scalatest.{MustMatchers, WordSpec}
import org.scalatestplus.play.OneAppPerSuite
import play.api.data.mapping.Path
import play.api.data.validation.ValidationError
import play.api.i18n.Messages
import views.ViewFixture


class excise_goodsSpec extends WordSpec with MustMatchers with OneAppPerSuite {

  "excise_goods view" must {
    "have correct title" in new ViewFixture {

      val form2: ValidForm[ExciseGoods] = Form2(ExciseGoods(true))

      def view = views.html.hvd.excise_goods(form2, true)

      doc.title must startWith(Messages("hvd.excise.goods.title") + " - " + Messages("summary.hvd"))
    }

    "have correct headings" in new ViewFixture {

      val form2: ValidForm[ExciseGoods] = Form2(ExciseGoods(false))

      def view = views.html.hvd.excise_goods(form2, true)

      heading.html must be(Messages("hvd.excise.goods.title"))
      subHeading.html must include(Messages("summary.hvd"))

    }

    "show errors in the correct locations" in new ViewFixture {

      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq(
          (Path \ "exciseGoods") -> Seq(ValidationError("not a message Key"))
        ))

      def view = views.html.hvd.excise_goods(form2, true)

      errorSummary.html() must include("not a message Key")

      doc.getElementById("exciseGoods")
        .getElementsByClass("error-notification").first().html() must include("not a message Key")
    }
  }
}
