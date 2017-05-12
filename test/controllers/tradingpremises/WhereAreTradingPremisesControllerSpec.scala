package controllers.tradingpremises

import connectors.DataCacheConnector
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
import org.scalatest.mock.MockitoSugar
import utils.GenericTestHelper
import play.api.i18n.Messages

import scala.collection.JavaConversions._
import play.api.test.Helpers.{status => hstatus, _}
import services.StatusService
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.AuthorisedFixture

import scala.concurrent.Future

class WhereAreTradingPremisesControllerSpec extends GenericTestHelper with MockitoSugar with BeforeAndAfter {

  private val mockDataCacheConnector = mock[DataCacheConnector]

  trait Fixture extends AuthorisedFixture {
    self => val request = addToken(authRequest)

    val controller = new WhereAreTradingPremisesController {
      override val dataCacheConnector = mockDataCacheConnector
      override val authConnector = self.authConnector
      override val statusService = mock[StatusService]
    }

    when(controller.statusService.getStatus(any(), any(), any())).thenReturn(Future.successful(SubmissionDecisionRejected))
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

      }

      "respond with NOT_FOUND when there is no data at all at the given index" in new Fixture {

        when(mockDataCacheConnector.fetch[Seq[TradingPremises]](any())
          (any(), any(), any())).thenReturn(Future.successful(None))

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

          val newRequest = request.withFormUrlEncodedBody(
            "tradingName" -> "Trading Name",
            "addressLine1" -> "Address 1",
            "addressLine2" -> "Address 2",
            "postcode" -> "AA1 1AA"
          )

          when(controller.dataCacheConnector.fetch[Seq[TradingPremises]](any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(Seq(TradingPremises(yourTradingPremises = Some(ytp))))))

          val updatedYtp = ytp.copy(tradingName = "Trading Name", tradingPremisesAddress = Address("Address 1", "Address 2", None, None, "AA1 1AA"))

          when(controller.dataCacheConnector.save[Seq[TradingPremises]](any(),
            meq(Seq(TradingPremises(yourTradingPremises = Some(updatedYtp), hasChanged = true))))(any(), any(), any()))
            .thenReturn(Future.successful(emptyCache))

          val result = controller.post(RecordId1, false)(newRequest)

          hstatus(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(controllers.tradingpremises.routes.ActivityStartDateController.get(1).url))
        }
        
        "fail submission on invalid uk address" in new Fixture {

          val newRequest = request.withFormUrlEncodedBody(
            "tradingName" -> "Trading Name",
            "addressLine1" -> "Address **1",
            "addressLine2" -> "Address **2",
            "addressLine3" -> "Address **3",
            "addressLine4" -> "Address **4",
            "postcode" -> "AA1 1AA"
          )

          when(controller.dataCacheConnector.fetch[Seq[TradingPremises]](any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(Seq(TradingPremises()))))

          when(controller.dataCacheConnector.save[TradingPremises](any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(emptyCache))

          val result = controller.post(RecordId1, false)(newRequest)

          val document: Document  = Jsoup.parse(contentAsString(result))
          val errorCount = 4
          val elementsWithError : Elements = document.getElementsByClass("error-notification")
          elementsWithError.size() must be(errorCount)
          for (ele: Element <- elementsWithError) {
            ele.html() must include(Messages("err.text.validation"))
          }
        }

        "redirect to the 'Activity Start Date' page when no data in save4later" in new Fixture {

          val newRequest = request.withFormUrlEncodedBody(
            "tradingName" -> "Trading Name",
            "addressLine1" -> "Address 1",
            "addressLine2" -> "Address 2",
            "postcode" -> "AA1 1AA"
          )
          val newYtp = Some(YourTradingPremises(tradingName = "Trading Name",
            tradingPremisesAddress = Address("Address 1", "Address 2", None, None, "AA1 1AA")))

          when(controller.dataCacheConnector.fetch[Seq[TradingPremises]](any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(Seq(TradingPremises()))))

          when(controller.dataCacheConnector.save[Seq[TradingPremises]](any(), meq(Seq(TradingPremises(yourTradingPremises = newYtp,  hasChanged = true))))(any(), any(), any()))
            .thenReturn(Future.successful(emptyCache))


          val result = controller.post(RecordId1, false)(newRequest)

          hstatus(result) must be(SEE_OTHER)
          redirectLocation(result) must be(
            Some(controllers.tradingpremises.routes.ActivityStartDateController.get(1, false).url))
        }

        "edit mode is true, and redirect to check your answer page" in new Fixture {

          val newRequest = request.withFormUrlEncodedBody(
            "tradingName" -> "Trading Name",
            "addressLine1" -> "Address 1",
            "addressLine2" -> "Address 2",
            "postcode" -> "AA1 1AA"
          )
          when(controller.dataCacheConnector.fetch[Seq[TradingPremises]](any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(Seq(TradingPremises()))))

          when(controller.dataCacheConnector.save[Seq[TradingPremises]](any(), any())(any(), any(), any()))
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
            "addressLine1" -> "Address 1"*120,
            "addressLine2" -> "Address 2",
            "postcode" -> "AA1 1AA"
          )

          when(controller.dataCacheConnector.fetch[Seq[TradingPremises]](any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(Seq(TradingPremises()))))

          when(controller.dataCacheConnector.save[TradingPremises](any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(emptyCache))

          val result = controller.post(RecordId1, false)(newRequest)

          hstatus(result) must be(BAD_REQUEST)
          contentAsString(result) must include(Messages("error.max.length.address.line"))

        }
      }

      "respond with NOT_FOUND" when {
        "the given index is out of bounds" in new Fixture {

          val newRequest = request.withFormUrlEncodedBody(
            "tradingName" -> "Trading Name",
            "addressLine1" -> "Address 1",
            "addressLine2" -> "Address 2",
            "postcode" -> "AA1 1AA"
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
          "postcode" -> "AA1 1AA"
        )

        when(controller.dataCacheConnector.fetch[Seq[TradingPremises]](any())(any(), any(), any()))
          .thenReturn(Future.successful(Some(Seq(TradingPremisesSection.tradingPremisesWithHasChangedFalse))))

        when(controller.dataCacheConnector.save[TradingPremises](any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(emptyCache))

        val result = controller.post(1)(newRequest)

        hstatus(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some(routes.ActivityStartDateController.get(1, false).url))

        verify(controller.dataCacheConnector).save[Seq[TradingPremises]](
          any(),
          meq(Seq(TradingPremisesSection.tradingPremisesWithHasChangedFalse.copy(
            hasChanged = true,
            yourTradingPremises = Some(YourTradingPremises("Trading Name", TradingPremisesSection.address, Some(true), Some(TradingPremisesSection.date)))
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
        "postcode" -> "AA1 1AA"
      )

      val address = Address("addressLine1", "addressLine2", None, None, "AA1 1AA")
      val yourTradingPremises = YourTradingPremises(tradingName = "Trading Name 2", address, isResidential = Some(true), Some(LocalDate.now()))

      when(controller.dataCacheConnector.fetch[Seq[TradingPremises]](any())(any(), any(), any()))
        .thenReturn(Future.successful(Some(Seq(TradingPremises(yourTradingPremises = Some(yourTradingPremises), lineId = Some(1))))))

      when(controller.dataCacheConnector.save[TradingPremises](any(), any())(any(), any(), any()))
        .thenReturn(Future.successful(emptyCache))

      when(controller.statusService.getStatus(any(), any(), any()))
        .thenReturn(Future.successful(SubmissionDecisionApproved))

      val result = controller.post(1, edit = true)(initRequest)

      hstatus(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(routes.WhereAreTradingPremisesController.dateOfChange(1).url))
    }
  }

  "go to the date of change page" when {
    "the status id ready for renewal and trading name has changed" in new Fixture {

      val initRequest = request.withFormUrlEncodedBody(
        "tradingName" -> "Trading Name",
        "addressLine1" -> "Address 1",
        "addressLine2" -> "Address 2",
        "postcode" -> "AA1 1AA"
      )

      val address = Address("addressLine1", "addressLine2", None, None, "AA1 1AA")
      val yourTradingPremises = YourTradingPremises(tradingName = "Trading Name 2", address, isResidential = Some(true), Some(LocalDate.now()))

      when(controller.dataCacheConnector.fetch[Seq[TradingPremises]](any())(any(), any(), any()))
        .thenReturn(Future.successful(Some(Seq(TradingPremises(yourTradingPremises = Some(yourTradingPremises), lineId = Some(1))))))

      when(controller.dataCacheConnector.save[TradingPremises](any(), any())(any(), any(), any()))
        .thenReturn(Future.successful(emptyCache))

      when(controller.statusService.getStatus(any(), any(), any()))
        .thenReturn(Future.successful(ReadyForRenewal(None)))

      val result = controller.post(1, edit = true)(initRequest)

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
        "postcode" -> "AA1 1AA"
      )

      val address = Address("Address 1", "Address 2", None, None, "AA1 1AA")
      val yourTradingPremises = YourTradingPremises(tradingName = "Trading Name", address, Some(true), Some(new LocalDate(2007, 2, 1)))


      when(controller.dataCacheConnector.fetch[Seq[TradingPremises]](any())(any(), any(), any()))
        .thenReturn(Future.successful(Some(Seq(TradingPremises(yourTradingPremises = Some(yourTradingPremises))))))

      when(controller.dataCacheConnector.save[TradingPremises](any(), any())(any(), any(), any()))
        .thenReturn(Future.successful(emptyCache))


      when(controller.statusService.getStatus(any(), any(), any()))
        .thenReturn(Future.successful(SubmissionDecisionApproved))


      val result = controller.post(1)(initRequest)

      hstatus(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(controllers.tradingpremises.routes.ActivityStartDateController.get(1).url))
    }

    "the trading premises instance is brand new" in new Fixture {

      val initRequest = request.withFormUrlEncodedBody(
        "tradingName" -> "Trading Name",
        "addressLine1" -> "Address 1",
        "addressLine2" -> "Address 2",
        "postcode" -> "AA1 1AA"
      )

      val address = Address("Address 1", "Address 2", None, None, "AA1 1AA")
      val yourTradingPremises = YourTradingPremises(tradingName = "Trading Name 2", address, isResidential = Some(true), Some(new LocalDate(2007, 2, 1)))

      when(controller.dataCacheConnector.fetch[Seq[TradingPremises]](any())(any(), any(), any()))
        .thenReturn(Future.successful(Some(Seq(TradingPremises(yourTradingPremises = Some(yourTradingPremises), lineId = None)))))

      when(controller.dataCacheConnector.save[TradingPremises](any(), any())(any(), any(), any()))
        .thenReturn(Future.successful(emptyCache))

      when(controller.statusService.getStatus(any(), any(), any()))
        .thenReturn(Future.successful(SubmissionDecisionApproved))

      val result = controller.post(1, edit = true)(initRequest)

      hstatus(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(controllers.tradingpremises.routes.SummaryController.getIndividual(1).url))
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

      val address = Address("addressLine1", "addressLine2", None, None, "AA1 1AA", Some(DateOfChange(new LocalDate(2010, 10, 1))))

      val yourPremises = YourTradingPremises("Some name", address.copy(dateOfChange = None), isResidential = Some(true), Some(new LocalDate(2001, 1, 1)), None)
      val premises = TradingPremises(yourTradingPremises = Some(yourPremises))

      val expectedResult = yourPremises.copy(
        tradingNameChangeDate = Some(DateOfChange(new LocalDate(2010, 10, 1))),
        tradingPremisesAddress = address
      )

      when(controller.dataCacheConnector.fetch[Seq[TradingPremises]](any())(any(), any(), any()))
        .thenReturn(Future.successful(Some(Seq(premises))))

      when(controller.dataCacheConnector.save[TradingPremises](meq(TradingPremises.key), any[TradingPremises])(any(), any(), any())).
        thenReturn(Future.successful(mock[CacheMap]))

      val result = controller.saveDateOfChange(1)(postRequest)

      hstatus(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(routes.SummaryController.get().url))

      val captor = ArgumentCaptor.forClass(classOf[Seq[TradingPremises]])
      verify(controller.dataCacheConnector).save[Seq[TradingPremises]](meq(TradingPremises.key), captor.capture())(any(), any(), any())

      captor.getValue.head.yourTradingPremises match {
        case Some(result: YourTradingPremises) => result must be(expectedResult)
      }

    }

    "given invalid form data" in new Fixture {

      val tp = mock[TradingPremises]
      val ytp = mock[YourTradingPremises]

      when(tp.yourTradingPremises) thenReturn Some(ytp)
      when(ytp.startDate) thenReturn Some(new LocalDate(2011,1,1))

      val postRequest = request.withFormUrlEncodedBody()

      when(mockDataCacheConnector.fetch[Seq[TradingPremises]](meq(TradingPremises.key))(any(), any(), any())) thenReturn Future.successful(Some(Seq(tp)))

      val result = controller.saveDateOfChange(1)(postRequest)

      hstatus(result) must be(BAD_REQUEST)
      contentAsString(result) must include(Messages("error.expected.jodadate.format"))
    }

    "given a date of change in the future" in new Fixture {

      val tp = mock[TradingPremises]
      val ytp = mock[YourTradingPremises]

      when(tp.yourTradingPremises) thenReturn Some(ytp)
      when(ytp.startDate) thenReturn Some(new LocalDate(2011,1,1))

      val postRequest = request.withFormUrlEncodedBody(
        "dateOfChange.day" -> "1",
        "dateOfChange.month" -> "1",
        "dateOfChange.year" -> LocalDate.now.plusYears(1).getYear.toString
      )

      when(mockDataCacheConnector.fetch[Seq[TradingPremises]](meq(TradingPremises.key))(any(), any(), any())) thenReturn Future.successful(Some(Seq(tp)))

      val result = controller.saveDateOfChange(1)(postRequest)

      hstatus(result) must be(BAD_REQUEST)
      contentAsString(result) must include(Messages("error.future.date"))
    }

  }

  "given a date of change which is before the activity start date" in new Fixture {
    val postRequest = request.withFormUrlEncodedBody(
      "dateOfChange.year" -> "2007",
      "dateOfChange.month" -> "10",
      "dateOfChange.day" -> "01"
    )

    val yourPremises = YourTradingPremises("Some name", mock[Address], isResidential = Some(true), Some(new LocalDate(2008, 1, 1)), None)
    val premises = TradingPremises(yourTradingPremises = Some(yourPremises))

    when(controller.dataCacheConnector.fetch[Seq[TradingPremises]](any())(any(), any(), any()))
      .thenReturn(Future.successful(Some(Seq(premises))))

    val result = controller.saveDateOfChange(1)(postRequest)

    hstatus(result) must be(BAD_REQUEST)
    contentAsString(result) must include(Messages("error.expected.tp.dateofchange.after.startdate", "01-01-2008"))
  }
}
