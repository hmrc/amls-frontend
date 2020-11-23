/*
 * Copyright 2020 HM Revenue & Customs
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
import models._
import models.status.{ReadyForRenewal, SubmissionDecisionApproved, SubmissionDecisionRejected}
import models.tradingpremises._
import org.joda.time.LocalDate
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import org.jsoup.select.Elements
import org.mockito.ArgumentCaptor
import org.mockito.Matchers.{eq => meq, _}
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfter
import org.scalatest.mockito.MockitoSugar
import play.api.i18n.Messages
import play.api.test.Helpers.{status => hstatus, _}
import services.StatusService
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.http.connector.AuditResult.Success
import uk.gov.hmrc.play.audit.model.DataEvent
import utils.AmlsSpec
import views.html.date_of_change
import views.html.tradingpremises.where_are_trading_premises

import scala.collection.JavaConversions._
import scala.concurrent.{ExecutionContext, Future}

class WhereAreTradingPremisesControllerSpec extends AmlsSpec with MockitoSugar with BeforeAndAfter {

  private val mockDataCacheConnector = mock[DataCacheConnector]

  trait Fixture  {
    self => val request = addToken(authRequest)
    lazy val view1 = app.injector.instanceOf[where_are_trading_premises]
    lazy val view2 = app.injector.instanceOf[date_of_change]

    val controller = new WhereAreTradingPremisesController (
      dataCacheConnector = mockDataCacheConnector,
      authAction = SuccessfulAuthAction, ds = commonDependencies,
      statusService = mock[StatusService],
      auditConnector = mock[AuditConnector],
      cc = mockMcc,
      where_are_trading_premises = view1,
      date_of_change = view2,
      error = errorView
      )

    when(controller.statusService.getStatus(any[Option[String]](), any[(String, String)](), any[String]())(any(), any())).thenReturn(Future.successful(SubmissionDecisionRejected))

    when {
      controller.auditConnector.sendEvent(any())(any(), any())
    } thenReturn Future.successful(Success)
  }

  before {
    reset(mockDataCacheConnector)
  }

  val emptyCache = CacheMap("", Map.empty)
  val fields = Array[String]("tradingName", "addressLine1", "addressLine2", "postcode")
  val RecordId1 = 1


  "WhereAreTradingPremisesController" when {

    "get is called" must {
      "respond with OK and show the form with data when there is data" in new Fixture {

        val address = Address("addressLine1", "addressLine2", None, None, "AA1 1AA")
        val yourTradingPremises = YourTradingPremises(tradingName = "trading Name", address, Some(true), Some(LocalDate.now()))
        val tradingPremises = TradingPremises(None, Some(yourTradingPremises), None, None)

        when(mockDataCacheConnector.fetch[Seq[TradingPremises]](any(), any())(any(), any()))
          .thenReturn(Future.successful(Some(Seq(tradingPremises))))

        val result = controller.get(RecordId1, true)(request)
        val document = Jsoup.parse(contentAsString(result))

        hstatus(result) must be(OK)
        contentAsString(result) must include(Messages("tradingpremises.yourtradingpremises.title"))
        for (field <- fields)
          document.select(s"input[id=$field]").`val`() must not be empty
      }

      "respond with OK and show the empty form when there is no data" in new Fixture {

        when(mockDataCacheConnector.fetch[Seq[TradingPremises]](any(), any())(any(), any()))
          .thenReturn(Future.successful(Some(Seq(TradingPremises()))))

        val result = controller.get(RecordId1, false)(request)
        val document = Jsoup.parse(contentAsString(result))

        hstatus(result) must be(OK)

      }

      "respond with NOT_FOUND when there is no data at all at the given index" in new Fixture {

        when(mockDataCacheConnector.fetch[Seq[TradingPremises]](any(), any())
          (any(), any())).thenReturn(Future.successful(None))

        val result = controller.get(RecordId1, false)(request)

        hstatus(result) must be(NOT_FOUND)
      }
    }

    "post is called" must {

      val ytp = YourTradingPremises(
        "foo",
        Address(
          "1",
          "2",
          None,
          None,
          "AA11 1AA"
        ),
        Some(true),
        Some(new LocalDate(1990, 2, 24))
      )

      "respond with SEE_OTHER" when {
        "edit mode is false, and redirect to the 'Activity Start Date' page" in new Fixture {

          val newRequest = requestWithUrlEncodedBody(
            "tradingName" -> "Trading Name",
            "addressLine1" -> "Address 1",
            "addressLine2" -> "Address 2",
            "postcode" -> "AA1 1AA"
          )

          when(controller.dataCacheConnector.fetch[Seq[TradingPremises]](any(), any())(any(), any()))
            .thenReturn(Future.successful(Some(Seq(TradingPremises(yourTradingPremises = Some(ytp))))))

          val updatedYtp = ytp.copy(tradingName = "Trading Name", tradingPremisesAddress = Address("Address 1", "Address 2", None, None, "AA1 1AA"))

          when(controller.dataCacheConnector.save[Seq[TradingPremises]](any(), any(),
            meq(Seq(TradingPremises(yourTradingPremises = Some(updatedYtp), hasChanged = true))))(any(), any()))
            .thenReturn(Future.successful(emptyCache))

          val result = controller.post(RecordId1, false)(newRequest)

          hstatus(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(controllers.tradingpremises.routes.ActivityStartDateController.get(1).url))

          val captor = ArgumentCaptor.forClass(classOf[DataEvent])
          verify(controller.auditConnector).sendEvent(captor.capture())(any(), any())

          captor.getValue match {
            case d: DataEvent =>
              d.detail("addressLine1") mustBe "Address 1"
              d.detail("addressLine2") mustBe "Address 2"
              d.detail("postCode") mustBe "AA1 1AA"
          }
        }
        
        "fail submission on invalid uk address" in new Fixture {

          val newRequest = requestWithUrlEncodedBody(
            "tradingName" -> "Trading Name",
            "addressLine1" -> "Address **1",
            "addressLine2" -> "Address 2",
            "addressLine3" -> "Address 3",
            "addressLine4" -> "Address 4",
            "postcode" -> "AA1 1AA"
          )

          when(controller.dataCacheConnector.fetch[Seq[TradingPremises]](any(), any())(any(), any()))
            .thenReturn(Future.successful(Some(Seq(TradingPremises()))))

          when(controller.dataCacheConnector.save[TradingPremises](any(), any(), any())(any(), any()))
            .thenReturn(Future.successful(emptyCache))

          val result = controller.post(RecordId1, false)(newRequest)

          val document: Document  = Jsoup.parse(contentAsString(result))
          val errorCount = 1
          val elementsWithError : Elements = document.getElementsByClass("error-notification")
          elementsWithError.size() must be(errorCount)
          for (ele: Element <- elementsWithError) {
            ele.html() must include(Messages("error.required.enter.addresslineone.regex"))
          }
        }

        "redirect to the 'Activity Start Date' page when no data in mongoCache" in new Fixture {

          val newRequest = requestWithUrlEncodedBody(
            "tradingName" -> "Trading Name",
            "addressLine1" -> "Address 1",
            "addressLine2" -> "Address 2",
            "postcode" -> "AA1 1AA"
          )
          val newYtp = Some(YourTradingPremises(tradingName = "Trading Name",
            tradingPremisesAddress = Address("Address 1", "Address 2", None, None, "AA1 1AA")))

          when(controller.dataCacheConnector.fetch[Seq[TradingPremises]](any(), any())(any(), any()))
            .thenReturn(Future.successful(Some(Seq(TradingPremises()))))

          when(controller.dataCacheConnector.save[Seq[TradingPremises]](any(), any(), meq(Seq(TradingPremises(yourTradingPremises = newYtp,  hasChanged = true))))(any(), any()))
            .thenReturn(Future.successful(emptyCache))


          val result = controller.post(RecordId1, false)(newRequest)

          hstatus(result) must be(SEE_OTHER)
          redirectLocation(result) must be(
            Some(controllers.tradingpremises.routes.ActivityStartDateController.get(1, false).url))
        }

        "edit mode is true, and redirect to check your answer page" in new Fixture {

          val newRequest = requestWithUrlEncodedBody(
            "tradingName" -> "Trading Name",
            "addressLine1" -> "Address 1",
            "addressLine2" -> "Address 2",
            "postcode" -> "AA1 1AA"
          )

          val oldAddress = Address("Old address 1", "Old address 2", None, None, "Test")

          when(controller.dataCacheConnector.fetch[Seq[TradingPremises]](any(), any())(any(), any()))
            .thenReturn(Future.successful(Some(Seq(TradingPremises(yourTradingPremises = Some(YourTradingPremises("Test", oldAddress)))))))

          when(controller.dataCacheConnector.save[Seq[TradingPremises]](any(), any(), any())(any(), any()))
            .thenReturn(Future.successful(emptyCache))

          val result = controller.post(RecordId1, true)(newRequest)

          hstatus(result) must be(SEE_OTHER)
          redirectLocation(result) must be(
            Some(controllers.tradingpremises.routes.DetailedAnswersController.get(1).url))

          val captor = ArgumentCaptor.forClass(classOf[DataEvent])
          verify(controller.auditConnector).sendEvent(captor.capture())(any(), any())

          captor.getValue match {
            case d: DataEvent =>
              d.detail("addressLine1") mustBe "Address 1"
              d.detail("addressLine2") mustBe "Address 2"
              d.detail("postCode") mustBe "AA1 1AA"
              d.detail("originalLine1") mustBe "Old address 1"
              d.detail("originalLine2") mustBe "Old address 2"
          }
        }
      }

      "respond with BAD_REQUEST" when {
        "given an invalid request" in new Fixture {
          val newRequest = requestWithUrlEncodedBody(
            "tradingName" -> "Trading Name"
          )
          when(controller.dataCacheConnector.fetch[Seq[TradingPremises]](any(), any())(any(), any()))
            .thenReturn(Future.successful(Some(Seq(TradingPremises()))))

          when(controller.dataCacheConnector.save[TradingPremises](any(), any(), any())(any(), any()))
            .thenReturn(Future.successful(emptyCache))

          val result = controller.post(RecordId1, true)(newRequest)

          hstatus(result) must be(BAD_REQUEST)

        }

        "date contains an invalid year too great in length" in new Fixture {

          val newRequest = requestWithUrlEncodedBody(
            "tradingName" -> "Trading Name",
            "addressLine1" -> "Address 1"*120,
            "addressLine2" -> "Address 2",
            "postcode" -> "AA1 1AA"
          )

          when(controller.dataCacheConnector.fetch[Seq[TradingPremises]](any(), any())(any(), any()))
            .thenReturn(Future.successful(Some(Seq(TradingPremises()))))

          when(controller.dataCacheConnector.save[TradingPremises](any(), any(), any())(any(), any()))
            .thenReturn(Future.successful(emptyCache))

          val result = controller.post(RecordId1, false)(newRequest)

          hstatus(result) must be(BAD_REQUEST)
          contentAsString(result) must include(Messages("error.required.enter.addresslineone.charcount"))

        }
      }

      "respond with NOT_FOUND" when {
        "the given index is out of bounds" in new Fixture {

          val newRequest = requestWithUrlEncodedBody(
            "tradingName" -> "Trading Name",
            "addressLine1" -> "Address 1",
            "addressLine2" -> "Address 2",
            "postcode" -> "AA1 1AA"
          )

          when(controller.dataCacheConnector.fetch[Seq[TradingPremises]](any(), any())(any(), any()))
            .thenReturn(Future.successful(None))

          when(controller.dataCacheConnector.save[TradingPremises](any(), any(), any())(any(), any()))
            .thenReturn(Future.successful(emptyCache))


          val result = controller.post(3, false)(newRequest)

          hstatus(result) must be(NOT_FOUND)
        }
      }

      "set the hasChanged flag to true" in new Fixture {

        val newRequest = requestWithUrlEncodedBody(
          "tradingName" -> "Trading Name",
          "addressLine1" -> "Address 1",
          "addressLine2" -> "Address 2",
          "postcode" -> "AA1 1AA"
        )

        when(controller.dataCacheConnector.fetch[Seq[TradingPremises]](any(), any())(any(), any()))
          .thenReturn(Future.successful(Some(Seq(TradingPremisesSection.tradingPremisesWithHasChangedFalse))))

        when(controller.dataCacheConnector.save[TradingPremises](any(), any(), any())(any(), any()))
          .thenReturn(Future.successful(emptyCache))

        val result = controller.post(1)(newRequest)

        hstatus(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some(routes.ActivityStartDateController.get(1, false).url))

        verify(controller.dataCacheConnector).save[Seq[TradingPremises]](
          any(),
          any(),
          meq(Seq(TradingPremisesSection.tradingPremisesWithHasChangedFalse.copy(
            hasChanged = true,
            yourTradingPremises = Some(YourTradingPremises("Trading Name", TradingPremisesSection.address, Some(true), Some(TradingPremisesSection.date)))
          ))))(any(), any())
      }
    }
  }

  "go to the date of change page" when {
    "the submission has been approved and trading name has changed" in new Fixture {

      val initRequest = requestWithUrlEncodedBody(
        "tradingName" -> "Trading Name",
        "addressLine1" -> "Address 1",
        "addressLine2" -> "Address 2",
        "postcode" -> "AA1 1AA"
      )

      val address = Address("addressLine1", "addressLine2", None, None, "AA1 1AA")
      val yourTradingPremises = YourTradingPremises(tradingName = "Trading Name 2", address, isResidential = Some(true), Some(LocalDate.now()))

      when(controller.dataCacheConnector.fetch[Seq[TradingPremises]](any(), any())(any(), any()))
        .thenReturn(Future.successful(Some(Seq(TradingPremises(yourTradingPremises = Some(yourTradingPremises), lineId = Some(1))))))

      when(controller.dataCacheConnector.save[TradingPremises](any(), any(), any())(any(), any()))
        .thenReturn(Future.successful(emptyCache))

      when(controller.statusService.getStatus(any[Option[String]](), any[(String, String)](), any[String]())(any(), any()))
        .thenReturn(Future.successful(SubmissionDecisionApproved))

      val result = controller.post(1, edit = true)(initRequest)

      hstatus(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(routes.WhereAreTradingPremisesController.dateOfChange(1).url))
    }
  }

  "go to the date of change page" when {
    "the status id ready for renewal and trading name has changed" in new Fixture {

      val initRequest = requestWithUrlEncodedBody(
        "tradingName" -> "Trading Name",
        "addressLine1" -> "Address 1",
        "addressLine2" -> "Address 2",
        "postcode" -> "AA1 1AA"
      )

      val address = Address("addressLine1", "addressLine2", None, None, "AA1 1AA")
      val yourTradingPremises = YourTradingPremises(tradingName = "Trading Name 2", address, isResidential = Some(true), Some(LocalDate.now()))

      when(controller.dataCacheConnector.fetch[Seq[TradingPremises]](any(), any())(any(), any()))
        .thenReturn(Future.successful(Some(Seq(TradingPremises(yourTradingPremises = Some(yourTradingPremises), lineId = Some(1))))))

      when(controller.dataCacheConnector.save[TradingPremises](any(), any(), any())(any(), any()))
        .thenReturn(Future.successful(emptyCache))

      when(controller.statusService.getStatus(any[Option[String]](), any[(String, String)](), any[String]())(any(), any()))
        .thenReturn(Future.successful(ReadyForRenewal(None)))

      val result = controller.post(1, edit = true)(initRequest)

      hstatus(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(routes.WhereAreTradingPremisesController.dateOfChange(1).url))
    }
  }

  "go to the check your answers page" when {
    "data has not changed" in new Fixture {

      val initRequest = requestWithUrlEncodedBody(
        "tradingName" -> "Trading Name",
        "addressLine1" -> "Address 1",
        "addressLine2" -> "Address 2",
        "postcode" -> "AA1 1AA"
      )

      val address = Address("Address 1", "Address 2", None, None, "AA1 1AA")
      val yourTradingPremises = YourTradingPremises(tradingName = "Trading Name", address, Some(true), Some(new LocalDate(2007, 2, 1)))


      when(controller.dataCacheConnector.fetch[Seq[TradingPremises]](any(), any())(any(), any()))
        .thenReturn(Future.successful(Some(Seq(TradingPremises(yourTradingPremises = Some(yourTradingPremises))))))

      when(controller.dataCacheConnector.save[TradingPremises](any(),any(),  any())(any(), any()))
        .thenReturn(Future.successful(emptyCache))


      when(controller.statusService.getStatus(any[Option[String]](), any[(String, String)](), any[String]())(any(), any()))
        .thenReturn(Future.successful(SubmissionDecisionApproved))


      val result = controller.post(1)(initRequest)

      hstatus(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(controllers.tradingpremises.routes.ActivityStartDateController.get(1).url))
    }

    "the trading premises instance is brand new" in new Fixture {

      val initRequest = requestWithUrlEncodedBody(
        "tradingName" -> "Trading Name",
        "addressLine1" -> "Address 1",
        "addressLine2" -> "Address 2",
        "postcode" -> "AA1 1AA"
      )

      val address = Address("Address 1", "Address 2", None, None, "AA1 1AA")
      val yourTradingPremises = YourTradingPremises(tradingName = "Trading Name 2", address, isResidential = Some(true), Some(new LocalDate(2007, 2, 1)))

      when(controller.dataCacheConnector.fetch[Seq[TradingPremises]](any(), any())(any(), any()))
        .thenReturn(Future.successful(Some(Seq(TradingPremises(yourTradingPremises = Some(yourTradingPremises), lineId = None)))))

      when(controller.dataCacheConnector.save[TradingPremises](any(), any(), any())(any(), any()))
        .thenReturn(Future.successful(emptyCache))

      when(controller.statusService.getStatus(any[Option[String]](), any[(String, String)](), any[String]())(any(), any()))
        .thenReturn(Future.successful(SubmissionDecisionApproved))

      val result = controller.post(1, edit = true)(initRequest)

      hstatus(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(controllers.tradingpremises.routes.DetailedAnswersController.get(1).url))
    }
  }

  "return view for Date of Change" in new Fixture {
    val result = controller.dateOfChange(1)(request)
    implicit val ec: ExecutionContext = app.injector.instanceOf[ExecutionContext]
    hstatus(result) must be(OK)
  }

  "handle the date of change form post" when {
    "given valid data for a trading premises name" in new Fixture {

      val postRequest = requestWithUrlEncodedBody(
        "dateOfChange.year" -> "2010",
        "dateOfChange.month" -> "10",
        "dateOfChange.day" -> "01"
      )

      val address = Address("addressLine1", "addressLine2", None, None, "AA1 1AA", Some(DateOfChange(new LocalDate(2010, 10, 1))))

      val yourPremises = YourTradingPremises("Some name", address.copy(dateOfChange = None), isResidential = Some(true), Some(new LocalDate(2001, 1, 1)), None)
      val premises = TradingPremises(yourTradingPremises = Some(yourPremises))

      val expectedResult = yourPremises.copy(
        tradingNameChangeDate = Some(DateOfChange(new LocalDate(2010, 10, 1))),
        tradingPremisesAddress = address
      )

      when(controller.dataCacheConnector.fetch[Seq[TradingPremises]](any(), any())(any(), any()))
        .thenReturn(Future.successful(Some(Seq(premises))))

      when(controller.dataCacheConnector.save[TradingPremises](any(), meq(TradingPremises.key), any[TradingPremises])(any(), any())).
        thenReturn(Future.successful(mock[CacheMap]))

      val result = controller.saveDateOfChange(1)(postRequest)

      hstatus(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(routes.DetailedAnswersController.get(1).url))

      val captor = ArgumentCaptor.forClass(classOf[Seq[TradingPremises]])
      verify(controller.dataCacheConnector).save[Seq[TradingPremises]](any(), meq(TradingPremises.key), captor.capture())(any(), any())

      captor.getValue.head.yourTradingPremises match {
        case Some(result: YourTradingPremises) => result must be(expectedResult)
      }

    }

    "given invalid form data" in new Fixture {

      val tp = mock[TradingPremises]
      val ytp = mock[YourTradingPremises]

      when(tp.yourTradingPremises) thenReturn Some(ytp)
      when(ytp.startDate) thenReturn Some(new LocalDate(2011,1,1))

      val postRequest = requestWithUrlEncodedBody("invalid" -> "data")

      when(mockDataCacheConnector.fetch[Seq[TradingPremises]](any(), meq(TradingPremises.key))(any(), any())) thenReturn Future.successful(Some(Seq(tp)))

      val result = controller.saveDateOfChange(1)(postRequest)

      hstatus(result) must be(BAD_REQUEST)
      contentAsString(result) must include(Messages("error.expected.jodadate.format"))
    }

    "given a date of change in the future" in new Fixture {

      val tp = mock[TradingPremises]
      val ytp = mock[YourTradingPremises]

      when(tp.yourTradingPremises) thenReturn Some(ytp)
      when(ytp.startDate) thenReturn Some(new LocalDate(2011,1,1))

      val postRequest = requestWithUrlEncodedBody(
        "dateOfChange.day" -> "1",
        "dateOfChange.month" -> "1",
        "dateOfChange.year" -> LocalDate.now.plusYears(1).getYear.toString
      )

      when(mockDataCacheConnector.fetch[Seq[TradingPremises]](any(), meq(TradingPremises.key))(any(), any())) thenReturn Future.successful(Some(Seq(tp)))

      val result = controller.saveDateOfChange(1)(postRequest)

      hstatus(result) must be(BAD_REQUEST)
      contentAsString(result) must include(Messages("error.future.date"))
    }

  }

  "given a date of change which is before the activity start date" in new Fixture {
    val postRequest = requestWithUrlEncodedBody(
      "dateOfChange.year" -> "2007",
      "dateOfChange.month" -> "10",
      "dateOfChange.day" -> "01"
    )

    val yourPremises = YourTradingPremises("Some name", mock[Address], isResidential = Some(true), Some(new LocalDate(2008, 1, 1)), None)
    val premises = TradingPremises(yourTradingPremises = Some(yourPremises))

    when(controller.dataCacheConnector.fetch[Seq[TradingPremises]](any(), any())(any(), any()))
      .thenReturn(Future.successful(Some(Seq(premises))))

    val result = controller.saveDateOfChange(1)(postRequest)

    hstatus(result) must be(BAD_REQUEST)
    contentAsString(result) must include(Messages("error.expected.tp.dateofchange.after.startdate", "01-01-2008"))
  }
}
