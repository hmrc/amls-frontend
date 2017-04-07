package controllers.renewal

import connectors.DataCacheConnector
import models.Country
import models.renewal.{Renewal, SendTheLargestAmountsOfMoney}
import models.status.{NotCompleted, SubmissionDecisionApproved}
import org.jsoup.Jsoup
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatest.concurrent.{IntegrationPatience, PatienceConfiguration}
import org.scalatest.mock.MockitoSugar
import play.api.i18n.Messages
import play.api.test.Helpers._
import services.{RenewalService, StatusService}
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.{AuthorisedFixture, GenericTestHelper}

import scala.concurrent.Future

class SendTheLargestAmountsOfMoneyControllerSpec extends GenericTestHelper with MockitoSugar with PatienceConfiguration with IntegrationPatience {

  trait Fixture extends AuthorisedFixture {
    self =>
    val request = addToken(authRequest)

    val mockCacheMap = mock[CacheMap]

    val emptyCache = CacheMap("", Map.empty)

    lazy val mockDataCacheConnector = mock[DataCacheConnector]
    lazy val mockStatusService = mock[StatusService]
    lazy val mockRenewalService = mock[RenewalService]

    val controller = new MsbSendTheLargestAmountsOfMoneyController(
      dataCacheConnector = mockDataCacheConnector,
      authConnector = self.authConnector,
      renewalService = mockRenewalService
    )
  }

  val emptyCache = CacheMap("", Map.empty)

  "SendTheLargestAmountsOfMoneyController" when {

    "get is called" must {
      "load the 'Where to Send The Largest Amounts Of Money' page" in new Fixture {

        when(controller.dataCacheConnector.fetch[Renewal](any())(any(), any(), any()))
          .thenReturn(Future.successful(None))

        val result = controller.get()(request)
        status(result) must be(OK)

        val document = Jsoup.parse(contentAsString(result))
        document.title() must be(Messages("msb.send.the.largest.amounts.of.money.title") + " - " + Messages("summary.renewal") + " - " + Messages("title.amls") + " - " + Messages("title.gov"))
      }

      "pre-populate the 'Where to Send The Largest Amounts Of Money' Page" in new Fixture {

        when(controller.dataCacheConnector.fetch[Renewal](any())(any(), any(), any()))
          .thenReturn(Future.successful(Some(
            Renewal(sendTheLargestAmountsOfMoney = Some(SendTheLargestAmountsOfMoney(Country("United Kingdom", "GB"))))
          )))

        val result = controller.get()(request)
        status(result) must be(OK)

        val document = Jsoup.parse(contentAsString(result))
        document.select("select[name=country_1] > option[value=GB]").hasAttr("selected") must be(true)

      }
    }

    "post is called" when {
      "edit is false" must {
        "redirect to the MostTransactionsController with valid data" in new Fixture {

          val newRequest = request.withFormUrlEncodedBody(
            "country_1" -> "GS"
          )

          when(controller.dataCacheConnector.fetch[Renewal](any())(any(), any(), any()))
            .thenReturn(Future.successful(None))

          when(mockRenewalService.updateRenewal(any())(any(), any(), any()))
            .thenReturn(Future.successful(emptyCache))

          val result = controller.post()(newRequest)
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(routes.MsbMostTransactionsController.get().url))
        }
      }

      "edit is true" must {
        "redirect to the SummaryController" in new Fixture {

          val newRequest = request.withFormUrlEncodedBody(
            "country_1" -> "GB"
          )

          when(controller.dataCacheConnector.fetch[Renewal](any())(any(), any(), any()))
            .thenReturn(Future.successful(None))

          when(mockRenewalService.updateRenewal(any())(any(), any(), any()))
            .thenReturn(Future.successful(emptyCache))

          val result = controller.post(true)(newRequest)
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(routes.SummaryController.get().url))
        }
      }

      "given invalid data, must respond with BAD_REQUEST" in new Fixture {

        val newRequest = request.withFormUrlEncodedBody(
          "country_1" -> ""
        )

        val result = controller.post()(newRequest)
        status(result) must be(BAD_REQUEST)

        val document = Jsoup.parse(contentAsString(result))
        document.select("a[href=#country_1]").html() must include(Messages("error.required.country.name"))
      }
    }
  }
}
