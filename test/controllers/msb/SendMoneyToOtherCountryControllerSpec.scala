package controllers.msb

import connectors.DataCacheConnector
import models.moneyservicebusiness._
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.i18n.Messages
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.AuthorisedFixture

import scala.concurrent.Future

class SendMoneyToOtherCountryControllerSpec extends PlaySpec with OneAppPerSuite with MockitoSugar  {

  trait Fixture extends AuthorisedFixture {
    self =>

    val controller = new SendMoneyToOtherCountryController {
      override val dataCacheConnector: DataCacheConnector = mock[DataCacheConnector]
      override val authConnector: AuthConnector = self.authConnector
    }
  }

  val emptyCache = CacheMap("", Map.empty)

  "SendMoneyToOtherCountryController" must {

    "load the page 'Do you send money to other countries?'" in new Fixture {

      when(controller.dataCacheConnector.fetch[MoneyServiceBusiness](any())
        (any(), any(), any())).thenReturn(Future.successful(None))

      val result = controller.get()(request)
      status(result) must be(OK)
      contentAsString(result) must include(Messages("msb.send.money.title"))
    }

    "load the page 'Do you send money to other countries?' with pre populated data" in new Fixture  {

      when(controller.dataCacheConnector.fetch[MoneyServiceBusiness](any())
        (any(), any(), any())).thenReturn(Future.successful(Some(MoneyServiceBusiness(
        sendMoneyToOtherCountry = Some(SendMoneyToOtherCountry(true))))))

      val result = controller.get()(request)
      status(result) must be(OK)
      contentAsString(result) must include(Messages("msb.send.money.title"))
    }

    "Show error message when user has not filled the mandatory fields" in new Fixture  {

      val newRequest = request.withFormUrlEncodedBody(
      )

      when(controller.dataCacheConnector.fetch[MoneyServiceBusiness](any())
        (any(), any(), any())).thenReturn(Future.successful(None))

      when(controller.dataCacheConnector.save[MoneyServiceBusiness](any(), any())
        (any(), any(), any())).thenReturn(Future.successful(emptyCache))

      val result = controller.post()(newRequest)
      status(result) must be(BAD_REQUEST)
      contentAsString(result) must include (Messages("error.required.msb.send.money"))

    }

    "on valid post where the value is true" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody (
        "money" -> "true"
      )

      val incomingModel = MoneyServiceBusiness()

      val outgoingModel = incomingModel.copy(
        sendMoneyToOtherCountry = Some(SendMoneyToOtherCountry(true))
      )

      when(controller.dataCacheConnector.fetch[MoneyServiceBusiness](eqTo(MoneyServiceBusiness.key))
        (any(), any(), any())).thenReturn(Future.successful(Some(incomingModel)))

      when(controller.dataCacheConnector.save[MoneyServiceBusiness](eqTo(MoneyServiceBusiness.key), eqTo(outgoingModel))
        (any(), any(), any())).thenReturn(Future.successful(emptyCache))

      val result = controller.post()(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(controllers.msb.routes.SendTheLargestAmountsOfMoneyController.get().url))
    }

    "on valid post where the value is false (CE)" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody (
        "money" -> "false"
      )

      val incomingModel = MoneyServiceBusiness(
        msbServices = Some(MsbServices(
          Set(
            TransmittingMoney,
            CurrencyExchange
          )
        ))
      )

      val outgoingModel = incomingModel.copy(
        sendMoneyToOtherCountry = Some(SendMoneyToOtherCountry(false))
      )

      when(controller.dataCacheConnector.fetch[MoneyServiceBusiness](eqTo(MoneyServiceBusiness.key))
        (any(), any(), any())).thenReturn(Future.successful(Some(incomingModel)))

      when(controller.dataCacheConnector.save[MoneyServiceBusiness](eqTo(MoneyServiceBusiness.key), eqTo(outgoingModel))
        (any(), any(), any())).thenReturn(Future.successful(emptyCache))

      val result = controller.post(false)(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(controllers.msb.routes.CETransactionsInNext12MonthsController.get().url))
    }
  }

  "on valid post where the value is false (Non-CE)" in new Fixture {

    val newRequest = request.withFormUrlEncodedBody (
      "money" -> "false"
    )

    val incomingModel = MoneyServiceBusiness()

    val outgoingModel = incomingModel.copy(
      sendMoneyToOtherCountry = Some(SendMoneyToOtherCountry(false))
    )

    when(controller.dataCacheConnector.fetch[MoneyServiceBusiness](eqTo(MoneyServiceBusiness.key))
      (any(), any(), any())).thenReturn(Future.successful(Some(incomingModel)))

    when(controller.dataCacheConnector.save[MoneyServiceBusiness](eqTo(MoneyServiceBusiness.key), eqTo(outgoingModel))
      (any(), any(), any())).thenReturn(Future.successful(emptyCache))

    val result = controller.post(false)(newRequest)
    status(result) must be(SEE_OTHER)
    redirectLocation(result) must be(Some(controllers.msb.routes.SummaryController.get().url))
  }

  "on valid post where the value is true in edit mode" in new Fixture {

    val newRequest = request.withFormUrlEncodedBody (
      "money" -> "true"
    )

    val incomingModel = MoneyServiceBusiness()

    val outgoingModel = incomingModel.copy(
      sendMoneyToOtherCountry = Some(SendMoneyToOtherCountry(true))
    )

    when(controller.dataCacheConnector.fetch[MoneyServiceBusiness](eqTo(MoneyServiceBusiness.key))
      (any(), any(), any())).thenReturn(Future.successful(Some(incomingModel)))

    when(controller.dataCacheConnector.save[MoneyServiceBusiness](eqTo(MoneyServiceBusiness.key), eqTo(outgoingModel))
      (any(), any(), any())).thenReturn(Future.successful(emptyCache))

    val result = controller.post(true)(newRequest)
    status(result) must be(SEE_OTHER)
    redirectLocation(result) must be(Some(controllers.msb.routes.SendTheLargestAmountsOfMoneyController.get(true).url))
  }

  "on valid post where the value is false in edit mode (CE)" in new Fixture {

    val newRequest = request.withFormUrlEncodedBody (
      "money" -> "false"
    )

    val incomingModel = MoneyServiceBusiness(
      msbServices = Some(MsbServices(
        Set(
          CurrencyExchange
        )
      ))
    )

    val outgoingModel = incomingModel.copy(
      sendMoneyToOtherCountry = Some(SendMoneyToOtherCountry(false))
    )

    when(controller.dataCacheConnector.fetch[MoneyServiceBusiness](eqTo(MoneyServiceBusiness.key))
      (any(), any(), any())).thenReturn(Future.successful(Some(incomingModel)))

    when(controller.dataCacheConnector.save[MoneyServiceBusiness](eqTo(MoneyServiceBusiness.key), eqTo(outgoingModel))
      (any(), any(), any())).thenReturn(Future.successful(emptyCache))

    val result = controller.post(true)(newRequest)
    status(result) must be(SEE_OTHER)
    redirectLocation(result) must be(Some(controllers.msb.routes.CETransactionsInNext12MonthsController.get(true).url))
  }

  "on valid post where the value is false in edit mode (Non-CE)" in new Fixture {

    val newRequest = request.withFormUrlEncodedBody (
      "money" -> "false"
    )

    val incomingModel = MoneyServiceBusiness()

    val outgoingModel = incomingModel.copy(
      sendMoneyToOtherCountry = Some(SendMoneyToOtherCountry(false))
    )

    when(controller.dataCacheConnector.fetch[MoneyServiceBusiness](eqTo(MoneyServiceBusiness.key))
      (any(), any(), any())).thenReturn(Future.successful(Some(incomingModel)))

    when(controller.dataCacheConnector.save[MoneyServiceBusiness](eqTo(MoneyServiceBusiness.key), eqTo(outgoingModel))
      (any(), any(), any())).thenReturn(Future.successful(emptyCache))

    val result = controller.post(true)(newRequest)
    status(result) must be(SEE_OTHER)
    redirectLocation(result) must be(Some(controllers.msb.routes.SummaryController.get().url))
  }
}
