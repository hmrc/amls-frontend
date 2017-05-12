package views.msb

import forms.{Form2, ValidForm}
import models.moneyservicebusiness.WhichCurrencies
import org.scalatest.{MustMatchers, WordSpec}
import org.scalatestplus.play.OneAppPerSuite
import play.api.i18n.Messages
import play.api.test.FakeApplication
import utils.GenericTestHelper
import views.Fixture


class which_currenciesPreRelease7Spec extends GenericTestHelper with MustMatchers  {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)
  }

  override lazy val app = FakeApplication(additionalConfiguration = Map("Test.microservice.services.feature-toggle.release7" -> false))

  "The Which Currencies view" should {

    "not ask the user whether the use foreign currencies" in new ViewFixture {

      val formData = Form2(WhichCurrencies(Seq("GBP"), None, None, None, None))

      def view = views.html.msb.which_currencies(formData, edit = true)

      Option(doc.getElementById("usesForeignCurrencies-Yes")).isEmpty must be(true)
      Option(doc.getElementById("usesForeignCurrencies-No")).isEmpty must be(true)

    }

  }

}