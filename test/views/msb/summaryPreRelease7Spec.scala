package views.msb

import models.businessmatching.{CurrencyExchange, MsbServices}
import models.moneyservicebusiness.{MoneyServiceBusiness, WhichCurrencies}
import org.scalatest.{MustMatchers, WordSpec}
import org.scalatestplus.play.OneAppPerSuite
import play.api.i18n.Messages
import play.api.test.FakeApplication
import utils.GenericTestHelper
import views.Fixture


class summaryPreRelease7Spec extends  GenericTestHelper with MustMatchers {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)
  }

  override lazy val app = FakeApplication(additionalConfiguration = Map("Test.microservice.services.feature-toggle.release7" -> false))

  "MSB Summary page" should {

    "indicate whether foreign currencies are used" in new ViewFixture {

      val model = MoneyServiceBusiness(whichCurrencies = Some(WhichCurrencies(Seq("GBP"), None, None, None, None)))

      def view = views.html.msb.summary(model, Some(MsbServices(Set(CurrencyExchange))), false)

      html.contains(Messages("msb.which_currencies.foreign_currencies_question")) must be(false)

    }

  }

}