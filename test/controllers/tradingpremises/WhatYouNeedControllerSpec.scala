package controllers.tradingpremises

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.i18n.Messages
import play.api.test.Helpers._
import utils.AuthorisedFixture

class WhatYouNeedControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar {

  val mockDataCacheConnector = mock[DataCacheConnector]

  trait Fixture extends AuthorisedFixture {
    self =>

    val controller = new WhatYouNeedController {
      override val dataCacheConnector = mockDataCacheConnector
      override val authConnector = self.authConnector
    }
  }

  "WhatYouNeedController" must {

    "use correct services" in new Fixture {
      WhatYouNeedController.authConnector must be(AMLSAuthConnector)
    }


    "load the what you need page" in new Fixture {
      val result = controller.get(1)(request)
      status(result) must be(OK)
      contentAsString(result) must include(Messages("tradingpremises.whatyouneed.title"))
    }
  }

}
