package controllers.tradingpremises

import connectors.DataCacheConnector
import models.businessmatching.{BillPaymentServices, EstateAgentBusinessService, MoneyServiceBusiness}
import models.status.{NotCompleted, SubmissionDecisionApproved, SubmissionReady, SubmissionReadyForReview}
import models.tradingpremises._
import org.joda.time.LocalDate
import org.jsoup.Jsoup
import org.mockito.Matchers.{eq => meq, _}
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.i18n.Messages
import play.api.test.Helpers._
import services.{AuthEnrolmentsService, StatusService}
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.{AuthorisedFixture, StatusConstants}

import scala.concurrent.Future

class RemoveTradingPremisesControllerSpec extends PlaySpec with OneAppPerSuite with MockitoSugar {

  trait Fixture extends AuthorisedFixture {
    self =>

    val controller = new RemoveTradingPremisesController {
      override val dataCacheConnector: DataCacheConnector = mock[DataCacheConnector]
      override val statusService: StatusService = mock[StatusService]

      override protected def authConnector: AuthConnector = self.authConnector

      override val authEnrolmentsService: AuthEnrolmentsService = mock[AuthEnrolmentsService]
    }
  }

  "RemoveTradingPremisesController" must {

    val address = Address("1", "2", None, None, "asdfasdf")
    val year = 1990
    val month = 2
    val day = 24
    val date = new LocalDate(year, month, day)

    val ytp = YourTradingPremises("tradingName1", address, true, date)
    val ytp1 = YourTradingPremises("tradingName2", address, true, date)
    val ytp2 = YourTradingPremises("tradingName3", address, true, date)
    val ytp3 = YourTradingPremises("tradingName3", address, true, date)


    val businessStructure = SoleProprietor
    val agentName = AgentName("test")
    val agentCompanyName = AgentCompanyName("test")
    val agentPartnership = AgentPartnership("test")
    val wdbd = WhatDoesYourBusinessDo(
      Set(
        BillPaymentServices,
        EstateAgentBusinessService,
        MoneyServiceBusiness)
    )
    val msbServices = MsbServices(Set(TransmittingMoney, CurrencyExchange))

    val completeModel1 = TradingPremises(
      Some(RegisteringAgentPremises(true)),
      Some(ytp),
      Some(businessStructure),
      Some(agentName),
      Some(agentCompanyName),
      Some(agentPartnership),
      Some(wdbd),
      Some(msbServices)
    )

    val completeModel2 = TradingPremises(
      Some(RegisteringAgentPremises(true)),
      Some(ytp1),
      Some(businessStructure),
      Some(agentName),
      Some(agentCompanyName),
      Some(agentPartnership),
      Some(wdbd),
      Some(msbServices)
    )

    val completeModel3 = TradingPremises(
      Some(RegisteringAgentPremises(true)),
      Some(ytp2),
      Some(businessStructure),
      Some(agentName),
      Some(agentCompanyName),
      Some(agentPartnership),
      Some(wdbd),
      Some(msbServices)
    )

    val completeModel4 = TradingPremises(
      Some(RegisteringAgentPremises(true)),
      Some(ytp3),
      Some(businessStructure),
      Some(agentName),
      Some(agentCompanyName),
      Some(agentPartnership),
      Some(wdbd),
      Some(msbServices)
    )
    val emptyCache = CacheMap("", Map.empty)

    "successfully load remove trading premises page" when {

      "application status is approved" in new Fixture {

        when(controller.dataCacheConnector.fetch[Seq[TradingPremises]](any())(any(), any(), any()))
          .thenReturn(Future.successful(Some(Seq(TradingPremises(None, Some(ytp))))))

        when(controller.statusService.getStatus(any(), any(), any()))
          .thenReturn(Future.successful(SubmissionDecisionApproved))

        val result = controller.get(1, false)(request)

        val contentString = contentAsString(result)

        val document = Jsoup.parse(contentString)
        document.title() must be(Messages("tradingpremises.remove.trading.premises.title"))
        document.getElementById("endDate-day").`val`() must be("")

      }

      "application status is NotCompleted" in new Fixture {

        when(controller.dataCacheConnector.fetch[Seq[TradingPremises]](any())(any(), any(), any()))
          .thenReturn(Future.successful(Some(Seq(TradingPremises(None, Some(ytp))))))

        when(controller.statusService.getStatus(any(), any(), any()))
          .thenReturn(Future.successful(NotCompleted))

        val result = controller.get(1, false)(request)

        val contentString = contentAsString(result)

        val document = Jsoup.parse(contentString)
        document.title() must be(Messages("tradingpremises.remove.trading.premises.title"))
      }
    }

    "successfully load remove trading premises page with no trading name" in new Fixture {

      when(controller.dataCacheConnector.fetch[Seq[TradingPremises]](any())(any(), any(), any()))
        .thenReturn(Future.successful(Some(Seq(TradingPremises(None, None)))))

      when(controller.statusService.getStatus(any(), any(), any()))
        .thenReturn(Future.successful(SubmissionDecisionApproved))

      val result = controller.get(1, false)(request)

      val contentString = contentAsString(result)

      val document = Jsoup.parse(contentString)
      document.title() must be(Messages("tradingpremises.remove.trading.premises.title"))
    }

    "respond with NOT_FOUND" when {
      "there is no data at all at that index" in new Fixture {
        when(controller.dataCacheConnector.fetch[Seq[TradingPremises]](any())(any(), any(), any()))
          .thenReturn(Future.successful(None))
        when(controller.statusService.getStatus(any(), any(), any()))
          .thenReturn(Future.successful(NotCompleted))

        val result = controller.get(1, false)(request)

        status(result) must be(NOT_FOUND)
      }
    }

  }
  it when {
    "remove is called" must {
      "respond with SEE_OTHER" when {
        "removing a trading premises from an application with status NotCompleted" in new Fixture {

          val emptyCache = CacheMap("", Map.empty)

          when(controller.authEnrolmentsService.amlsRegistrationNumber(any(), any(), any()))
            .thenReturn(Future.successful(None))
          when(controller.dataCacheConnector.fetch[Seq[TradingPremises]](any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(tradingPremisesList)))
          when(controller.dataCacheConnector.save[Seq[TradingPremises]](any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(emptyCache))
          when(controller.statusService.getStatus(any(), any(), any()))
            .thenReturn(Future.successful(NotCompleted))

          val result = controller.remove(1, false, "John Envy Doe")(request)
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(controllers.tradingpremises.routes.SummaryController.get().url))

          verify(controller.dataCacheConnector).save[Seq[TradingPremises]](any(), meq(Seq(
            completeTradingPremises2,
            completeTradingPremises3
          )))(any(), any(), any())
        }
        "removing a trading premises from an application with status SubmissionReady" in new Fixture {

          val emptyCache = CacheMap("", Map.empty)

          when(controller.authEnrolmentsService.amlsRegistrationNumber(any(), any(), any()))
            .thenReturn(Future.successful(None))
          when(controller.dataCacheConnector.fetch[Seq[TradingPremises]](any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(tradingPremisesList)))
          when(controller.dataCacheConnector.save[Seq[TradingPremises]](any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(emptyCache))
          when(controller.statusService.getStatus(any(), any(), any()))
            .thenReturn(Future.successful(SubmissionReady))

          val result = controller.remove(1, false, "John Envy Doe")(request)
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(controllers.tradingpremises.routes.SummaryController.get().url))

          verify(controller.dataCacheConnector).save[Seq[TradingPremises]](any(), meq(Seq(
            completeTradingPremises2,
            completeTradingPremises3
          )))(any(), any(), any())
        }

        "removing a trading premises from an application with status SubmissionReadyForReview" in new Fixture {

          val emptyCache = CacheMap("", Map.empty)

          when(controller.authEnrolmentsService.amlsRegistrationNumber(any(), any(), any()))
            .thenReturn(Future.successful(Some("RegNo")))
          when(controller.dataCacheConnector.fetch[Seq[TradingPremises]](any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(tradingPremisesList)))
          when(controller.dataCacheConnector.save[Seq[TradingPremises]](any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(emptyCache))
          when(controller.statusService.getStatus(any(), any(), any()))
            .thenReturn(Future.successful(SubmissionReadyForReview))


          val result = controller.remove(1, false, "John Envy Doe")(request)
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(controllers.tradingpremises.routes.SummaryController.get().url))

          verify(controller.dataCacheConnector).save[Seq[TradingPremises]](any(), meq(Seq(
            completeTradingPremises1.copy(status = Some(StatusConstants.Deleted), hasChanged = true),
            completeTradingPremises2,
            completeTradingPremises3
          )))(any(), any(), any())
        }

        "removing a trading premises from an application with status SubmissionDecisionApproved" in new Fixture {

          val emptyCache = CacheMap("", Map.empty)
          val newRequest = request.withFormUrlEncodedBody(
            "endDate.day" -> "1",
            "endDate.month" -> "1",
            "endDate.year" -> "1990"
          )

          when(controller.authEnrolmentsService.amlsRegistrationNumber(any(), any(), any()))
            .thenReturn(Future.successful(Some("RegNo")))
          when(controller.dataCacheConnector.fetch[Seq[TradingPremises]](any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(tradingPremisesList)))
          when(controller.dataCacheConnector.save[Seq[TradingPremises]](any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(emptyCache))
          when(controller.statusService.getStatus(any(), any(), any()))
            .thenReturn(Future.successful(SubmissionDecisionApproved))


          val result = controller.remove(1, false, "John Envy Doe")(newRequest)
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(controllers.tradingpremises.routes.SummaryController.get().url))

          verify(controller.dataCacheConnector).save[Seq[TradingPremises]](any(), meq(Seq(
            completeTradingPremises1.copy(
              status = Some(StatusConstants.Deleted),
              hasChanged = true,
              endDate = Some(ActivityEndDate(new LocalDate(1990, 1, 1)))),
            completeTradingPremises2,
            completeTradingPremises3
          )))(any(), any(), any())
        }
      }

      "respond with BAD_REQUEST" when {
        "removing a trading premises from an application with no date" in new Fixture {
          val emptyCache = CacheMap("", Map.empty)

          val newRequest = request.withFormUrlEncodedBody(
            "endDate.day" -> "",
            "endDate.month" -> "",
            "endDate.year" -> ""
          )

          when(controller.dataCacheConnector.fetch[Seq[TradingPremises]](any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(tradingPremisesList)))
          when(controller.dataCacheConnector.save[Seq[TradingPremises]](any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(emptyCache))
          when(controller.statusService.getStatus(any(), any(), any()))
            .thenReturn(Future.successful(SubmissionDecisionApproved))

          val result = controller.remove(1, true, "trading Name")(newRequest)
          status(result) must be(BAD_REQUEST)
          contentAsString(result) must include(Messages("error.expected.jodadate.format"))

        }

        "removing a trading premises from an application with future date" in new Fixture {
          val emptyCache = CacheMap("", Map.empty)

          val newRequest = request.withFormUrlEncodedBody(
            "endDate.day" -> "15",
            "endDate.month" -> "1",
            "endDate.year" -> "2020"
          )

          when(controller.dataCacheConnector.fetch[Seq[TradingPremises]](any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(tradingPremisesList)))
          when(controller.dataCacheConnector.save[Seq[TradingPremises]](any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(emptyCache))
          when(controller.statusService.getStatus(any(), any(), any()))
            .thenReturn(Future.successful(SubmissionDecisionApproved))

          val result = controller.remove(1, true, "trading Name")(newRequest)
          status(result) must be(BAD_REQUEST)
          contentAsString(result) must include(Messages("error.future.date"))

        }
      }

    }
  }

  val ytp = YourTradingPremises(
    "foo",
    Address(
      "1",
      "2",
      None,
      None,
      "asdfasdf"
    ),
    true,
    new LocalDate(1990, 2, 24)
  )

  val businessStructure = SoleProprietor
  val agentName = AgentName("test")
  val agentCompanyName = AgentCompanyName("test")
  val agentPartnership = AgentPartnership("test")
  val wdbd = WhatDoesYourBusinessDo(
    Set(
      BillPaymentServices,
      EstateAgentBusinessService,
      MoneyServiceBusiness)
  )
  val msbServices = MsbServices(Set(TransmittingMoney, CurrencyExchange))
  val completeTradingPremises1 = TradingPremises(
    Some(RegisteringAgentPremises(true)),
    Some(ytp),
    Some(businessStructure),
    Some(agentName),
    Some(agentCompanyName),
    Some(agentPartnership),
    Some(wdbd),
    Some(msbServices),
    false,
    Some(123456),
    Some("Added"),
    Some(ActivityEndDate(new LocalDate(1999, 1, 1)))
  )
  val completeTradingPremises2 = TradingPremises(
    Some(RegisteringAgentPremises(true)),
    Some(ytp),
    Some(businessStructure),
    Some(agentName),
    Some(agentCompanyName),
    Some(agentPartnership),
    Some(wdbd),
    Some(msbServices),
    false,
    Some(123456),
    Some("Added"),
    Some(ActivityEndDate(new LocalDate(1999, 1, 1)))
  )
  val completeTradingPremises3 = TradingPremises(
    Some(RegisteringAgentPremises(true)),
    Some(ytp),
    Some(businessStructure),
    Some(agentName),
    Some(agentCompanyName),
    Some(agentPartnership),
    Some(wdbd),
    Some(msbServices),
    false,
    Some(123456),
    Some("Added"),
    Some(ActivityEndDate(new LocalDate(1999, 1, 1)))
  )

  val tradingPremisesList = Seq(completeTradingPremises1, completeTradingPremises2, completeTradingPremises3)
}
