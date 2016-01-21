package controllers.tradingpremises

import connectors.DataCacheConnector
import models.aboutthebusiness._
import org.jsoup.Jsoup
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.i18n.Messages
import play.api.test.FakeRequest
import play.api.test.Helpers._
import utils.AuthorisedFixture

import scala.concurrent.Future


class WhatYouNeedControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with ScalaFutures {
  trait Fixture extends AuthorisedFixture {
    self =>

    val controller = new WhatYouNeedController {
      override val dataCacheConnector = mock[DataCacheConnector]
      override val authConnector = self.authConnector
    }
  }

  "WhatYouNeedController" must {

    "on get display the what you need page" in new Fixture {
      val result = controller.get()(request)
      status(result) must be(OK)
      contentAsString(result) must include(Messages("tradingpremises.whatyouneed.title"))
    }

    "on click of continue button, page to be redirectd to " in new Fixture{

      val result = controller.get()(request)
      status(result) must be(OK)
      contentAsString(result) must include(Messages("amls.continue_button.text"))
    //  contentAsString(result) must include("/anti-money-laundering/trading-premises/premises")
    }
  }

}


