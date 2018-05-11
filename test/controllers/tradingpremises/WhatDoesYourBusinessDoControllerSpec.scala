/*
 * Copyright 2018 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package controllers.tradingpremises


import config.AMLSAuthConnector
import connectors.DataCacheConnector
import models.{DateOfChange, TradingPremisesSection}
import models.businessactivities.{BusinessActivities, ExpectedBusinessTurnover, InvolvedInOtherYes}
import models.businessmatching.{BusinessActivities => BusinessMatchingActivities, _}
import models.status.{ReadyForRenewal, SubmissionDecisionApproved, SubmissionReady}
import models.tradingpremises._
import org.joda.time.LocalDate
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.ArgumentCaptor
import org.mockito.Matchers.{eq => meq, _}
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfter
import org.scalatest.mock.MockitoSugar
import utils.AmlsSpec
import play.api.test.Helpers._
import services.StatusService
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.frontend.auth.AuthContext
import utils.AuthorisedFixture

import scala.concurrent.Future
import uk.gov.hmrc.http.HeaderCarrier

class WhatDoesYourBusinessDoControllerSpec extends AmlsSpec with MockitoSugar with BeforeAndAfter {

  val mockDataCacheConnector = mock[DataCacheConnector]
  val mockCacheMap = mock[CacheMap]
  val fieldElements = Array("report-name", "report-email", "report-action", "report-error")
  val recordId1 = 1

  before {
    reset(mockDataCacheConnector)
  }

  trait Fixture extends AuthorisedFixture {
    self => val request = addToken(authRequest)

    val whatDoesYourBusinessDoController = new WhatDoesYourBusinessDoController {
      override val dataCacheConnector = mockDataCacheConnector
      override val authConnector = self.authConnector
      override val statusService = mock[StatusService]
    }

    val businessMatchingActivitiesAll = BusinessMatchingActivities(
      Set(AccountancyServices, BillPaymentServices, EstateAgentBusinessService))

    val emptyCache = CacheMap("", Map.empty)
    when(mockDataCacheConnector.save[Seq[TradingPremises]](any(), any())(any(), any(), any()))
      .thenReturn(Future.successful(emptyCache))

    when(mockDataCacheConnector.fetchAll(any[HeaderCarrier], any[AuthContext]))
      .thenReturn(Future.successful(Some(mockCacheMap)))

    when(whatDoesYourBusinessDoController.statusService.getStatus(any(), any(), any())).
      thenReturn(Future.successful(SubmissionReady))
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

      "redirect to Premises registered page" when {
        "only one activity is selected in Business Matching business activities page" in new Fixture {
          val tradingPremises = TradingPremises()

          val businessActivity = BusinessMatchingActivities(Set(AccountancyServices))

          when(mockDataCacheConnector.fetch[Seq[TradingPremises]](any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(Seq(tradingPremises))))

          when(mockCacheMap.getEntry[Seq[TradingPremises]](any())(any()))
            .thenReturn(Some(Seq(tradingPremises)))
          when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
            .thenReturn(Some(BusinessMatching(None, Some(businessActivity))))

          val result = whatDoesYourBusinessDoController.get(recordId1)(request)

          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(routes.PremisesRegisteredController.get(recordId1).url))
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

          val result = whatDoesYourBusinessDoController.post(recordId1, edit = true)(newRequest)
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(routes.SummaryController.getIndividual(recordId1).url))
        }


        "given a Valid Request with multiple ACTIVITIES and show the 'premises registered' page" in new Fixture {

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
          redirectLocation(result) must be(Some(routes.PremisesRegisteredController.get(1).url))
        }

        "given a valid request and money services were specified" must {

          "redirect to the Money Services page" in new Fixture {

            val model = WhatDoesYourBusinessDo(Set(AccountancyServices))
            val tradingPremises = TradingPremises(None, None, None, None, None, None, Some(model), None)
            val businessMatchingActivitiesSingle = BusinessMatchingActivities(Set(AccountancyServices))

            when(mockDataCacheConnector.fetch[Seq[TradingPremises]](any())(any(), any(), any()))
              .thenReturn(Future.successful(Some(Seq(tradingPremises))))
            when(mockCacheMap.getEntry[Seq[TradingPremises]](any())(any()))
              .thenReturn(Some(Seq(tradingPremises)))
            when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
              .thenReturn(Some(BusinessMatching(None, Some(businessMatchingActivitiesSingle))))

            val newRequest = request.withFormUrlEncodedBody("activities[0]" -> "01", "activities[1]" -> "05")

            val result = whatDoesYourBusinessDoController.post(recordId1, edit = true)(newRequest)
            status(result) must be(SEE_OTHER)
            redirectLocation(result) must be(Some(routes.MSBServicesController.get(recordId1, edit = true, changed = true).url))
          }

        }

        "the amendment is a variation" must {

          "redirect to the dateOfChange page when no money services have been added" in new Fixture {

            when(whatDoesYourBusinessDoController.statusService.getStatus(any(), any(), any())).
              thenReturn(Future.successful(SubmissionDecisionApproved))

            val model = WhatDoesYourBusinessDo(Set(AccountancyServices))
            val tradingPremises = TradingPremises(None, None, None, None, None, None, Some(model), None, lineId = Some(1))
            val businessMatchingActivitiesSingle = BusinessMatchingActivities(Set(AccountancyServices))

            when(mockDataCacheConnector.fetch[Seq[TradingPremises]](any())(any(), any(), any()))
              .thenReturn(Future.successful(Some(Seq(tradingPremises))))
            when(mockCacheMap.getEntry[Seq[TradingPremises]](any())(any()))
              .thenReturn(Some(Seq(tradingPremises)))
            when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
              .thenReturn(Some(BusinessMatching(None, Some(businessMatchingActivitiesSingle))))

            val newRequest = request.withFormUrlEncodedBody("activities[0]" -> "02", "activities[1]" -> "01")

            val result = whatDoesYourBusinessDoController.post(recordId1, edit = true)(newRequest)
            status(result) must be(SEE_OTHER)
            redirectLocation(result) must be(Some(routes.WhatDoesYourBusinessDoController.dateOfChange(recordId1).url))

          }

        }

        "ready for renewal status" must {

          "redirect to the dateOfChange page when no money services have been added" in new Fixture {

            when(whatDoesYourBusinessDoController.statusService.getStatus(any(), any(), any())).
              thenReturn(Future.successful(ReadyForRenewal(None)))

            val model = WhatDoesYourBusinessDo(Set(AccountancyServices))
            val tradingPremises = TradingPremises(None, None, None, None, None, None, Some(model), None, lineId = Some(1))
            val businessMatchingActivitiesSingle = BusinessMatchingActivities(Set(AccountancyServices))

            when(mockDataCacheConnector.fetch[Seq[TradingPremises]](any())(any(), any(), any()))
              .thenReturn(Future.successful(Some(Seq(tradingPremises))))
            when(mockCacheMap.getEntry[Seq[TradingPremises]](any())(any()))
              .thenReturn(Some(Seq(tradingPremises)))
            when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
              .thenReturn(Some(BusinessMatching(None, Some(businessMatchingActivitiesSingle))))

            val newRequest = request.withFormUrlEncodedBody("activities[0]" -> "02", "activities[1]" -> "01")

            val result = whatDoesYourBusinessDoController.post(recordId1, edit = true)(newRequest)
            status(result) must be(SEE_OTHER)
            redirectLocation(result) must be(Some(routes.WhatDoesYourBusinessDoController.dateOfChange(recordId1).url))

          }
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

      "set the hasChanged flag to true" in new Fixture {

        val newRequest = request.withFormUrlEncodedBody(
          "activities[0]" -> "01",
          "activities[1]" -> "02",
          "activities[2]" -> "03"
        )

        when(mockDataCacheConnector.fetch[Seq[TradingPremises]](any())(any(), any(), any()))
          .thenReturn(Future.successful(Some(Seq(TradingPremisesSection.tradingPremisesWithHasChangedFalse))))

        when(mockDataCacheConnector.save[TradingPremises](any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(emptyCache))

        when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
          .thenReturn(Some(BusinessMatching(None, Some(businessMatchingActivitiesAll))))

        val result = whatDoesYourBusinessDoController.post(1)(newRequest)

        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some(routes.PremisesRegisteredController.get(1).url))

        verify(mockDataCacheConnector).save[Seq[TradingPremises]](
          any(),
          meq(Seq(TradingPremisesSection.tradingPremisesWithHasChangedFalse.copy(
            hasChanged = true,
            whatDoesYourBusinessDoAtThisAddress = Some(WhatDoesYourBusinessDo(Set(AccountancyServices, BillPaymentServices, EstateAgentBusinessService))),
            msbServices = None
          ))))(any(), any(), any())
      }
    }

    "the dateOfChange action is called" must {

      "show the correct view" in new Fixture {
        val result = whatDoesYourBusinessDoController.dateOfChange(1)(request)
        status(result) must be(OK)
      }

    }

    "the dateOfChange action is posted to" must {

      "update the dateOfChange field in the data" in new Fixture {

        val postRequest = request.withFormUrlEncodedBody(
          "dateOfChange.year" -> "2010",
          "dateOfChange.month" -> "10",
          "dateOfChange.day" -> "01"
        )

        val data = WhatDoesYourBusinessDo(Set(AccountancyServices))
        val expectedData = WhatDoesYourBusinessDo(data.activities, dateOfChange = Some(DateOfChange(new LocalDate(2010, 10, 1))))

        val yourPremises = mock[YourTradingPremises]
        when(yourPremises.startDate) thenReturn Some(new LocalDate(2005, 1, 1))

        val premises = TradingPremises(yourTradingPremises = Some(yourPremises), whatDoesYourBusinessDoAtThisAddress = Some(data))

        when(whatDoesYourBusinessDoController.dataCacheConnector.fetch[Seq[TradingPremises]](meq(TradingPremises.key))(any(), any(), any()))
          .thenReturn(Future.successful(Some(Seq(premises))))

        when(mockDataCacheConnector.fetch[Seq[TradingPremises]](any())
          (any(), any(), any())).thenReturn(Future.successful(Some(Seq(premises))))

        when(whatDoesYourBusinessDoController.dataCacheConnector.save[TradingPremises](meq(TradingPremises.key), any[TradingPremises])(any(), any(), any())).
          thenReturn(Future.successful(mock[CacheMap]))

        val result = whatDoesYourBusinessDoController.saveDateOfChange(1)(postRequest)

        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some(routes.SummaryController.get().url))

        val captor = ArgumentCaptor.forClass(classOf[Seq[TradingPremises]])
        verify(whatDoesYourBusinessDoController.dataCacheConnector).save[Seq[TradingPremises]](meq(TradingPremises.key), captor.capture())(any(), any(), any())

        captor.getValue.head.whatDoesYourBusinessDoAtThisAddress match {
          case Some(x) => x must be(expectedData)
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
