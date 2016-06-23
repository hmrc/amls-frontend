package controllers.msb

import connectors.DataCacheConnector
import models.Country
import models.moneyservicebusiness._
import org.jsoup.Jsoup
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.AuthorisedFixture
import org.mockito.Mockito._
import org.mockito.Matchers.{eq => eqTo, _}
import org.scalatest.mock.MockitoSugar
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap

import scala.concurrent.Future

class MostTransactionsControllerSpec extends PlaySpec with MockitoSugar with OneServerPerSuite {

  trait Fixture extends AuthorisedFixture {
    self =>

    val cache: DataCacheConnector = mock[DataCacheConnector]

    val controller = new MostTransactionsController {
      override val cache: DataCacheConnector = self.cache
      override protected def authConnector: AuthConnector = self.authConnector
    }
  }

  "MostTransactionsController" must {

    "show an empty form on get with no data in store" in new Fixture {

      when(cache.fetch[MoneyServiceBusiness](eqTo(MoneyServiceBusiness.key))(any(), any(), any()))
        .thenReturn(Future.successful(None))

      val result = controller.get()(request)
      val document = Jsoup.parse(contentAsString(result))

      status(result) mustEqual OK

      document.select("select").size mustEqual 3
      document.select("option[selected]").size mustEqual 0
      document.select(".amls-error-summary").size mustEqual 0
    }

    "show a prefilled form when there is data in the store" in new Fixture {

      val model = MoneyServiceBusiness(
        mostTransactions = Some(
          MostTransactions(
            models.countries.take(3)
          )
        )
      )

      when(cache.fetch[MoneyServiceBusiness](eqTo(MoneyServiceBusiness.key))(any(), any(), any()))
        .thenReturn(Future.successful(Some(model)))

      val result = controller.get()(request)
      val document = Jsoup.parse(contentAsString(result))

      status(result) mustEqual OK

      document.select("select").size mustEqual 3
      document.select("option[selected]").size mustEqual 3
      document.select(".amls-error-summary").size mustEqual 0
    }

    "return a Bad request with errors on invalid submission" in new Fixture {

      val result = controller.post()(request)
      val document = Jsoup.parse(contentAsString(result))

      status(result) mustEqual BAD_REQUEST

      document.select("select").size mustEqual 3
      document.select("option[selected]").size mustEqual 0
      document.select(".amls-error-summary").size mustEqual 1
    }

    "on valid submission (no edit) (CE)" in new Fixture {

      val incomingModel = MoneyServiceBusiness(
        msbServices = Some(MsbServices(
          Set(
            CurrencyExchange
          )
        ))
      )

      val outgoingModel = incomingModel.copy(
        mostTransactions = Some(
          MostTransactions(
            Seq(Country("United Kingdom", "GB"))
          )
        )
      )

      val newRequest = request.withFormUrlEncodedBody(
        "mostTransactionsCountries[]" -> "GB"
      )

      when(cache.fetch[MoneyServiceBusiness](eqTo(MoneyServiceBusiness.key))(any(), any(), any()))
        .thenReturn(Future.successful(Some(incomingModel)))

      when(cache.save[MoneyServiceBusiness](eqTo(MoneyServiceBusiness.key), eqTo(outgoingModel))(any(), any(), any()))
        .thenReturn(Future.successful(new CacheMap("", Map.empty)))

      val result = controller.post(edit = false)(newRequest)

      status(result) mustEqual SEE_OTHER
      redirectLocation(result) mustEqual Some(routes.CETransactionsInNext12MonthsController.get().url)
    }

    "on valid submission (no edit) (non-CE)" in new Fixture {

      val incomingModel = MoneyServiceBusiness()

      val outgoingModel = incomingModel.copy(
        mostTransactions = Some(
          MostTransactions(
            Seq(Country("United Kingdom", "GB"))
          )
        )
      )

      val newRequest = request.withFormUrlEncodedBody(
        "mostTransactionsCountries[]" -> "GB"
      )

      when(cache.fetch[MoneyServiceBusiness](eqTo(MoneyServiceBusiness.key))(any(), any(), any()))
        .thenReturn(Future.successful(None))

      when(cache.save[MoneyServiceBusiness](eqTo(MoneyServiceBusiness.key), eqTo(outgoingModel))(any(), any(), any()))
        .thenReturn(Future.successful(new CacheMap("", Map.empty)))

      val result = controller.post(edit = false)(newRequest)

      status(result) mustEqual SEE_OTHER
      redirectLocation(result) mustEqual Some(routes.SummaryController.get().url)
    }

    "return a redirect to the summary page on valid submission where the next page data exists (edit) (CE)" in new Fixture {

      val incomingModel = MoneyServiceBusiness(
        msbServices = Some(MsbServices(
          Set(
            CurrencyExchange
          )
        )),
        ceTransactionsInNext12Months = Some(CETransactionsInNext12Months(
          ""
        ))
      )

      val outgoingModel = MoneyServiceBusiness(
        mostTransactions = Some(
          MostTransactions(
            Seq(Country("United Kingdom", "GB"))
          )
        )
      )

      val newRequest = request.withFormUrlEncodedBody(
        "mostTransactionsCountries[]" -> "GB"
      )

      when(cache.fetch[MoneyServiceBusiness](eqTo(MoneyServiceBusiness.key))(any(), any(), any()))
        .thenReturn(Future.successful(None))

      when(cache.save[MoneyServiceBusiness](eqTo(MoneyServiceBusiness.key), eqTo(outgoingModel))(any(), any(), any()))
        .thenReturn(Future.successful(new CacheMap("", Map.empty)))

      val result = controller.post(edit = true)(newRequest)

      status(result) mustEqual SEE_OTHER
      redirectLocation(result) mustEqual Some(routes.SummaryController.get().url)
    }

    "return a redirect on valid submission where the next page data doesn't exist (edit) (CE)" in new Fixture {

      val incomingModel = MoneyServiceBusiness(
        msbServices = Some(MsbServices(
          Set(
            CurrencyExchange
          )
        ))
      )

      val outgoingModel = incomingModel.copy(
        mostTransactions = Some(
          MostTransactions(
            Seq(Country("United Kingdom", "GB"))
          )
        )
      )

      val newRequest = request.withFormUrlEncodedBody(
        "mostTransactionsCountries[0]" -> "GB"
      )

      when(cache.fetch[MoneyServiceBusiness](eqTo(MoneyServiceBusiness.key))(any(), any(), any()))
        .thenReturn(Future.successful(Some(incomingModel)))

      when(cache.save[MoneyServiceBusiness](eqTo(MoneyServiceBusiness.key), eqTo(outgoingModel))(any(), any(), any()))
        .thenReturn(Future.successful(new CacheMap("", Map.empty)))

      val result = controller.post(edit = true)(newRequest)

      status(result) mustEqual SEE_OTHER
      redirectLocation(result) mustEqual Some(routes.CETransactionsInNext12MonthsController.get(true).url)
    }

    "return a redirect to the summary page on valid submission (edit) (non-CE)" in new Fixture {

      val incomingModel = MoneyServiceBusiness()

      val outgoingModel = incomingModel.copy(
        mostTransactions = Some(
          MostTransactions(
            Seq(Country("United Kingdom", "GB"))
          )
        )
      )

      val newRequest = request.withFormUrlEncodedBody(
        "mostTransactionsCountries[]" -> "GB"
      )

      when(cache.fetch[MoneyServiceBusiness](eqTo(MoneyServiceBusiness.key))(any(), any(), any()))
        .thenReturn(Future.successful(None))

      when(cache.save[MoneyServiceBusiness](eqTo(MoneyServiceBusiness.key), eqTo(outgoingModel))(any(), any(), any()))
        .thenReturn(Future.successful(new CacheMap("", Map.empty)))

      val result = controller.post(edit = true)(newRequest)

      status(result) mustEqual SEE_OTHER
      redirectLocation(result) mustEqual Some(routes.SummaryController.get().url)
    }
  }
}
