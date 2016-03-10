package controllers.tradingpremises

import java.util.UUID

import connectors.DataCacheConnector
import models.businessactivities.{BusinessActivities, InvolvedInOtherYes}
import models.businessmatching.{BusinessActivities => BusinessMatchingBusinessActivities, _}
import models.tradingpremises.{TradingPremises, WhatDoesYourBusinessDo}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http.HeaderCarrier
import utils.AuthorisedFixture

import scala.concurrent.Future

class WhatDoesYourBusinessDoControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar {

  implicit val request = FakeRequest
  val userId = s"user-${UUID.randomUUID()}"
  val mockDataCacheConnector = mock[DataCacheConnector]

  trait Fixture extends AuthorisedFixture {
    self =>

    val whatDoesYourBusinessDoController = new WhatDoesYourBusinessDoController {
      override val dataCacheConnector = mockDataCacheConnector
      override val authConnector = self.authConnector
    }
  }

  val fieldElements = Array("report-name", "report-email", "report-action", "report-error")

  "WhatDoesYourBusinessDoController" must {

    "when the form is empty then go to what does your business do with empty fields" in new Fixture {

      val tradingPremises = TradingPremises()
      val mockCacheMap = mock[CacheMap]
      val businessActivities = BusinessActivities(involvedInOther = Some(InvolvedInOtherYes("test")))

      when(mockDataCacheConnector.fetchAll(any[HeaderCarrier], any[AuthContext]))
        .thenReturn(Future.successful(Some(mockCacheMap)))

      when(mockDataCacheConnector.fetchDataShortLivedCache[Seq[TradingPremises]](any())
        (any(), any(), any())).thenReturn(Future.successful(Some(Seq(tradingPremises))))

      when(mockDataCacheConnector.saveDataShortLivedCache[Seq[TradingPremises]](any(), any())
        (any(), any(), any())).thenReturn(Future.successful(Some(Seq(tradingPremises))))

      when(mockCacheMap.getEntry[BusinessActivities](BusinessActivities.key))
        .thenReturn(Some(businessActivities))

      when(mockCacheMap.getEntry[Seq[TradingPremises]](any())(any())).thenReturn(Some(Seq(tradingPremises)))

      val businessMatchingBusinessActivities = BusinessMatchingBusinessActivities(Set(AccountancyServices, BillPaymentServices, EstateAgentBusinessService))
      when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key)).thenReturn(Some(BusinessMatching(Some(businessMatchingBusinessActivities))))

      val RecordId = 1
      val result = whatDoesYourBusinessDoController.get(RecordId)(request)

      status(result) must be(OK)

      /*
            val document: Document = Jsoup.parse(contentAsString(result))
            document.select(s"input[id=activities-01]").hasAttr("checked") must be(false)
      */
    }

    "when the form is not-empty then go to what does your business do with fields populated" in new Fixture {

      val wdbd = WhatDoesYourBusinessDo(Set(AccountancyServices, BillPaymentServices))
      val tradingPremises = TradingPremises(None, None, Some(wdbd))

      val mockCacheMap = mock[CacheMap]
      val businessActivities = BusinessActivities(involvedInOther = Some(InvolvedInOtherYes("test")))

      when(mockDataCacheConnector.fetchAll(any[HeaderCarrier], any[AuthContext]))
        .thenReturn(Future.successful(Some(mockCacheMap)))

      when(mockDataCacheConnector.fetchDataShortLivedCache[Seq[TradingPremises]](any())
        (any(), any(), any())).thenReturn(Future.successful(Some(Seq(tradingPremises))))

      when(mockDataCacheConnector.saveDataShortLivedCache[Seq[TradingPremises]](any(), any())
        (any(), any(), any())).thenReturn(Future.successful(Some(Seq(tradingPremises))))

      when(mockCacheMap.getEntry[BusinessActivities](BusinessActivities.key))
        .thenReturn(Some(businessActivities))

      when(mockCacheMap.getEntry[Seq[TradingPremises]](any())(any())).thenReturn(Some(Seq(tradingPremises)))

      val businessMatchingBusinessActivities = BusinessMatchingBusinessActivities(Set(AccountancyServices, BillPaymentServices, EstateAgentBusinessService))
      when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key)).thenReturn(Some(BusinessMatching(Some(businessMatchingBusinessActivities))))

      val RecordId = 1
      val result = whatDoesYourBusinessDoController.get(RecordId)(request)

      status(result) must be(OK)

      val document: Document = Jsoup.parse(contentAsString(result))
      document.select(s"input[id=activities-01]").hasAttr("checked") must be(true)

    }

    "when no Business Matching then redirect to the trading premises with recordId " in new Fixture {

      val tradingPremises = TradingPremises()
      val mockCacheMap = mock[CacheMap]
      when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key)).thenReturn(None)

      when(whatDoesYourBusinessDoController.dataCacheConnector.fetchAll(any[HeaderCarrier], any[AuthContext]))
        .thenReturn(Future.successful(Some(mockCacheMap)))

      when(mockDataCacheConnector.fetchDataShortLivedCache[Seq[TradingPremises]](any())
        (any(), any(), any())).thenReturn(Future.successful(Some(Seq(tradingPremises))))

      when(mockDataCacheConnector.saveDataShortLivedCache[Seq[TradingPremises]](any(), any())
        (any(), any(), any())).thenReturn(Future.successful(Some(Seq(tradingPremises))))

      val RecordId = 1
      val result = whatDoesYourBusinessDoController.get(RecordId)(request)
      redirectLocation(result) must be(Some(s"/anti-money-laundering/trading-premises/premises/${RecordId}"))

      status(result) must be(SEE_OTHER)
    }


  }

}
