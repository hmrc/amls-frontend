package controllers.tradingpremises


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

  val mockDataCacheConnector = mock[DataCacheConnector]
  val mockCacheMap = mock[CacheMap]
  val fieldElements = Array("report-name", "report-email", "report-action", "report-error")
  val recordId1 = 1


  trait Fixture extends AuthorisedFixture {
    self =>

    val whatDoesYourBusinessDoController = new WhatDoesYourBusinessDoController {
      override val dataCacheConnector = mockDataCacheConnector
      override val authConnector = self.authConnector
    }

    val businessMatchingActivitiesAll = BusinessMatchingActivities(
      Set(AccountancyServices, BillPaymentServices, EstateAgentBusinessService))
//    when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
//      .thenReturn(Some(BusinessMatching(None, Some(businessMatchingActivitiesAll))))

    val emptyCache = CacheMap("", Map.empty)
    when(mockDataCacheConnector.save[Seq[TradingPremises]](any(), any())(any(), any(), any()))
      .thenReturn(Future.successful(emptyCache))

    when(mockDataCacheConnector.fetchAll(any[HeaderCarrier], any[AuthContext]))
      .thenReturn(Future.successful(Some(mockCacheMap)))
  }


  "WhatDoesYourBusinessDoController" when {

    "get is called" must {
      "respond with OK and show the 'what does your business do' page" when {
        "there is no data - with empty form" in new Fixture {

          val tradingPremises = TradingPremises()
          val businessActivities = BusinessActivities(involvedInOther = Some(InvolvedInOtherYes("test")))

          when(mockDataCacheConnector.fetch[Seq[TradingPremises]](any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(Seq(tradingPremises))))
          when(mockCacheMap.getEntry[BusinessActivities](BusinessActivities.key))
            .thenReturn(Some(businessActivities))
          when(mockCacheMap.getEntry[Seq[TradingPremises]](any())(any()))
            .thenReturn(Some(Seq(tradingPremises)))
          when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
            .thenReturn(Some(BusinessMatching(None, Some(businessMatchingActivitiesAll))))

          val result = whatDoesYourBusinessDoController.get(recordId1)(request)

          status(result) must be(OK)

          val document: Document = Jsoup.parse(contentAsString(result))
          document.select(s"input[id=activities-01]").hasAttr("checked") must be(false)
        }

        "there is data - with form populated" in new Fixture {

          val wdbd = WhatDoesYourBusinessDo(Set(AccountancyServices, BillPaymentServices))
          val tradingPremises = TradingPremises(None, None, None,None,None, None,Some(wdbd),None)
          val businessActivities = BusinessActivities(
            involvedInOther = Some(InvolvedInOtherYes("test")),
            expectedBusinessTurnover = Some(ExpectedBusinessTurnover.Fifth))

          when(mockDataCacheConnector.fetch[Seq[TradingPremises]](any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(Seq(tradingPremises))))
          when(mockCacheMap.getEntry[BusinessActivities](BusinessActivities.key))
            .thenReturn(Some(businessActivities))
          when(mockCacheMap.getEntry[Seq[TradingPremises]](any())(any()))
            .thenReturn(Some(Seq(tradingPremises)))
          when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
            .thenReturn(Some(BusinessMatching(None, Some(businessMatchingActivitiesAll))))

          val result = whatDoesYourBusinessDoController.get(recordId1)(request)
          val document: Document = Jsoup.parse(contentAsString(result))

          status(result) must be(OK)
          document.select(s"input[id=activities-01]").hasAttr("checked") must be(true)

        }
      }

      "respond with SEE_OTHER and show the trading premises page" when {
        "there is no business activity" in new Fixture {

          val tradingPremises = TradingPremises()

          when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
            .thenReturn(None)
          when(mockDataCacheConnector.fetch[Seq[TradingPremises]](any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(Seq(tradingPremises))))

          val result = whatDoesYourBusinessDoController.get(recordId1)(request)

          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(routes.WhereAreTradingPremisesController.get(recordId1).url))
        }
      }
    }

    "post is called" must {

      "respond with BAD_REQUEST" when {
        "given an Invalid Request" in new Fixture {

          val tradingPremises = TradingPremises(None, None, None)

          when(mockDataCacheConnector.fetch[Seq[TradingPremises]](any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(Seq(tradingPremises))))
          when(mockCacheMap.getEntry[Seq[TradingPremises]](any())(any()))
            .thenReturn(Some(Seq(tradingPremises)))
          when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
            .thenReturn(Some(BusinessMatching(None, Some(businessMatchingActivitiesAll))))

          val invalidRequest = request.withFormUrlEncodedBody(
            "activities" -> ""
          )
          val result = whatDoesYourBusinessDoController.post(recordId1)(invalidRequest)

          status(result) must be(BAD_REQUEST)
        }
      }

      "respond with SEE_OTHER" when {
        "given a Valid Request with SINGLE Activity and show the summary page" in new Fixture {

          val wdbd = WhatDoesYourBusinessDo(Set(AccountancyServices))
          val tradingPremises = TradingPremises(None, None, None,None, None,None,Some(wdbd),None)
          val businessMatchingActivitiesSingle = BusinessMatchingActivities(Set(AccountancyServices))

          when(mockDataCacheConnector.fetch[Seq[TradingPremises]](any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(Seq(tradingPremises))))
          when(mockCacheMap.getEntry[Seq[TradingPremises]](any())(any()))
            .thenReturn(Some(Seq(tradingPremises)))
          when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
            .thenReturn(Some(BusinessMatching(None, Some(businessMatchingActivitiesSingle))))

          val newRequest = request.withFormUrlEncodedBody("activities[0]" -> "01")

          val result = whatDoesYourBusinessDoController.post(recordId1)(newRequest)
          status(result) must be(SEE_OTHER)
        }


        "given a Valid Request with multiple ACTIVITIES and show the summary page" in new Fixture {

          val wdbd = WhatDoesYourBusinessDo(Set(AccountancyServices, BillPaymentServices))
          val tradingPremises = TradingPremises(None, None, None, None,None,None,Some(wdbd),None)

          when(mockDataCacheConnector.fetch[Seq[TradingPremises]](any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(Seq(tradingPremises))))
          when(mockCacheMap.getEntry[Seq[TradingPremises]](any())(any()))
            .thenReturn(Some(Seq(tradingPremises)))
          when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
            .thenReturn(Some(BusinessMatching(None, Some(businessMatchingActivitiesAll))))

          val newRequest = request.withFormUrlEncodedBody(
            "activities[0]" -> "01",
            "activities[1]" -> "02",
            "activities[2]" -> "03"
          )

          val result = whatDoesYourBusinessDoController.post(recordId1)(newRequest)
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(routes.SummaryController.get().url))
        }


        "given a Valid Request in EDIT Mode and show the trading premises summary with record id" in new Fixture {

          val wdbd = WhatDoesYourBusinessDo(Set(AccountancyServices, BillPaymentServices))
          val tradingPremises = TradingPremises(None, None, None, None,None,None,Some(wdbd),None)

          when(mockDataCacheConnector.fetch[Seq[TradingPremises]](any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(Seq(tradingPremises))))
          when(mockCacheMap.getEntry[Seq[TradingPremises]](any())(any()))
            .thenReturn(Some(Seq(tradingPremises)))
          when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
            .thenReturn(Some(BusinessMatching(None, Some(businessMatchingActivitiesAll))))

          val newRequest = request.withFormUrlEncodedBody(
            "activities[0]" -> "01",
            "activities[1]" -> "02",
            "activities[2]" -> "03"
          )

          val result = whatDoesYourBusinessDoController.post(recordId1, true)(newRequest)

          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(routes.SummaryController.getIndividual(recordId1).url))
        }
      }
    }
  }

  it must {
    "use correct services" in new Fixture {
      WhatDoesYourBusinessDoController.authConnector must be(AMLSAuthConnector)
      WhatDoesYourBusinessDoController.dataCacheConnector must be(DataCacheConnector)
    }
  }
}
