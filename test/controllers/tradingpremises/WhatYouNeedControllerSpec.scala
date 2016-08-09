package controllers.tradingpremises

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import models.businessmatching._
import org.jsoup.Jsoup
import org.scalatest.mock.MockitoSugar
import org.mockito.Mockito._
import org.mockito.Matchers._
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.i18n.Messages
import play.api.test.Helpers._
import utils.AuthorisedFixture

import scala.concurrent.Future

class WhatYouNeedControllerSpec extends PlaySpec with OneAppPerSuite with MockitoSugar {

  val mockDataCacheConnector = mock[DataCacheConnector]

  trait Fixture extends AuthorisedFixture {
    self =>

    val controller = new WhatYouNeedController {
      override val dataCacheConnector = mockDataCacheConnector
      override val authConnector = self.authConnector
    }
  }

  "WhatYouNeedController" must {

    "load the what you need page" in new Fixture {
      when (controller.dataCacheConnector.fetch[BusinessMatching](any())(any(),any(),any())) thenReturn(Future.successful(None))
        val result = controller.get(1)(request)
        status(result) must be(OK)
      val document = Jsoup.parse(contentAsString(result))
      document.title() must be(Messages("tradingpremises.whatyouneed.title"))

      status(result) mustBe OK
    }

    "load the what you need page when msb selected as an option in business matching" in new Fixture {
      val BusinessActivitiesModel = BusinessActivities(Set(MoneyServiceBusiness, TrustAndCompanyServices, TelephonePaymentService))
      val bm = Some(BusinessMatching(activities = Some(BusinessActivitiesModel)))

      when (controller.dataCacheConnector.fetch[BusinessMatching](any())(any(),any(),any())) thenReturn(Future.successful(bm))
      val result = controller.get(1)(request)
      status(result) must be(OK)
      contentAsString(result) must include(Messages("tradingpremises.whatyouneed.agents.sub.heading"))
      contentAsString(result) must include(Messages("tradingpremises.whatyouneed.agents.circumstances.info.text1"))
    }
  }

  it must {
    "use correct services" in new Fixture {
      WhatYouNeedController.authConnector must be(AMLSAuthConnector)
    }
  }
}
