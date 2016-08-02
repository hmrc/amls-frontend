package controllers.tradingpremises

import java.util.UUID

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import models.businessactivities.{BusinessActivities, ExpectedBusinessTurnover, InvolvedInOtherYes}
import models.businessmatching.{BusinessActivities => BusinessMatchingActivities, _}
import models.tradingpremises.{TradingPremises, WhatDoesYourBusinessDo}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http.HeaderCarrier
import utils.AuthorisedFixture

import scala.concurrent.Future

class WhatDoesYourBusinessDoControllerSpec extends PlaySpec with OneAppPerSuite with MockitoSugar {

  val userId = s"user-${UUID.randomUUID()}"
  val mockDataCacheConnector = mock[DataCacheConnector]

  trait Fixture extends AuthorisedFixture {
    self =>

    val whatDoesYourBusinessDoController = new WhatDoesYourBusinessDoController {
      override val dataCacheConnector = mockDataCacheConnector
      override val authConnector = self.authConnector
    }
  }

  val emptyCache = CacheMap("", Map.empty)

  val fieldElements = Array("report-name", "report-email", "report-action", "report-error")

  "WhatDoesYourBusinessDoController" must {

    "use correct services" in new Fixture {
      WhatDoesYourBusinessDoController.authConnector must be(AMLSAuthConnector)
      WhatDoesYourBusinessDoController.dataCacheConnector must be(DataCacheConnector)
    }

    "load what does your business do with empty fields" in new Fixture {

      val tradingPremises = TradingPremises()
      val mockCacheMap = mock[CacheMap]
      val businessActivities = BusinessActivities(involvedInOther = Some(InvolvedInOtherYes("test")))

      when(mockDataCacheConnector.fetchAll(any[HeaderCarrier], any[AuthContext]))
        .thenReturn(Future.successful(Some(mockCacheMap)))

      when(mockDataCacheConnector.fetch[Seq[TradingPremises]](any())
        (any(), any(), any())).thenReturn(Future.successful(Some(Seq(tradingPremises))))

      when(mockDataCacheConnector.save[Seq[TradingPremises]](any(), any())
        (any(), any(), any())).thenReturn(Future.successful(emptyCache))

      when(mockCacheMap.getEntry[BusinessActivities](BusinessActivities.key))
        .thenReturn(Some(businessActivities))

      when(mockCacheMap.getEntry[Seq[TradingPremises]](any())(any())).thenReturn(Some(Seq(tradingPremises)))

      val businessMatchingActivities = BusinessMatchingActivities(Set(AccountancyServices, BillPaymentServices, EstateAgentBusinessService))
      when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key)).thenReturn(Some(BusinessMatching(None, Some(businessMatchingActivities))))

      val RecordId = 1
      val result = whatDoesYourBusinessDoController.get(RecordId)(request)

      status(result) must be(OK)

      val document: Document = Jsoup.parse(contentAsString(result))
      document.select(s"input[id=activities-01]").hasAttr("checked") must be(false)
    }

    "load what does your business do with fields populated if the the form is not-empty" in new Fixture {

      val wdbd = WhatDoesYourBusinessDo(Set(AccountancyServices, BillPaymentServices))
      val tradingPremises = TradingPremises(None,None, None, Some(wdbd))

      val mockCacheMap = mock[CacheMap]
      val businessActivities = BusinessActivities(involvedInOther = Some(InvolvedInOtherYes("test")),
        expectedBusinessTurnover = Some(ExpectedBusinessTurnover.Fifth))

      when(mockDataCacheConnector.fetchAll(any[HeaderCarrier], any[AuthContext]))
        .thenReturn(Future.successful(Some(mockCacheMap)))

      when(mockDataCacheConnector.fetch[Seq[TradingPremises]](any())
        (any(), any(), any())).thenReturn(Future.successful(Some(Seq(tradingPremises))))

      when(mockDataCacheConnector.save[Seq[TradingPremises]](any(), any())
        (any(), any(), any())).thenReturn(Future.successful(emptyCache))

      when(mockCacheMap.getEntry[BusinessActivities](BusinessActivities.key))
        .thenReturn(Some(businessActivities))

      when(mockCacheMap.getEntry[Seq[TradingPremises]](any())(any())).thenReturn(Some(Seq(tradingPremises)))

      val businessMatchingActivities = BusinessMatchingActivities(Set(AccountancyServices, BillPaymentServices, EstateAgentBusinessService))
      when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key)).thenReturn(Some(BusinessMatching(None, Some(businessMatchingActivities))))

      val recordId = 1
      val result = whatDoesYourBusinessDoController.get(recordId)(request)

      status(result) must be(OK)

      val document: Document = Jsoup.parse(contentAsString(result))
      document.select(s"input[id=activities-01]").hasAttr("checked") must be(true)

    }

    "must redirect to the trading premises with recordId when no Business Activity" in new Fixture {

      val tradingPremises = TradingPremises()
      val mockCacheMap = mock[CacheMap]
      when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key)).thenReturn(None)

      when(mockDataCacheConnector.fetchAll(any[HeaderCarrier], any[AuthContext]))
        .thenReturn(Future.successful(Some(mockCacheMap)))

      when(mockDataCacheConnector.fetch[Seq[TradingPremises]](any())
        (any(), any(), any())).thenReturn(Future.successful(Some(Seq(tradingPremises))))

      when(mockDataCacheConnector.save[Seq[TradingPremises]](any(), any())
        (any(), any(), any())).thenReturn(Future.successful(emptyCache))

      val recordId = 1
      val result = whatDoesYourBusinessDoController.get(recordId)(request)
      redirectLocation(result) must be(Some(routes.WhereAreTradingPremisesController.get(recordId).url))

      status(result) must be(SEE_OTHER)
    }
  }

  "WhatDoesYourBusinessDoController post" must {

    "for an Invalid Request must give a Bad Request" in new Fixture {

      val tradingPremises = TradingPremises(None, None, None)
      val mockCacheMap = mock[CacheMap]

      when(mockDataCacheConnector.fetchAll(any[HeaderCarrier], any[AuthContext]))
        .thenReturn(Future.successful(Some(mockCacheMap)))

      when(mockDataCacheConnector.fetch[Seq[TradingPremises]](any())
        (any(), any(), any())).thenReturn(Future.successful(Some(Seq(tradingPremises))))

      when(mockDataCacheConnector.save[Seq[TradingPremises]](any(), any())
        (any(), any(), any())).thenReturn(Future.successful(emptyCache))

      when(mockCacheMap.getEntry[Seq[TradingPremises]](any())(any())).thenReturn(Some(Seq(tradingPremises)))

      val businessMatchingActivities = BusinessMatchingActivities(Set(AccountancyServices, BillPaymentServices, EstateAgentBusinessService))
      when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key)).thenReturn(Some(BusinessMatching(None, Some(businessMatchingActivities))))

      val invalidRequest = request.withFormUrlEncodedBody(
        "activities" -> ""
      )

      val RecordId = 1
      val result = whatDoesYourBusinessDoController.post(RecordId)(invalidRequest)
      status(result) must be(BAD_REQUEST)
    }


    "for a Valid Request with SINGLE Activity must redirect to Summary Controller" in new Fixture {

      val wdbd = WhatDoesYourBusinessDo(Set(AccountancyServices))
      val tradingPremises = TradingPremises(None,None, None, Some(wdbd))
      val mockCacheMap = mock[CacheMap]

      when(mockDataCacheConnector.fetchAll(any[HeaderCarrier], any[AuthContext]))
        .thenReturn(Future.successful(Some(mockCacheMap)))

      when(mockDataCacheConnector.fetch[Seq[TradingPremises]](any())
        (any(), any(), any())).thenReturn(Future.successful(Some(Seq(tradingPremises))))

      when(mockDataCacheConnector.save[Seq[TradingPremises]](any(), any())
        (any(), any(), any())).thenReturn(Future.successful(emptyCache))

      when(mockCacheMap.getEntry[Seq[TradingPremises]](any())(any())).thenReturn(Some(Seq(tradingPremises)))

      val businessMatchingActivities = BusinessMatchingActivities(Set(AccountancyServices))
      when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key)).thenReturn(Some(BusinessMatching(None, Some(businessMatchingActivities))))

      val newRequest = request.withFormUrlEncodedBody("activities[0]" -> "01")

      val RecordId = 1
      val result = whatDoesYourBusinessDoController.post(RecordId)(newRequest)
      status(result) must be(SEE_OTHER)
    }


    "for a Valid Request with multiple ACTIVITIES must redirect to Summary Controller" in new Fixture {

      val wdbd = WhatDoesYourBusinessDo(Set(AccountancyServices, BillPaymentServices))
      val tradingPremises = TradingPremises(None,None, None, Some(wdbd))
      val businessMatchingActivities = BusinessMatchingActivities(Set(AccountancyServices,
        BillPaymentServices, EstateAgentBusinessService))
      val mockCacheMap = mock[CacheMap]

      when(mockDataCacheConnector.fetchAll(any[HeaderCarrier], any[AuthContext]))
        .thenReturn(Future.successful(Some(mockCacheMap)))

      when(mockDataCacheConnector.fetch[Seq[TradingPremises]](any())
        (any(), any(), any())).thenReturn(Future.successful(Some(Seq(tradingPremises))))

      when(mockDataCacheConnector.save[Seq[TradingPremises]](any(), any())
        (any(), any(), any())).thenReturn(Future.successful(emptyCache))

      when(mockCacheMap.getEntry[Seq[TradingPremises]](any())(any())).thenReturn(Some(Seq(tradingPremises)))

      when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key)).thenReturn(Some(BusinessMatching(None, Some(businessMatchingActivities))))

      val newRequest = request.withFormUrlEncodedBody(
        "activities[0]" -> "01",
        "activities[1]" -> "02",
        "activities[2]" -> "03"
      )

      val RecordId = 1
      val result = whatDoesYourBusinessDoController.post(RecordId)(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(routes.SummaryController.get().url))
    }


    "for a Valid Request in EDIT Mode must redirect to the trading premises summary with record id" in new Fixture {

      val wdbd = WhatDoesYourBusinessDo(Set(AccountancyServices, BillPaymentServices))
      val tradingPremises = TradingPremises(None,None, None, Some(wdbd))
      val businessMatchingActivities = BusinessMatchingActivities(Set(AccountancyServices,
        BillPaymentServices, EstateAgentBusinessService))
      val mockCacheMap = mock[CacheMap]

      when(mockDataCacheConnector.fetchAll(any[HeaderCarrier], any[AuthContext]))
        .thenReturn(Future.successful(Some(mockCacheMap)))

      when(mockDataCacheConnector.fetch[Seq[TradingPremises]](any())
        (any(), any(), any())).thenReturn(Future.successful(Some(Seq(tradingPremises))))

      when(mockDataCacheConnector.save[Seq[TradingPremises]](any(), any())
        (any(), any(), any())).thenReturn(Future.successful(emptyCache))

      when(mockCacheMap.getEntry[Seq[TradingPremises]](any())(any())).thenReturn(Some(Seq(tradingPremises)))

      when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key)).thenReturn(Some(BusinessMatching(None, Some(businessMatchingActivities))))

      val newRequest = request.withFormUrlEncodedBody(
        "activities[0]" -> "01",
        "activities[1]" -> "02",
        "activities[2]" -> "03"
      )

      val recordId = 1
      val result = whatDoesYourBusinessDoController.post(recordId, true)(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(routes.SummaryController.getIndividual(recordId).url))
    }
  }
}
