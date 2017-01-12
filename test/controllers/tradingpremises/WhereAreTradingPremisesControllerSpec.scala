package controllers.tradingpremises


import connectors.DataCacheConnector
import controllers.aboutthebusiness.routes
import models.businessmatching.{BillPaymentServices, EstateAgentBusinessService, MoneyServiceBusiness}
import models.tradingpremises._
import org.joda.time.LocalDate
import org.jsoup.Jsoup
import org.mockito.Mockito._
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.i18n.Messages
import play.api.test.Helpers.{status => hstatus, _}
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.AuthorisedFixture
import org.scalatest.mock.MockitoSugar
import org.mockito.Matchers.{eq => meq, _}
import models._
import models.aboutthebusiness.{AboutTheBusiness, ActivityStartDate}
import models.status.{SubmissionDecisionApproved, SubmissionDecisionRejected}
import org.mockito.ArgumentCaptor
import services.StatusService

import scala.concurrent.Future

class WhereAreTradingPremisesControllerSpec extends PlaySpec with OneAppPerSuite with MockitoSugar {

  val mockDataCacheConnector = mock[DataCacheConnector]

  trait Fixture extends AuthorisedFixture {
    self =>

    val controller = new WhereAreTradingPremisesController {
      override val dataCacheConnector = mockDataCacheConnector
      override val authConnector = self.authConnector
      override val statusService = mock[StatusService]
    }

    when(controller.statusService.getStatus(any(),any(),any())).thenReturn(Future.successful(SubmissionDecisionRejected))
  }

  val emptyCache = CacheMap("", Map.empty)
  val fields = Array[String]("tradingName", "addressLine1", "addressLine2", "postcode")
  val RecordId1 = 1


  "WhereAreTradingPremisesController" when {

    "get is called" must {
      "respond with OK and show the form with data when there is data" in new Fixture {

        val address = Address("addressLine1", "addressLine2", None, None, "NE98 1ZZ")
        val yourTradingPremises = YourTradingPremises(tradingName = "trading Name", address, true, LocalDate.now())
        val tradingPremises = TradingPremises(None, Some(yourTradingPremises), None, None)

        when(mockDataCacheConnector.fetch[Seq[TradingPremises]](any())(any(), any(), any()))
          .thenReturn(Future.successful(Some(Seq(tradingPremises))))

        val result = controller.get(RecordId1, true)(request)
        val document = Jsoup.parse(contentAsString(result))

        hstatus(result) must be(OK)
        contentAsString(result) must include(Messages("tradingpremises.yourtradingpremises.title"))
        for (field <- fields)
          document.select(s"input[id=$field]").`val`() must not be empty
      }

      "respond with OK and show the empty form when there is no data" in new Fixture {

        when(mockDataCacheConnector.fetch[Seq[TradingPremises]](any())(any(), any(), any()))
          .thenReturn(Future.successful(Some(Seq(TradingPremises()))))

        val result = controller.get(RecordId1, false)(request)
        val document = Jsoup.parse(contentAsString(result))

        hstatus(result) must be(OK)
        contentAsString(result) must include(Messages("tradingpremises.yourtradingpremises.title"))
        for (field <- fields)
          document.select(s"input[id=$field]").`val`() must be(empty)

      }

      "respond with NOT_FOUND when there is no data at all at the given index" in new Fixture {

        when(mockDataCacheConnector.fetch[Seq[TradingPremises]](any())
          (any(), any(), any())).thenReturn(Future.successful(None))

        val result = controller.get(RecordId1, false)(request)

        hstatus(result) must be(NOT_FOUND)
      }
    }

    "post is called" must {
      "respond with SEE_OTHER" when {
        "edit mode is false, and redirect to the 'what does your business do' page" in new Fixture {

          val newRequest = request.withFormUrlEncodedBody(
            "tradingName" -> "Trading Name",
            "addressLine1" -> "Address 1",
            "addressLine2" -> "Address 2",
            "postcode" -> "NE98 1ZZ",
            "isResidential" -> "true",
            "startDate.day" -> "01",
            "startDate.month" -> "02",
            "startDate.year" -> "2010"
          )

          when(controller.dataCacheConnector.fetch[Seq[TradingPremises]](any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(Seq(TradingPremises()))))

          when(controller.dataCacheConnector.save[TradingPremises](any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(emptyCache))

          val result = controller.post(RecordId1, false)(newRequest)

          hstatus(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(controllers.tradingpremises.routes.WhatDoesYourBusinessDoController.get(1).url))
        }

        "edit mode is true, and redirect to WhatDoesYourBusinessDo Controller" in new Fixture {

          val newRequest = request.withFormUrlEncodedBody(
            "tradingName" -> "Trading Name",
            "addressLine1" -> "Address 1",
            "addressLine2" -> "Address 2",
            "postcode" -> "NE98 1ZZ",
            "isResidential" -> "true",
            "startDate.day" -> "01",
            "startDate.month" -> "02",
            "startDate.year" -> "2010"
          )

          when(controller.dataCacheConnector.fetch[Seq[TradingPremises]](any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(Seq(TradingPremises()))))

          when(controller.dataCacheConnector.save[TradingPremises](any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(emptyCache))


          val result = controller.post(RecordId1, true)(newRequest)

          hstatus(result) must be(SEE_OTHER)
          redirectLocation(result) must be(
            Some(controllers.tradingpremises.routes.SummaryController.getIndividual(1).url))
        }
      }

      "respond with BAD_REQUEST" when {
        "given an invalid request" in new Fixture {
          val newRequest = request.withFormUrlEncodedBody(
            "tradingName" -> "Trading Name"
          )
          when(controller.dataCacheConnector.fetch[Seq[TradingPremises]](any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(Seq(TradingPremises()))))

          when(controller.dataCacheConnector.save[TradingPremises](any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(emptyCache))

          val result = controller.post(RecordId1, true)(newRequest)

          hstatus(result) must be(BAD_REQUEST)

        }

        "date contains an invalid year too great in length" in new Fixture {

          val newRequest = request.withFormUrlEncodedBody(
            "tradingName" -> "Trading Name",
            "addressLine1" -> "Address 1",
            "addressLine2" -> "Address 2",
            "postcode" -> "NE98 1ZZ",
            "isResidential" -> "true",
            "startDate.day" -> "01",
            "startDate.month" -> "02",
            "startDate.year" -> "201345670"
          )

          when(controller.dataCacheConnector.fetch[Seq[TradingPremises]](any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(Seq(TradingPremises()))))

          when(controller.dataCacheConnector.save[TradingPremises](any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(emptyCache))

          val result = controller.post(RecordId1, false)(newRequest)

          hstatus(result) must be(BAD_REQUEST)
          contentAsString(result) must include(Messages("error.expected.jodadate.format"))

        }
      }

      "respond with NOT_FOUND" when {
        "the given index is out of bounds" in new Fixture {

          val newRequest = request.withFormUrlEncodedBody(
            "tradingName" -> "Trading Name",
            "addressLine1" -> "Address 1",
            "addressLine2" -> "Address 2",
            "postcode" -> "NE98 1ZZ",
            "isResidential" -> "true",
            "startDate.day" -> "01",
            "startDate.month" -> "02",
            "startDate.year" -> "2010"
          )

          when(controller.dataCacheConnector.fetch[Seq[TradingPremises]](any())(any(), any(), any()))
            .thenReturn(Future.successful(None))

          when(controller.dataCacheConnector.save[TradingPremises](any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(emptyCache))


          val result = controller.post(3, false)(newRequest)

          hstatus(result) must be(NOT_FOUND)
        }
      }

      "set the hasChanged flag to true" in new Fixture {

        val newRequest = request.withFormUrlEncodedBody(
          "tradingName" -> "Trading Name",
          "addressLine1" -> "Address 1",
          "addressLine2" -> "Address 2",
          "postcode" -> "NE98 1ZZ",
          "isResidential" -> "true",
          "startDate.day" -> "01",
          "startDate.month" -> "02",
          "startDate.year" -> "2010"
        )

        when(controller.dataCacheConnector.fetch[Seq[TradingPremises]](any())(any(), any(), any()))
          .thenReturn(Future.successful(Some(Seq(TradingPremisesSection.tradingPremisesWithHasChangedFalse))))

        when(controller.dataCacheConnector.save[TradingPremises](any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(emptyCache))

        val result = controller.post(1)(newRequest)

        hstatus(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some(routes.WhatDoesYourBusinessDoController.get(1, false).url))

        verify(controller.dataCacheConnector).save[Seq[TradingPremises]](
          any(),
          meq(Seq(TradingPremisesSection.tradingPremisesWithHasChangedFalse.copy(
            hasChanged = true,
            yourTradingPremises = Some(YourTradingPremises("Trading Name", TradingPremisesSection.address, true, TradingPremisesSection.date))
          ))))(any(), any(), any())
      }
    }
  }

  "go to the date of change page" when {
    "the submission has been approved and trading name has changed" in new Fixture {

      val initRequest = request.withFormUrlEncodedBody(
        "tradingName" -> "Trading Name",
        "addressLine1" -> "Address 1",
        "addressLine2" -> "Address 2",
        "postcode" -> "NE98 1ZZ",
        "isResidential" -> "true",
        "startDate.day" -> "01",
        "startDate.month" -> "02",
        "startDate.year" -> "2010"
      )

      val address = Address("addressLine1", "addressLine2", None, None, "NE98 1ZZ")
      val yourTradingPremises = YourTradingPremises(tradingName = "Trading Name 2", address, true, LocalDate.now())


      when(controller.dataCacheConnector.fetch[Seq[TradingPremises]](any())(any(), any(), any()))
        .thenReturn(Future.successful(Some(Seq(TradingPremises(yourTradingPremises = Some(yourTradingPremises))))))

      when(controller.dataCacheConnector.save[TradingPremises](any(), any())(any(), any(), any()))
        .thenReturn(Future.successful(emptyCache))


      when(controller.statusService.getStatus(any(), any(), any()))
        .thenReturn(Future.successful(SubmissionDecisionApproved))


      val result = controller.post(1)(initRequest)

      hstatus(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(routes.WhereAreTradingPremisesController.dateOfChange(1).url))
    }
  }

  "go to the summary page" when {
    "data has not changed" in new Fixture {

      val initRequest = request.withFormUrlEncodedBody(
        "tradingName" -> "Trading Name",
        "addressLine1" -> "Address 1",
        "addressLine2" -> "Address 2",
        "postcode" -> "NE98 1ZZ",
        "isResidential" -> "true",
        "startDate.day" -> "01",
        "startDate.month" -> "02",
        "startDate.year" -> "2010"
      )

      val address = Address("Address 1", "Address 2", None, None, "NE98 1ZZ")
      val yourTradingPremises = YourTradingPremises(tradingName = "Trading Name", address, true, new LocalDate(2010,2,1))


      when(controller.dataCacheConnector.fetch[Seq[TradingPremises]](any())(any(), any(), any()))
        .thenReturn(Future.successful(Some(Seq(TradingPremises(yourTradingPremises = Some(yourTradingPremises))))))

      when(controller.dataCacheConnector.save[TradingPremises](any(), any())(any(), any(), any()))
        .thenReturn(Future.successful(emptyCache))


      when(controller.statusService.getStatus(any(), any(), any()))
        .thenReturn(Future.successful(SubmissionDecisionApproved))


      val result = controller.post(1)(initRequest)

      hstatus(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(controllers.tradingpremises.routes.WhatDoesYourBusinessDoController.get(1).url))
    }
  }
  "return view for Date of Change" in new Fixture {
    val result = controller.dateOfChange(1)(request)
    hstatus(result) must be(OK)
  }

  "handle the date of change form post" when {
    "given valid data for a trading premises name" in new Fixture {

      val postRequest = request.withFormUrlEncodedBody(
        "dateOfChange.year" -> "2010",
        "dateOfChange.month" -> "10",
        "dateOfChange.day" -> "01"
      )

      val name = AgentName("someName")
      val updatedName = name.copy(dateOfChange = Some(DateOfChange(new LocalDate(2010, 10, 1))))

      val premises = TradingPremises(agentName = Some(name))

      when(controller.dataCacheConnector.fetch[AboutTheBusiness](meq(AboutTheBusiness.key))(any(), any(), any())).
        thenReturn(Future.successful(Some(AboutTheBusiness(activityStartDate = Some(ActivityStartDate(new LocalDate(2009, 1, 1)))))))

      when(controller.dataCacheConnector.fetch[Seq[TradingPremises]](any())(any(), any(), any()))
        .thenReturn(Future.successful(Some(Seq(premises))))

      when(controller.dataCacheConnector.save[TradingPremises](meq(TradingPremises.key), any[TradingPremises])(any(), any(), any())).
        thenReturn(Future.successful(mock[CacheMap]))

      val result = controller.saveDateOfChange(1)(postRequest)

      hstatus(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(routes.SummaryController.get().url))

      val captor = ArgumentCaptor.forClass(classOf[TradingPremises])
      verify(controller.dataCacheConnector).save[TradingPremises](meq(TradingPremises.key), captor.capture())(any(), any(), any())

      captor.getValue.agentName match {
        case Some(savedName: AgentName) => savedName must be(updatedName)
      }

    }


    "given a date of change which is before the activity start date" in new Fixture {
      val postRequest = request.withFormUrlEncodedBody(
        "dateOfChange.year" -> "2007",
        "dateOfChange.month" -> "10",
        "dateOfChange.day" -> "01"
      )

      when(controller.dataCacheConnector.fetch[AboutTheBusiness](meq(AboutTheBusiness.key))(any(), any(), any())).
        thenReturn(Future.successful(Some(AboutTheBusiness(activityStartDate = Some(ActivityStartDate(new LocalDate(2009, 1, 1)))))))

      val result = controller.saveDateOfChange(1)(postRequest)

      hstatus(result) must be(BAD_REQUEST)
    }
  }
}


