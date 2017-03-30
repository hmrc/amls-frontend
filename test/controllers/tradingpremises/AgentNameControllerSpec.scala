package controllers.tradingpremises

import connectors.DataCacheConnector
import models.DateOfChange
import models.businessmatching.{BillPaymentServices, EstateAgentBusinessService, MoneyServiceBusiness}
import models.status.{SubmissionDecisionApproved, SubmissionDecisionRejected}
import models.tradingpremises._
import org.joda.time.LocalDate
import org.jsoup.Jsoup
import org.mockito.ArgumentCaptor
import org.mockito.Matchers.{eq => meq, _}
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import  utils.GenericTestHelper
import play.api.i18n.Messages
import play.api.test.Helpers._
import services.StatusService
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.http.HeaderCarrier
import utils.AuthorisedFixture

import scala.concurrent.Future

class AgentNameControllerSpec extends GenericTestHelper with MockitoSugar with ScalaFutures {

  trait Fixture extends AuthorisedFixture {
    self => val request = addToken(authRequest)

    val controller = new AgentNameController {
      override val dataCacheConnector: DataCacheConnector = mock[DataCacheConnector]
      override val authConnector: AuthConnector = self.authConnector
      override val statusService = mock[StatusService]
    }

    when(controller.statusService.getStatus(any(),any(),any())).thenReturn(Future.successful(SubmissionDecisionRejected))
  }

  "AgentNameController" when {

    val emptyCache = CacheMap("", Map.empty)
    val mockCacheMap = mock[CacheMap]

    "get is called" must {
      "display agent name Page" in new Fixture {

        when(controller.dataCacheConnector.fetch[Seq[TradingPremises]](any())(any(), any(), any()))
          .thenReturn(Future.successful(Some(Seq(TradingPremises()))))

        val result = controller.get(1)(request)
        status(result) must be(OK)

        val document = Jsoup.parse(contentAsString(result))

        val title = s"${Messages("tradingpremises.agentname.title")} - ${Messages("summary.tradingpremises")} - ${Messages("title.amls")} - ${Messages("title.gov")}"

        document.title() must be(title)
        document.select("input[type=text]").`val`() must be(empty)
      }

      "display main Summary Page" in new Fixture {

        when(controller.dataCacheConnector.fetch[Seq[TradingPremises]](any())(any(), any(), any()))
          .thenReturn(Future.successful(Some(Seq(TradingPremises(agentName = Some(AgentName("test")))))))

        val result = controller.get(1)(request)
        status(result) must be(OK)

        val document = Jsoup.parse(contentAsString(result))

        val title = s"${Messages("tradingpremises.agentname.title")} - ${Messages("summary.tradingpremises")} - ${Messages("title.amls")} - ${Messages("title.gov")}"

        document.title() must be(title)
        document.select("input[type=text]").`val`() must be("test")
      }
      "respond with NOT_FOUND" when {
        "there is no data at all at that index" in new Fixture {
          when(controller.dataCacheConnector.fetch[Seq[TradingPremises]](any())(any(), any(), any()))
            .thenReturn(Future.successful(None))

          val result = controller.get(1)(request)

          status(result) must be(NOT_FOUND)
        }
      }
    }

    "post is called" must {
      "respond with NOT_FOUND" when {
        "there is no data at all at that index" in new Fixture {
          val newRequest = request.withFormUrlEncodedBody(
            "agentName" -> "text",
            "agentDateOfBirth.day" -> "15",
            "agentDateOfBirth.month" -> "2",
            "agentDateOfBirth.year" -> "1956"
          )

          when(mockCacheMap.getEntry[Seq[TradingPremises]](any())(any()))
            .thenReturn(Some(Seq(TradingPremises())))

          when(controller.dataCacheConnector.fetchAll(any[HeaderCarrier], any[AuthContext]))
            .thenReturn(Future.successful(Some(mockCacheMap)))

          val result = controller.post(99)(newRequest)
          status(result) must be(NOT_FOUND)
        }
      }

      "respond with SEE_OTHER" when {
        "edit is false and given valid data" in new Fixture {

          val newRequest = request.withFormUrlEncodedBody(
            "agentName" -> "text",
            "agentDateOfBirth.day" -> "15",
            "agentDateOfBirth.month" -> "2",
            "agentDateOfBirth.year" -> "1956"
          )

          when(mockCacheMap.getEntry[Seq[TradingPremises]](any())(any()))
            .thenReturn(Some(Seq(TradingPremises())))

          when(controller.dataCacheConnector.fetchAll(any[HeaderCarrier], any[AuthContext]))
            .thenReturn(Future.successful(Some(mockCacheMap)))

          val result = controller.post(1)(newRequest)
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(routes.ConfirmAddressController.get(1).url))
        }

        "edit is true and given valid data" in new Fixture {

          val newRequest = request.withFormUrlEncodedBody(
            "agentName" -> "text",
            "agentDateOfBirth.day" -> "15",
            "agentDateOfBirth.month" -> "2",
            "agentDateOfBirth.year" -> "1956"
          )

          when(mockCacheMap.getEntry[Seq[TradingPremises]](any())(any()))
            .thenReturn(Some(Seq(TradingPremises())))

          when(controller.dataCacheConnector.fetchAll(any[HeaderCarrier], any[AuthContext]))
            .thenReturn(Future.successful(Some(mockCacheMap)))

          val result = controller.post(1, true)(newRequest)
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(routes.SummaryController.getIndividual(1).url))

        }
      }

      "respond with BAD_REQUEST" when {
        "given invalid data" in new Fixture {

          val newRequest = request.withFormUrlEncodedBody(
            "agentName" -> "11111111111" * 40
          )

          when(mockCacheMap.getEntry[Seq[TradingPremises]](any())(any()))
            .thenReturn(Some(Seq(TradingPremises())))

          when(controller.dataCacheConnector.fetchAll(any[HeaderCarrier], any[AuthContext]))
            .thenReturn(Future.successful(Some(mockCacheMap)))

          when(controller.dataCacheConnector.save[TradingPremises](any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(emptyCache))

          val result = controller.post(1)(newRequest)
          status(result) must be(BAD_REQUEST)
          contentAsString(result) must include(Messages("error.invalid.tp.agent.name"))

        }

        "given missing mandatory field" in new Fixture {
          val newRequest = request.withFormUrlEncodedBody(
            "agentName" -> " "
          )

          when(mockCacheMap.getEntry[Seq[TradingPremises]](any())(any()))
            .thenReturn(Some(Seq(TradingPremises())))

          when(controller.dataCacheConnector.fetchAll(any[HeaderCarrier], any[AuthContext]))
            .thenReturn(Future.successful(Some(mockCacheMap)))

          val result = controller.post(1)(newRequest)
          status(result) must be(BAD_REQUEST)
          contentAsString(result) must include(Messages("error.required.tp.agent.name"))
        }
      }
      "set the hasChanged flag to true" in new Fixture {

        val newRequest = request.withFormUrlEncodedBody("agentName" -> "text",
          "agentDateOfBirth.day" -> "15",
          "agentDateOfBirth.month" -> "2",
          "agentDateOfBirth.year" -> "1956")

        when(mockCacheMap.getEntry[Seq[TradingPremises]](any())(any()))
          .thenReturn(Some(Seq(tradingPremisesWithHasChangedFalse, TradingPremises())))

        when(controller.dataCacheConnector.fetchAll(any[HeaderCarrier], any[AuthContext]))
          .thenReturn(Future.successful(Some(mockCacheMap)))

        val result = controller.post(1)(newRequest)

        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some(routes.WhereAreTradingPremisesController.get(1, false).url))

        verify(controller.dataCacheConnector).save[Seq[TradingPremises]](
          any(),
          meq(Seq(tradingPremisesWithHasChangedFalse.copy(
            hasChanged = true,
            agentName = Some(AgentName("text",None,Some(new LocalDate(1956,2,15)))),
            agentCompanyDetails = None,
            agentPartnership = None
          ), TradingPremises())))(any(), any(), any())
      }

      "go to the date of change page" when {
        "the agent name has been changed and submission is successful" in new Fixture {

          when(mockCacheMap.getEntry[Seq[TradingPremises]](any())(any()))
            .thenReturn(Some(Seq(tradingPremisesWithHasChangedFalse.copy(lineId = Some(1)))))

          when(controller.dataCacheConnector.fetchAll(any[HeaderCarrier], any[AuthContext]))
            .thenReturn(Future.successful(Some(mockCacheMap)))

          when(controller.dataCacheConnector.save[TradingPremises](any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(emptyCache))

          when(controller.statusService.getStatus(any(), any(), any())) thenReturn Future.successful(SubmissionDecisionApproved)

          val newRequest = request.withFormUrlEncodedBody(
            "agentName" -> "someName",
            "agentDateOfBirth.day" -> "15",
            "agentDateOfBirth.month" -> "2",
            "agentDateOfBirth.year" -> "1956")

          val result = controller.post(1)(newRequest)

          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(routes.AgentNameController.dateOfChange(1).url))
        }
      }

      "return view for Date of Change" in new Fixture {
        val result = controller.dateOfChange(1)(request)
        status(result) must be(OK)
      }

      "handle the date of change form post" when {

        "given valid data for a agent name" in new Fixture {

          val postRequest = request.withFormUrlEncodedBody(
            "dateOfChange.year" -> "2010",
            "dateOfChange.month" -> "10",
            "dateOfChange.day" -> "01"
          )

          val name = AgentName("someName")
          val updatedName= name.copy(dateOfChange = Some(DateOfChange(new LocalDate(2010, 10, 1))))

          val yourPremises = mock[YourTradingPremises]
          when(yourPremises.startDate) thenReturn new Some(new LocalDate(2005, 1, 1))

          val premises = TradingPremises(agentName = Some(name), yourTradingPremises = Some(yourPremises))

          when(controller.dataCacheConnector.fetch[Seq[TradingPremises]](meq(TradingPremises.key))(any(), any(), any()))
            .thenReturn(Future.successful(Some(Seq(premises))))

          when(controller.dataCacheConnector.save[TradingPremises](meq(TradingPremises.key), any[TradingPremises])(any(), any(), any())).
            thenReturn(Future.successful(mock[CacheMap]))

          val result = controller.saveDateOfChange(1)(postRequest)

          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(routes.SummaryController.get().url))

          val captor = ArgumentCaptor.forClass(classOf[Seq[TradingPremises]])
          verify(controller.dataCacheConnector).save[Seq[TradingPremises]](meq(TradingPremises.key), captor.capture())(any(), any(), any())

          captor.getValue.head.agentName match {
            case Some(savedName: AgentName) => savedName must be(updatedName)
          }

        }

        "given a date of change which is before the activity start date" in new Fixture {
          val postRequest = request.withFormUrlEncodedBody(
            "dateOfChange.year" -> "2003",
            "dateOfChange.month" -> "10",
            "dateOfChange.day" -> "01"
          )

          val yourPremises = mock[YourTradingPremises]
          when(yourPremises.startDate) thenReturn Some(new LocalDate(2005, 1, 1))

          val premises = TradingPremises(agentName = Some(AgentName("Trading Name")), yourTradingPremises = Some(yourPremises))

          when(controller.dataCacheConnector.fetch[Seq[TradingPremises]](meq(TradingPremises.key))(any(), any(), any()))
            .thenReturn(Future.successful(Some(Seq(premises))))

          val result = controller.saveDateOfChange(1)(postRequest)

          status(result) must be(BAD_REQUEST)
        }
      }

    }
  }
  val address = Address("1", "2",None,None,"asdfasdf")
  val year =1990
  val month = 2
  val day = 24
  val date = new LocalDate(year, month, day)

  val ytp = YourTradingPremises("tradingName1", address, Some(true), Some(date))
  val ytp1 = YourTradingPremises("tradingName2", address, Some(true), Some(date))
  val ytp2 = YourTradingPremises("tradingName3", address, Some(true), Some(date))
  val ytp3 = YourTradingPremises("tradingName3", address, Some(true), Some(date))


  val businessStructure = SoleProprietor
  val testAgentName = AgentName("test")
  val testAgentCompanyName = AgentCompanyDetails("test", Some("12345678"))
  val testAgentPartnership = AgentPartnership("test")
  val wdbd = WhatDoesYourBusinessDo(
    Set(
      BillPaymentServices,
      EstateAgentBusinessService,
      MoneyServiceBusiness)
  )
  val msbServices = MsbServices(Set(TransmittingMoney, CurrencyExchange))

  val tradingPremisesWithHasChangedFalse = TradingPremises(
    Some(RegisteringAgentPremises(true)),
    Some(ytp),
    Some(businessStructure),
    Some(testAgentName),
    Some(testAgentCompanyName),
    Some(testAgentPartnership),
    Some(wdbd),
    Some(msbServices),
    false
  )
}
