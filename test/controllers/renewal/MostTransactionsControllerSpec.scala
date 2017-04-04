package controllers.renewal

import connectors.DataCacheConnector
import models.Country
import models.businessmatching._
import models.moneyservicebusiness.{MoneyServiceBusiness, _}
import models.status.{NotCompleted, SubmissionDecisionApproved}
import org.jsoup.Jsoup
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import play.api.test.Helpers._
import services.StatusService
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.{AuthorisedFixture, GenericTestHelper}

import scala.concurrent.Future

class MostTransactionsControllerSpec extends GenericTestHelper with MockitoSugar {

  trait Fixture extends AuthorisedFixture {
    self => val request = addToken(authRequest)

    val cache: DataCacheConnector = mock[DataCacheConnector]
    val cacheMap = mock[CacheMap]
    val controller = new MostTransactionsController(self.authConnector, self.cache)(mock[StatusService])
  }

  "MostTransactionsController" must {

    "show an empty form on get with no data in store" in new Fixture {

      when(controller.statusService.getStatus(any(), any(), any()))
        .thenReturn(Future.successful(NotCompleted))

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

      when(controller.statusService.getStatus(any(), any(), any()))
        .thenReturn(Future.successful(NotCompleted))

      when(cache.fetch[MoneyServiceBusiness](eqTo(MoneyServiceBusiness.key))(any(), any(), any()))
        .thenReturn(Future.successful(Some(model)))

      val result = controller.get()(request)
      val document = Jsoup.parse(contentAsString(result))

      status(result) mustEqual OK

      document.select("select").size mustEqual 3
      document.select("option[selected]").size mustEqual 3
      document.select(".amls-error-summary").size mustEqual 0
    }

    "redirect to Page not found" when {
      "application is in variation mode" in new Fixture {

        when(controller.statusService.getStatus(any(), any(), any()))
          .thenReturn(Future.successful(SubmissionDecisionApproved))

        val result = controller.get()(request)
        status(result) must be(NOT_FOUND)
      }
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

      val msbServices = Some(
        MsbServices(
          Set(
            CurrencyExchange
          )
        )
      )
      val incomingModel = MoneyServiceBusiness()

      val outgoingModel = incomingModel.copy(
        mostTransactions = Some(
          MostTransactions(
            Seq(Country("United Kingdom", "GB"))
          )
        ), hasChanged = true
      )

      val newRequest = request.withFormUrlEncodedBody(
        "mostTransactionsCountries[]" -> "GB"
      )

      when(cache.fetchAll(any(), any()))
        .thenReturn(Future.successful(Some(cacheMap)))

      when(cacheMap.getEntry[MoneyServiceBusiness](eqTo(MoneyServiceBusiness.key))(any()))
        .thenReturn(Some(incomingModel))

      when(cacheMap.getEntry[BusinessMatching](BusinessMatching.key))
        .thenReturn(Some(BusinessMatching(msbServices = msbServices)))

      when(cache.save[MoneyServiceBusiness](eqTo(MoneyServiceBusiness.key), eqTo(outgoingModel))(any(), any(), any()))
        .thenReturn(Future.successful(new CacheMap("", Map.empty)))

      val result = controller.post(edit = false)(newRequest)

      status(result) mustEqual SEE_OTHER
      redirectLocation(result) mustEqual Some(routes.SummaryController.get().url)
    }

    "on valid submission (no edit) (non-CE)" in new Fixture {

      val incomingModel = MoneyServiceBusiness()

      val outgoingModel = incomingModel.copy(
        mostTransactions = Some(
          MostTransactions(
            Seq(Country("United Kingdom", "GB"))
          )
        ), hasChanged = true
      )
      val msbServices = Some(
        MsbServices(
          Set(
            ChequeCashingScrapMetal
          )
        )
      )
      val newRequest = request.withFormUrlEncodedBody(
        "mostTransactionsCountries[]" -> "GB"
      )
      when(cache.fetchAll(any(), any()))
        .thenReturn(Future.successful(Some(cacheMap)))

      when(cacheMap.getEntry[MoneyServiceBusiness](eqTo(MoneyServiceBusiness.key))(any()))
        .thenReturn(Some(incomingModel))

      when(cacheMap.getEntry[BusinessMatching](BusinessMatching.key))
        .thenReturn(Some(BusinessMatching(msbServices = msbServices)))

      when(cache.save[MoneyServiceBusiness](eqTo(MoneyServiceBusiness.key), eqTo(outgoingModel))(any(), any(), any()))
        .thenReturn(Future.successful(new CacheMap("", Map.empty)))

      val result = controller.post(edit = false)(newRequest)

      status(result) mustEqual SEE_OTHER
      redirectLocation(result) mustEqual Some(routes.SummaryController.get().url)
    }

    "return a redirect to the summary page on valid submission where the next page data exists (edit) (CE)" in new Fixture {

      val msbServices = Some(
        MsbServices(
          Set(
            CurrencyExchange
          )
        )
      )

      val incomingModel = MoneyServiceBusiness(
        ceTransactionsInNext12Months = Some(CETransactionsInNext12Months(
          "1223131"
        ))
      )

      val outgoingModel = MoneyServiceBusiness(
        ceTransactionsInNext12Months = Some(CETransactionsInNext12Months(
          "1223131"
        )),
        mostTransactions = Some(
          MostTransactions(
            Seq(Country("United Kingdom", "GB"))
          )
        ), hasChanged = true
      )

      val newRequest = request.withFormUrlEncodedBody(
        "mostTransactionsCountries[]" -> "GB"
      )

      when(cache.fetchAll(any(), any()))
        .thenReturn(Future.successful(Some(cacheMap)))

      when(cacheMap.getEntry[MoneyServiceBusiness](eqTo(MoneyServiceBusiness.key))(any()))
        .thenReturn(Some(incomingModel))

      when(cacheMap.getEntry[BusinessMatching](BusinessMatching.key))
        .thenReturn(Some(BusinessMatching(msbServices = msbServices)))

      when(cache.save[MoneyServiceBusiness](eqTo(MoneyServiceBusiness.key), eqTo(outgoingModel))(any(), any(), any()))
        .thenReturn(Future.successful(new CacheMap("", Map.empty)))

      val result = controller.post(edit = true)(newRequest)

      status(result) mustEqual SEE_OTHER
      redirectLocation(result) mustEqual Some(routes.SummaryController.get().url)
    }

    "return a redirect on valid submission where the next page data doesn't exist (edit) (CE)" in new Fixture {
      val msbServices = Some(
        MsbServices(
          Set(
            CurrencyExchange
          )
        )
      )
      val incomingModel = MoneyServiceBusiness()

      val outgoingModel = incomingModel.copy(
        mostTransactions = Some(
          MostTransactions(
            Seq(Country("United Kingdom", "GB"))
          )
        ), hasChanged = true
      )

      val newRequest = request.withFormUrlEncodedBody(
        "mostTransactionsCountries[0]" -> "GB"
      )

      when(cache.fetchAll(any(), any()))
        .thenReturn(Future.successful(Some(cacheMap)))
      when(cacheMap.getEntry[BusinessMatching](BusinessMatching.key))
        .thenReturn(Some(BusinessMatching(msbServices = msbServices)))
      when(cacheMap.getEntry[MoneyServiceBusiness](eqTo(MoneyServiceBusiness.key))(any()))
        .thenReturn(Some(incomingModel))
      when(cache.save[MoneyServiceBusiness](eqTo(MoneyServiceBusiness.key), eqTo(outgoingModel))(any(), any(), any()))
        .thenReturn(Future.successful(new CacheMap("", Map.empty)))

      val result = controller.post(edit = true)(newRequest)

      status(result) mustEqual SEE_OTHER
      redirectLocation(result) mustEqual Some(routes.SummaryController.get().url)
    }

    "return a redirect to the summary page on valid submission (edit) (non-CE)" in new Fixture {

      val incomingModel = MoneyServiceBusiness()

      val outgoingModel = incomingModel.copy(
        mostTransactions = Some(
          MostTransactions(
            Seq(Country("United Kingdom", "GB"))
          )
        ), hasChanged = true
      )

      val newRequest = request.withFormUrlEncodedBody(
        "mostTransactionsCountries[]" -> "GB"
      )
      val msbServices = Some(
        MsbServices(
          Set(
            ChequeCashingScrapMetal
          )
        )
      )
      when(cache.fetchAll(any(), any()))
        .thenReturn(Future.successful(Some(cacheMap)))
      when(cacheMap.getEntry[BusinessMatching](BusinessMatching.key))
        .thenReturn(Some(BusinessMatching(msbServices = msbServices)))
      when(cacheMap.getEntry[MoneyServiceBusiness](eqTo(MoneyServiceBusiness.key))(any()))
        .thenReturn(Some(incomingModel))
      when(cache.save[MoneyServiceBusiness](eqTo(MoneyServiceBusiness.key), eqTo(outgoingModel))(any(), any(), any()))
        .thenReturn(Future.successful(new CacheMap("", Map.empty)))

      val result = controller.post(edit = true)(newRequest)

      status(result) mustEqual SEE_OTHER
      redirectLocation(result) mustEqual Some(routes.SummaryController.get().url)
    }
  }

  "throw exception when Msb services in Business Matching returns none" in new Fixture {

    val newRequest = request.withFormUrlEncodedBody(
      "mostTransactionsCountries[]" -> "GB"
    )

    val incomingModel = MoneyServiceBusiness()

    val outgoingModel = incomingModel.copy(
      sendMoneyToOtherCountry = Some(SendMoneyToOtherCountry(false)),
      hasChanged = true
    )

    when(cache.fetchAll(any(), any()))
      .thenReturn(Future.successful(Some(cacheMap)))

    when(cacheMap.getEntry[BusinessMatching](BusinessMatching.key))
      .thenReturn(None)

    when(cacheMap.getEntry[MoneyServiceBusiness](eqTo(MoneyServiceBusiness.key))(any()))
      .thenReturn(Some(incomingModel))

    when(cache.save[MoneyServiceBusiness](eqTo(MoneyServiceBusiness.key), eqTo(outgoingModel))
      (any(), any(), any())).thenReturn(Future.successful(new CacheMap("", Map.empty)))


    a[Exception] must be thrownBy {
      ScalaFutures.whenReady(controller.post(true)(newRequest)) { x => x }
    }
  }
}
