/*
 * Copyright 2024 HM Revenue & Customs
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

import connectors.DataCacheConnector
import controllers.actions.SuccessfulAuthAction
import forms.DateOfChangeFormProvider
import forms.tradingpremises.WhatDoesYourBusinessDoFormProvider
import models.businessactivities.{BusinessActivities, ExpectedBusinessTurnover, InvolvedInOtherYes}
import models.businessmatching.BusinessActivity._
import models.businessmatching.{BusinessActivities => BusinessMatchingActivities, _}
import models.status.{ReadyForRenewal, SubmissionDecisionApproved, SubmissionReady}
import models.tradingpremises._
import models.{DateOfChange, TradingPremisesSection}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.{eq => meq, _}
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfter
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Injecting}
import services.StatusService
import services.cache.Cache
import uk.gov.hmrc.auth.core.AffinityGroup.Individual
import uk.gov.hmrc.auth.core.{Enrolments, User}
import utils.{AmlsSpec, AuthorisedRequest, DateHelper}
import views.html.DateOfChangeView
import views.html.tradingpremises.WhatDoesYourBusinessDoView

import java.time.LocalDate
import scala.concurrent.Future

class WhatDoesYourBusinessDoControllerSpec extends AmlsSpec with MockitoSugar with BeforeAndAfter with Injecting {

  val mockDataCacheConnector = mock[DataCacheConnector]
  val mockCacheMap           = mock[Cache]
  val fieldElements          = Array("report-name", "report-email", "report-action", "report-error")
  val recordId1              = 1

  before {
    reset(mockDataCacheConnector)
  }

  trait Fixture {
    self =>
    val request    = addToken(authRequest)
    lazy val view1 = inject[WhatDoesYourBusinessDoView]
    lazy val view2 = inject[DateOfChangeView]

    val whatDoesYourBusinessDoController = new WhatDoesYourBusinessDoController(
      dataCacheConnector = mockDataCacheConnector,
      authAction = SuccessfulAuthAction,
      ds = commonDependencies,
      statusService = mock[StatusService],
      cc = mockMcc,
      formProvider = inject[WhatDoesYourBusinessDoFormProvider],
      dateChangeFormProvider = inject[DateOfChangeFormProvider],
      activitiesView = view1,
      dateChangeView = view2,
      error = errorView
    )

    val businessMatchingActivitiesAll = BusinessMatchingActivities(
      Set(AccountancyServices, BillPaymentServices, EstateAgentBusinessService)
    )

    val emptyCache = Cache.empty
    when(mockDataCacheConnector.save[Seq[TradingPremises]](any(), any(), any())(any()))
      .thenReturn(Future.successful(emptyCache))

    when(mockDataCacheConnector.fetchAll(any()))
      .thenReturn(Future.successful(Some(mockCacheMap)))

    when(
      whatDoesYourBusinessDoController.statusService
        .getStatus(any[Option[String]](), any[(String, String)](), any[String]())(any(), any(), any())
    ).thenReturn(Future.successful(SubmissionReady))
  }

  "WhatDoesYourBusinessDoController" when {

    "get is called" must {
      "respond with OK and show the 'what does your business do' page" when {
        "there is no data - with empty form" in new Fixture {

          val tradingPremises    = TradingPremises()
          val businessActivities = BusinessActivities(involvedInOther = Some(InvolvedInOtherYes("test")))

          when(mockDataCacheConnector.fetch[Seq[TradingPremises]](any(), any())(any()))
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
          document.getElementById("value_1").hasAttr("checked") must be(false)
        }

        "there is data - with form populated" in new Fixture {

          val wdbd               = WhatDoesYourBusinessDo(Set(AccountancyServices, BillPaymentServices))
          val tradingPremises    = TradingPremises(None, None, None, None, None, None, Some(wdbd), None)
          val businessActivities = BusinessActivities(
            involvedInOther = Some(InvolvedInOtherYes("test")),
            expectedBusinessTurnover = Some(ExpectedBusinessTurnover.Fifth)
          )

          when(mockDataCacheConnector.fetch[Seq[TradingPremises]](any(), any())(any()))
            .thenReturn(Future.successful(Some(Seq(tradingPremises))))
          when(mockCacheMap.getEntry[BusinessActivities](BusinessActivities.key))
            .thenReturn(Some(businessActivities))
          when(mockCacheMap.getEntry[Seq[TradingPremises]](any())(any()))
            .thenReturn(Some(Seq(tradingPremises)))
          when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
            .thenReturn(Some(BusinessMatching(None, Some(businessMatchingActivitiesAll))))

          val result             = whatDoesYourBusinessDoController.get(recordId1)(request)
          val document: Document = Jsoup.parse(contentAsString(result))

          status(result) must be(OK)

          document.getElementById("value_1").hasAttr("checked") must be(true)
          document.getElementById("value_3").hasAttr("checked") must be(true)
        }
      }

      "redirect to not found page" when {
        "whatDoesYourBusinessDoAtThisAddress can not be persisted" when {
          "only one activity is selected in Business Matching business activities page" in new Fixture {

            val tradingPremises = TradingPremises()

            val businessActivity = BusinessMatchingActivities(Set(MoneyServiceBusiness))

            when(mockDataCacheConnector.fetch[Seq[TradingPremises]](any(), any())(any()))
              .thenReturn(Future.successful(None))

            when(mockCacheMap.getEntry[Seq[TradingPremises]](any())(any()))
              .thenReturn(Some(Seq(tradingPremises)))

            when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
              .thenReturn(Some(BusinessMatching(None, Some(businessActivity))))

            val result = whatDoesYourBusinessDoController.get(recordId1)(request)

            status(result)           must be(NOT_FOUND)
            redirectLocation(result) must be(None)
          }
        }
      }

      "redirect to MSB Services page" when {
        "activity is MoneyServiceBusiness" when {
          "only one activity is selected in Business Matching business activities page" in new Fixture {
            val tradingPremises = TradingPremises()

            val businessActivity = BusinessMatchingActivities(Set(MoneyServiceBusiness))

            when(mockDataCacheConnector.fetch[Seq[TradingPremises]](any(), any())(any()))
              .thenReturn(Future.successful(Some(Seq(tradingPremises))))

            when(mockCacheMap.getEntry[Seq[TradingPremises]](any())(any()))
              .thenReturn(Some(Seq(tradingPremises)))

            when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
              .thenReturn(Some(BusinessMatching(None, Some(businessActivity))))

            val result = whatDoesYourBusinessDoController.get(recordId1)(request)

            status(result)           must be(SEE_OTHER)
            redirectLocation(result) must be(Some(routes.MSBServicesController.get(recordId1).url))
          }
        }
      }

      "redirect to Check Your Answers Page page" when {
        "only one activity is selected in Business Matching business activities page" in new Fixture {
          val tradingPremises = TradingPremises()

          val businessActivity = BusinessMatchingActivities(Set(AccountancyServices))

          when(mockDataCacheConnector.fetch[Seq[TradingPremises]](any(), any())(any()))
            .thenReturn(Future.successful(Some(Seq(tradingPremises))))

          when(mockCacheMap.getEntry[Seq[TradingPremises]](any())(any()))
            .thenReturn(Some(Seq(tradingPremises)))
          when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
            .thenReturn(Some(BusinessMatching(None, Some(businessActivity))))

          val result = whatDoesYourBusinessDoController.get(recordId1, true)(request)

          status(result)           must be(SEE_OTHER)
          redirectLocation(result) must be(Some(routes.CheckYourAnswersController.get(recordId1).url))
        }
      }

      "respond with SEE_OTHER and show the trading premises page" when {
        "there is no business activity" in new Fixture {

          val tradingPremises = TradingPremises()

          when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
            .thenReturn(None)
          when(mockDataCacheConnector.fetch[Seq[TradingPremises]](any(), any())(any()))
            .thenReturn(Future.successful(Some(Seq(tradingPremises))))

          val result = whatDoesYourBusinessDoController.get(recordId1)(request)

          status(result)           must be(SEE_OTHER)
          redirectLocation(result) must be(Some(routes.WhereAreTradingPremisesController.get(recordId1).url))
        }
      }
    }

    "post is called" must {

      "respond with BAD_REQUEST" when {
        "given an Invalid Request" in new Fixture {

          val tradingPremises = TradingPremises(None, None, None)

          when(mockDataCacheConnector.fetch[Seq[TradingPremises]](any(), any())(any()))
            .thenReturn(Future.successful(Some(Seq(tradingPremises))))
          when(mockCacheMap.getEntry[Seq[TradingPremises]](any())(any()))
            .thenReturn(Some(Seq(tradingPremises)))
          when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
            .thenReturn(Some(BusinessMatching(None, Some(businessMatchingActivitiesAll))))

          val invalidRequest = FakeRequest(POST, routes.WhatDoesYourBusinessDoController.post(1, false).url)
            .withFormUrlEncodedBody(
              "value[1]" -> ""
            )
          val result         = whatDoesYourBusinessDoController.post(recordId1)(invalidRequest)

          status(result) must be(BAD_REQUEST)
        }
      }

      "respond with SEE_OTHER" when {
        "given a Valid Request with SINGLE Activity and show the check your answers page" in new Fixture {

          val wdbd                             = WhatDoesYourBusinessDo(Set(AccountancyServices))
          val tradingPremises                  = TradingPremises(None, None, None, None, None, None, Some(wdbd), None)
          val businessMatchingActivitiesSingle = BusinessMatchingActivities(Set(AccountancyServices))

          when(mockDataCacheConnector.fetch[Seq[TradingPremises]](any(), any())(any()))
            .thenReturn(Future.successful(Some(Seq(tradingPremises))))
          when(mockCacheMap.getEntry[Seq[TradingPremises]](any())(any()))
            .thenReturn(Some(Seq(tradingPremises)))
          when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
            .thenReturn(Some(BusinessMatching(None, Some(businessMatchingActivitiesSingle))))

          val newRequest = FakeRequest(POST, routes.WhatDoesYourBusinessDoController.post(1, true).url)
            .withFormUrlEncodedBody("value[1]" -> AccountancyServices.toString)

          val result = whatDoesYourBusinessDoController.post(recordId1, edit = true)(newRequest)
          status(result)           must be(SEE_OTHER)
          redirectLocation(result) must be(Some(routes.CheckYourAnswersController.get(recordId1).url))
        }

        "given a Valid Request with multiple ACTIVITIES and show the 'Check Your Answers' page" in new Fixture {

          val wdbd            = WhatDoesYourBusinessDo(Set(AccountancyServices, BillPaymentServices))
          val tradingPremises = TradingPremises(None, None, None, None, None, None, Some(wdbd), None)

          when(mockDataCacheConnector.fetch[Seq[TradingPremises]](any(), any())(any()))
            .thenReturn(Future.successful(Some(Seq(tradingPremises))))
          when(mockCacheMap.getEntry[Seq[TradingPremises]](any())(any()))
            .thenReturn(Some(Seq(tradingPremises)))
          when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
            .thenReturn(Some(BusinessMatching(None, Some(businessMatchingActivitiesAll))))

          val newRequest = FakeRequest(POST, routes.WhatDoesYourBusinessDoController.post(1, false).url)
            .withFormUrlEncodedBody(
              "value[1]" -> AccountancyServices.toString,
              "value[2]" -> ArtMarketParticipant.toString,
              "value[3]" -> BillPaymentServices.toString
            )

          val result = whatDoesYourBusinessDoController.post(recordId1)(newRequest)
          status(result)           must be(SEE_OTHER)
          redirectLocation(result) must be(Some(routes.CheckYourAnswersController.get(recordId1).url))
        }

        "given a valid request and money services were specified" must {

          "redirect to the Money Services page" in new Fixture {

            val model                            = WhatDoesYourBusinessDo(Set(AccountancyServices))
            val tradingPremises                  = TradingPremises(None, None, None, None, None, None, Some(model), None)
            val businessMatchingActivitiesSingle = BusinessMatchingActivities(Set(AccountancyServices))

            when(mockDataCacheConnector.fetch[Seq[TradingPremises]](any(), any())(any()))
              .thenReturn(Future.successful(Some(Seq(tradingPremises))))
            when(mockCacheMap.getEntry[Seq[TradingPremises]](any())(any()))
              .thenReturn(Some(Seq(tradingPremises)))
            when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
              .thenReturn(Some(BusinessMatching(None, Some(businessMatchingActivitiesSingle))))

            val newRequest = FakeRequest(POST, routes.WhatDoesYourBusinessDoController.post(1, true).url)
              .withFormUrlEncodedBody(
                "value[1]" -> AccountancyServices.toString,
                "value[2]" -> MoneyServiceBusiness.toString
              )

            val result = whatDoesYourBusinessDoController.post(recordId1, edit = true)(newRequest)
            status(result)           must be(SEE_OTHER)
            redirectLocation(result) must be(
              Some(routes.MSBServicesController.get(recordId1, edit = true, changed = true).url)
            )
          }

        }

        "the amendment is a variation" must {

          "redirect to the dateOfChange page when no money services have been added" in new Fixture {

            when(
              whatDoesYourBusinessDoController.statusService
                .getStatus(any[Option[String]](), any[(String, String)](), any[String]())(any(), any(), any())
            ).thenReturn(Future.successful(SubmissionDecisionApproved))

            val model                            = WhatDoesYourBusinessDo(Set(AccountancyServices))
            val tradingPremises                  =
              TradingPremises(None, None, None, None, None, None, Some(model), None, lineId = Some(1))
            val businessMatchingActivitiesSingle = BusinessMatchingActivities(Set(AccountancyServices))

            when(mockDataCacheConnector.fetch[Seq[TradingPremises]](any(), any())(any()))
              .thenReturn(Future.successful(Some(Seq(tradingPremises))))
            when(mockCacheMap.getEntry[Seq[TradingPremises]](any())(any()))
              .thenReturn(Some(Seq(tradingPremises)))
            when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
              .thenReturn(Some(BusinessMatching(None, Some(businessMatchingActivitiesSingle))))

            val newRequest = FakeRequest(POST, routes.WhatDoesYourBusinessDoController.post(1, true).url)
              .withFormUrlEncodedBody(
                "value[1]" -> ArtMarketParticipant.toString,
                "value[2]" -> AccountancyServices.toString
              )

            val result = whatDoesYourBusinessDoController.post(recordId1, edit = true)(newRequest)
            status(result)           must be(SEE_OTHER)
            redirectLocation(result) must be(Some(routes.WhatDoesYourBusinessDoController.dateOfChange(recordId1).url))

          }

        }

        "ready for renewal status" must {

          "redirect to the dateOfChange page when no money services have been added" in new Fixture {

            when(
              whatDoesYourBusinessDoController.statusService
                .getStatus(any[Option[String]](), any[(String, String)](), any[String]())(any(), any(), any())
            ).thenReturn(Future.successful(ReadyForRenewal(None)))

            val model                            = WhatDoesYourBusinessDo(Set(AccountancyServices))
            val tradingPremises                  =
              TradingPremises(None, None, None, None, None, None, Some(model), None, lineId = Some(1))
            val businessMatchingActivitiesSingle = BusinessMatchingActivities(Set(AccountancyServices))

            when(mockDataCacheConnector.fetch[Seq[TradingPremises]](any(), any())(any()))
              .thenReturn(Future.successful(Some(Seq(tradingPremises))))
            when(mockCacheMap.getEntry[Seq[TradingPremises]](any())(any()))
              .thenReturn(Some(Seq(tradingPremises)))
            when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
              .thenReturn(Some(BusinessMatching(None, Some(businessMatchingActivitiesSingle))))

            val newRequest = FakeRequest(POST, routes.WhatDoesYourBusinessDoController.post(1, true).url)
              .withFormUrlEncodedBody(
                "value[1]" -> ArtMarketParticipant.toString,
                "value[2]" -> AccountancyServices.toString
              )

            val result = whatDoesYourBusinessDoController.post(recordId1, edit = true)(newRequest)
            status(result)           must be(SEE_OTHER)
            redirectLocation(result) must be(Some(routes.WhatDoesYourBusinessDoController.dateOfChange(recordId1).url))

          }
        }

        "given a Valid Request in EDIT Mode and show the trading premises check your answers with record id" in new Fixture {

          val wdbd            = WhatDoesYourBusinessDo(Set(AccountancyServices, BillPaymentServices))
          val tradingPremises = TradingPremises(None, None, None, None, None, None, Some(wdbd), None)

          when(mockDataCacheConnector.fetch[Seq[TradingPremises]](any(), any())(any()))
            .thenReturn(Future.successful(Some(Seq(tradingPremises))))
          when(mockCacheMap.getEntry[Seq[TradingPremises]](any())(any()))
            .thenReturn(Some(Seq(tradingPremises)))
          when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
            .thenReturn(Some(BusinessMatching(None, Some(businessMatchingActivitiesAll))))

          val newRequest = FakeRequest(POST, routes.WhatDoesYourBusinessDoController.post(1, true).url)
            .withFormUrlEncodedBody(
              "value[1]" -> AccountancyServices.toString,
              "value[2]" -> ArtMarketParticipant.toString,
              "value[3]" -> BillPaymentServices.toString
            )

          val result = whatDoesYourBusinessDoController.post(recordId1, true)(newRequest)

          status(result)           must be(SEE_OTHER)
          redirectLocation(result) must be(Some(routes.CheckYourAnswersController.get(recordId1).url))
        }
      }

      "set the hasChanged flag to true" in new Fixture {

        val newRequest = FakeRequest(POST, routes.WhatDoesYourBusinessDoController.post(1, false).url)
          .withFormUrlEncodedBody(
            "value[1]" -> AccountancyServices.toString,
            "value[2]" -> BillPaymentServices.toString,
            "value[3]" -> EstateAgentBusinessService.toString
          )

        when(mockDataCacheConnector.fetch[Seq[TradingPremises]](any(), any())(any()))
          .thenReturn(Future.successful(Some(Seq(TradingPremisesSection.tradingPremisesWithHasChangedFalse))))

        when(mockDataCacheConnector.save[TradingPremises](any(), any(), any())(any()))
          .thenReturn(Future.successful(emptyCache))

        when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
          .thenReturn(Some(BusinessMatching(None, Some(businessMatchingActivitiesAll))))

        val result = whatDoesYourBusinessDoController.post(1)(newRequest)

        status(result)           must be(SEE_OTHER)
        redirectLocation(result) must be(Some(routes.CheckYourAnswersController.get(1).url))

        verify(mockDataCacheConnector).save[Seq[TradingPremises]](
          any(),
          any(),
          meq(
            Seq(
              TradingPremisesSection.tradingPremisesWithHasChangedFalse.copy(
                hasChanged = true,
                whatDoesYourBusinessDoAtThisAddress = Some(
                  WhatDoesYourBusinessDo(Set(AccountancyServices, BillPaymentServices, EstateAgentBusinessService))
                ),
                msbServices = None
              )
            )
          )
        )(any())
      }
    }

    "the dateOfChange action is called" must {

      "show the correct view" in new Fixture {
        val authorisedRequest = AuthorisedRequest(
          request,
          Some("REF"),
          "CREDID",
          Individual,
          Enrolments(Set()),
          ("TYPE", "ID"),
          Some("GROUPID"),
          Some(User)
        )
        val result            = whatDoesYourBusinessDoController.dateOfChange(1)(authorisedRequest)
        status(result) must be(OK)
      }

    }

    "the dateOfChange action is posted to" must {

      "return 400 when given date is before start date" in new Fixture {

        val postRequest = FakeRequest(POST, routes.WhatDoesYourBusinessDoController.saveDateOfChange(1).url)
          .withFormUrlEncodedBody(
            "dateOfChange.year"  -> "2010",
            "dateOfChange.month" -> "10",
            "dateOfChange.day"   -> "01"
          )

        val date = LocalDate.of(2011, 10, 1)

        val authorisedRequest = AuthorisedRequest(
          postRequest,
          Some("REF"),
          "CREDID",
          Individual,
          Enrolments(Set()),
          ("TYPE", "ID"),
          Some("GROUPID"),
          Some(User)
        )

        val data = WhatDoesYourBusinessDo(Set(AccountancyServices))

        val yourPremises = mock[YourTradingPremises]
        when(yourPremises.startDate) thenReturn Some(date)

        val premises =
          TradingPremises(yourTradingPremises = Some(yourPremises), whatDoesYourBusinessDoAtThisAddress = Some(data))

        when(
          whatDoesYourBusinessDoController.dataCacheConnector
            .fetch[Seq[TradingPremises]](any(), meq(TradingPremises.key))(any())
        )
          .thenReturn(Future.successful(Some(Seq(premises))))

        when(mockDataCacheConnector.fetch[Seq[TradingPremises]](any(), any())(any()))
          .thenReturn(Future.successful(Some(Seq(premises))))

        val result = whatDoesYourBusinessDoController.saveDateOfChange(1)(authorisedRequest)

        status(result)          must be(BAD_REQUEST)
        contentAsString(result) must include(
          messages("error.expected.tp.dateofchange.after.startdate", DateHelper.formatDate(date))
        )
      }

      "update the dateOfChange field in the data" in new Fixture {

        val postRequest = FakeRequest(POST, routes.WhatDoesYourBusinessDoController.saveDateOfChange(1).url)
          .withFormUrlEncodedBody(
            "dateOfChange.year"  -> "2010",
            "dateOfChange.month" -> "10",
            "dateOfChange.day"   -> "01"
          )

        val authorisedRequest = AuthorisedRequest(
          postRequest,
          Some("REF"),
          "CREDID",
          Individual,
          Enrolments(Set()),
          ("TYPE", "ID"),
          Some("GROUPID"),
          Some(User)
        )

        val data         = WhatDoesYourBusinessDo(Set(AccountancyServices))
        val expectedData =
          WhatDoesYourBusinessDo(data.activities, dateOfChange = Some(DateOfChange(LocalDate.of(2010, 10, 1))))

        val yourPremises = mock[YourTradingPremises]
        when(yourPremises.startDate) thenReturn Some(LocalDate.of(2005, 1, 1))

        val premises =
          TradingPremises(yourTradingPremises = Some(yourPremises), whatDoesYourBusinessDoAtThisAddress = Some(data))

        when(
          whatDoesYourBusinessDoController.dataCacheConnector
            .fetch[Seq[TradingPremises]](any(), meq(TradingPremises.key))(any())
        )
          .thenReturn(Future.successful(Some(Seq(premises))))

        when(mockDataCacheConnector.fetch[Seq[TradingPremises]](any(), any())(any()))
          .thenReturn(Future.successful(Some(Seq(premises))))

        when(
          whatDoesYourBusinessDoController.dataCacheConnector
            .save[TradingPremises](any(), meq(TradingPremises.key), any[TradingPremises])(any())
        ).thenReturn(Future.successful(mock[Cache]))

        val result = whatDoesYourBusinessDoController.saveDateOfChange(1)(authorisedRequest)

        status(result)           must be(SEE_OTHER)
        redirectLocation(result) must be(Some(routes.CheckYourAnswersController.get(1).url))

        val captor = ArgumentCaptor.forClass(classOf[Seq[TradingPremises]])
        verify(whatDoesYourBusinessDoController.dataCacheConnector)
          .save[Seq[TradingPremises]](any(), meq(TradingPremises.key), captor.capture())(any())

        captor.getValue.head.whatDoesYourBusinessDoAtThisAddress match {
          case Some(x) => x must be(expectedData)
        }

      }

    }

  }
}
