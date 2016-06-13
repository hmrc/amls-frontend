package controllers.msb

import connectors.DataCacheConnector
import models.moneyservicebusiness.{TransactionsInNext12Months, MoneyServiceBusiness}
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.i18n.Messages
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.AuthorisedFixture

import scala.concurrent.Future

class TransactionsInNext12MonthsControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar  {

  trait Fixture extends AuthorisedFixture {
    self =>

    val controller = new TransactionsInNext12MonthsController {
      override val dataCacheConnector: DataCacheConnector = mock[DataCacheConnector]
      override val authConnector: AuthConnector = self.authConnector
    }
  }

  val emptyCache = CacheMap("", Map.empty)

  "TransactionsInNext12MonthsController" must {

    "load the page 'How many transactions do you expect in the next 12 months?'" in new Fixture {

      when(controller.dataCacheConnector.fetch[MoneyServiceBusiness](any())
        (any(), any(), any())).thenReturn(Future.successful(None))

      val result = controller.get()(request)
      status(result) must be(OK)
      contentAsString(result) must include(Messages("msb.transactions.expected.title"))
    }

    "load the page 'How many transactions do you expect in the next 12 months?' with pre populated data" in new Fixture  {

      when(controller.dataCacheConnector.fetch[MoneyServiceBusiness](any())
        (any(), any(), any())).thenReturn(Future.successful(Some(MoneyServiceBusiness(
        transactionsInNext12Months = Some(TransactionsInNext12Months("12345678963"))))))

      val result = controller.get()(request)
      status(result) must be(OK)
      contentAsString(result) must include("12345678963")

    }

    "Show error message when user has not filled the mandatory fields" in new Fixture  {

      val newRequest = request.withFormUrlEncodedBody(
        "txnAmount" -> ""
      )

      when(controller.dataCacheConnector.fetch[MoneyServiceBusiness](any())
        (any(), any(), any())).thenReturn(Future.successful(None))

      when(controller.dataCacheConnector.save[MoneyServiceBusiness](any(), any())
        (any(), any(), any())).thenReturn(Future.successful(emptyCache))

      val result = controller.post()(newRequest)
      status(result) must be(BAD_REQUEST)
      contentAsString(result) must include (Messages("error.required.msb.transactions.in.12months"))

    }

    "Successfully save data in save4later and navigate to Next page" in new Fixture {
      val newRequest = request.withFormUrlEncodedBody (
        "txnAmount" -> "12345678963"
      )

      when(controller.dataCacheConnector.fetch[MoneyServiceBusiness](any())
        (any(), any(), any())).thenReturn(Future.successful(None))

      when(controller.dataCacheConnector.save[MoneyServiceBusiness](any(), any())
        (any(), any(), any())).thenReturn(Future.successful(emptyCache))

      val result = controller.post()(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(controllers.msb.routes.SendMoneyToOtherCountryController.get().url))
    }

    "Successfully save data in save4later and navigate to Summary page in edit mode" in new Fixture {
      val newRequest = request.withFormUrlEncodedBody (
        "txnAmount" -> "12345678963"
      )

      when(controller.dataCacheConnector.fetch[MoneyServiceBusiness](any())
        (any(), any(), any())).thenReturn(Future.successful(None))

      when(controller.dataCacheConnector.save[MoneyServiceBusiness](any(), any())
        (any(), any(), any())).thenReturn(Future.successful(emptyCache))

      val result = controller.post(true)(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(controllers.msb.routes.SummaryController.get().url))
    }
  }
}
