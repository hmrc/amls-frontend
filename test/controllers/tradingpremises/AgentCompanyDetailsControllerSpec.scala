package controllers.tradingpremises

import connectors.DataCacheConnector
import models.businessmatching.{MoneyServiceBusiness, EstateAgentBusinessService, BillPaymentServices}
import models.tradingpremises._
import org.joda.time.LocalDate
import org.jsoup.Jsoup
import org.mockito.Mockito._
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.i18n.Messages
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.AuthorisedFixture
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.mockito.Matchers.{eq => meq, _}


import scala.concurrent.Future

class AgentCompanyDetailsControllerSpec extends PlaySpec with OneAppPerSuite with MockitoSugar with ScalaFutures {

  trait Fixture extends AuthorisedFixture {
    self =>

    val controller = new AgentCompanyDetailsController {
      override val dataCacheConnector: DataCacheConnector = mock[DataCacheConnector]
      override val authConnector: AuthConnector = self.authConnector
    }
  }

  "AgentCompanyDetailsController" when {

    val emptyCache = CacheMap("", Map.empty)

    "get is called" must {
      "display agent company name Page" in new Fixture {

        when(controller.dataCacheConnector.fetch[Seq[TradingPremises]](any())(any(), any(), any()))
          .thenReturn(Future.successful(Some(Seq(TradingPremises()))))

        val result = controller.get(1)(request)
        status(result) must be(OK)

      }

      "display saved content" in new Fixture {

        when(controller.dataCacheConnector.fetch[Seq[TradingPremises]](any())(any(), any(), any()))
          .thenReturn(Future.successful(Some(Seq(TradingPremises(agentCompanyDetails = Some(AgentCompanyDetails("test", Some("12345678"))))))))

        val result = controller.get(1)(request)
        status(result) must be(OK)

        val document = Jsoup.parse(contentAsString(result))

        document.select("input[type=text]").first().`val`() must be("test")
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
            "agentCompanyName" -> "text",
            "companyRegistrationNumber" -> "12345678"
          )

          when(controller.dataCacheConnector.fetch[Seq[TradingPremises]](any())(any(), any(), any()))
            .thenReturn(Future.successful(None))

          when(controller.dataCacheConnector.save[TradingPremises](any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(emptyCache))

          val result = controller.post(99)(newRequest)
          status(result) must be(NOT_FOUND)
        }
      }
      "respond with SEE_OTHER" when {
        "edit is false and given valid data" in new Fixture {

          val newRequest = request.withFormUrlEncodedBody(
            "agentCompanyName" -> "text",
            "companyRegistrationNumber" -> "12345678"
          )

          when(controller.dataCacheConnector.fetch[Seq[TradingPremises]](any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(Seq(TradingPremises()))))

          when(controller.dataCacheConnector.save[TradingPremises](any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(emptyCache))

          val result = controller.post(1)(newRequest)
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(routes.WhereAreTradingPremisesController.get(1, false).url))
        }

        "edit is true and given valid data" in new Fixture {

          val newRequest = request.withFormUrlEncodedBody(
            "agentCompanyName" -> "text",
            "companyRegistrationNumber" -> "12345678"
          )

          when(controller.dataCacheConnector.fetch[Seq[TradingPremises]](any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(Seq(TradingPremises()))))

          when(controller.dataCacheConnector.save[TradingPremises](any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(emptyCache))

          val result = controller.post(1, true)(newRequest)
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(routes.SummaryController.getIndividual(1).url))

        }
      }

      "respond with BAD_REQUEST" when {
        "given invalid data" in new Fixture {

          val newRequest = request.withFormUrlEncodedBody(
            "agentCompanyName" -> "11111111111" * 40
          )

          when(controller.dataCacheConnector.fetch[TradingPremises](any())(any(), any(), any()))
            .thenReturn(Future.successful(None))

          when(controller.dataCacheConnector.save[TradingPremises](any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(emptyCache))

          val result = controller.post(1)(newRequest)
          status(result) must be(BAD_REQUEST)
          contentAsString(result) must include(Messages("error.invalid.tp.agent.registered.company.name"))

        }

        "given missing mandatory field" in new Fixture {
          val newRequest = request.withFormUrlEncodedBody(
            "agentCompanyName" -> " "
          )

          when(controller.dataCacheConnector.fetch[TradingPremises](any())(any(), any(), any()))
            .thenReturn(Future.successful(None))

          when(controller.dataCacheConnector.save[TradingPremises](any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(emptyCache))

          val result = controller.post(1)(newRequest)
          status(result) must be(BAD_REQUEST)
          contentAsString(result) must include(Messages("error.required.tp.agent.registered.company.name"))
        }
      }

      "set the hasChanged flag to true" in new Fixture {

        val newRequest = request.withFormUrlEncodedBody("agentCompanyName" -> "text", "companyRegistrationNumber" -> "12345678")

        when(controller.dataCacheConnector.fetch[Seq[TradingPremises]](any())(any(), any(), any()))
          .thenReturn(Future.successful(Some(Seq(tradingPremisesWithHasChangedFalse))))

        when(controller.dataCacheConnector.save[TradingPremises](any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(emptyCache))

        val result = controller.post(1)(newRequest)

        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some(routes.WhereAreTradingPremisesController.get(1, false).url))

        verify(controller.dataCacheConnector).save[Seq[TradingPremises]](
          any(),
          meq(Seq(tradingPremisesWithHasChangedFalse.copy(
            hasChanged = true,
            agentName = None,
            agentCompanyDetails = Some(AgentCompanyDetails("text", Some("12345678"))),
            agentPartnership = None
          ))))(any(), any(), any())
      }
    }
  }

  val address = Address("1", "2",None,None,"asdfasdf")
  val year =1990
  val month = 2
  val day = 24
  val date = new LocalDate(year, month, day)

  val ytp = YourTradingPremises("tradingName1", address, true, date)
  val ytp1 = YourTradingPremises("tradingName2", address, true, date)
  val ytp2 = YourTradingPremises("tradingName3", address, true, date)
  val ytp3 = YourTradingPremises("tradingName3", address, true, date)


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
