/*
 * Copyright 2024 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package controllers.renewal

import cats.implicits._
import connectors.DataCacheConnector
import controllers.actions.SuccessfulAuthAction
import forms.renewal.MostTransactionsFormProvider
import models.Country
import models.businessmatching._
import models.businessmatching.BusinessActivity._
import models.businessmatching.BusinessMatchingMsbService._
import models.renewal.{MostTransactions, Renewal}
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc.Result
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Injecting}
import services.RenewalService
import services.cache.Cache
import utils.{AmlsSpec, AutoCompleteServiceMocks}
import views.html.renewal.MostTransactionsView

import scala.concurrent.Future

class MostTransactionsControllerSpec extends AmlsSpec with MockitoSugar with Injecting {

  trait Fixture extends AutoCompleteServiceMocks {
    self =>
    val request = addToken(authRequest)

    val cache: DataCacheConnector = mock[DataCacheConnector]
    val cacheMap                  = mock[Cache]
    val emptyCache                = Cache.empty
    val mockRenewalService        = mock[RenewalService]
    lazy val view                 = inject[MostTransactionsView]
    val controller                = new MostTransactionsController(
      SuccessfulAuthAction,
      ds = commonDependencies,
      self.cache,
      self.mockRenewalService,
      mockAutoComplete,
      cc = mockMcc,
      formProvider = inject[MostTransactionsFormProvider],
      view = view
    )
  }

  trait FormSubmissionFixture extends Fixture {
    def formData(valid: Boolean)    =
      if (valid) "mostTransactionsCountries[0]" -> "GB" else "mostTransactionsCountries[0]" -> ""
    def formRequest(valid: Boolean) =
      FakeRequest(POST, routes.MostTransactionsController.post().url).withFormUrlEncodedBody(formData(valid))

    when(mockRenewalService.getRenewal(any())).thenReturn(Future.successful(None))

    when(mockRenewalService.updateRenewal(any(), any())).thenReturn(Future.successful(emptyCache))

    def post(edit: Boolean = false, valid: Boolean = true)(block: Future[Result] => Unit) =
      block(controller.post(edit)(formRequest(valid)))
  }

  trait RenewalModelFormSubmissionFixture extends FormSubmissionFixture {
    val incomingModel = Renewal()

    val outgoingModel = incomingModel.copy(
      mostTransactions = Some(
        MostTransactions(
          Seq(Country("United Kingdom", "GB"))
        )
      ),
      hasChanged = true
    )

    val newRequest = FakeRequest(POST, routes.MostTransactionsController.post().url).withFormUrlEncodedBody(
      "mostTransactionsCountries[]" -> "GB"
    )

    when(cache.fetchAll(any()))
      .thenReturn(Future.successful(Some(cacheMap)))

    when(cacheMap.getEntry[Renewal](eqTo(Renewal.key))(any()))
      .thenReturn(Some(incomingModel))

    when(cache.save[Renewal](any(), eqTo(Renewal.key), eqTo(outgoingModel))(any()))
      .thenReturn(Future.successful(Cache.empty))

    def setupBusinessMatching(
      activities: Set[BusinessActivity] = Set(),
      msbServices: Set[BusinessMatchingMsbService] = Set()
    ) = when {
      cacheMap.getEntry[BusinessMatching](BusinessMatching.key)
    } thenReturn Some(
      BusinessMatching(
        msbServices = Some(BusinessMatchingMsbServices(msbServices)),
        activities = Some(BusinessActivities(activities))
      )
    )
  }

  "MostTransactionsController" must {

    "show an empty form on get with no data in store" in new Fixture {

      when(cache.fetch[Renewal](any(), eqTo(Renewal.key))(any()))
        .thenReturn(Future.successful(None))

      val result   = controller.get()(request)
      val document = Jsoup.parse(contentAsString(result))

      status(result) mustEqual OK

      document.select("select").size mustEqual 3
      document.select("option[selected]").size mustEqual 0
      document.select(".govuk-error-summary").size mustEqual 0
    }

    "show a prefilled form when there is data in the store" in new Fixture {

      val model = Renewal(
        mostTransactions = Some(
          MostTransactions(
            Seq(
              Country("United Kingdom", "GB"),
              Country("American Samoa", "AS"),
              Country("Algeria", "DZ")
            )
          )
        )
      )

      when(cache.fetch[Renewal](any(), eqTo(Renewal.key))(any()))
        .thenReturn(Future.successful(Some(model)))

      val result   = controller.get()(request)
      val document = Jsoup.parse(contentAsString(result))

      status(result) mustEqual OK

      document.select("select").size mustEqual 3
      document.select("option[selected]").size mustEqual 3
      document.select(".govuk-error-summary").size mustEqual 0
    }

    "return a Bad request with errors on invalid submission" in new Fixture {

      val newRequest = FakeRequest(POST, routes.MostTransactionsController.post().url).withFormUrlEncodedBody(
        "mostTransactionsCountries[0]" -> "GBasdadsdas"
      )

      val result   = controller.post()(newRequest)
      val document = Jsoup.parse(contentAsString(result))

      status(result) mustEqual BAD_REQUEST

      document.select("select").size mustEqual 3
      document.select("option[selected]").size mustEqual 0
      document.select(".govuk-error-summary").size mustEqual 1
    }

    "on valid submission" when {
      "edit false" must {
        "go to CETransactionsInLast12MonthsController" when {
          "msb includes CE" in new RenewalModelFormSubmissionFixture {
            setupBusinessMatching(msbServices = Set(CurrencyExchange))

            post() { result =>
              status(result) mustBe SEE_OTHER
              redirectLocation(result) mustEqual routes.CETransactionsInLast12MonthsController.get().url.some
            }
          }

          "msb includes CE and FX" in new RenewalModelFormSubmissionFixture {
            setupBusinessMatching(msbServices = Set(CurrencyExchange, ForeignExchange))

            post() { result =>
              status(result) mustBe SEE_OTHER
              redirectLocation(result) mustEqual routes.CETransactionsInLast12MonthsController.get().url.some
            }

          }
        }

        "go to FXTransactionsInLast12MonthsController" when {
          "msb includes FX and not CE" in new RenewalModelFormSubmissionFixture {
            setupBusinessMatching(msbServices = Set(ForeignExchange))

            post() { result =>
              status(result) mustBe SEE_OTHER
              redirectLocation(result) mustEqual routes.FXTransactionsInLast12MonthsController.get().url.some
            }

          }
        }

        "go to PercentageOfCashPaymentOver15000Controller" when {
          "activities include both hvd AND asp" in new RenewalModelFormSubmissionFixture {
            setupBusinessMatching(Set(HighValueDealing, AccountancyServices))

            post() { result =>
              status(result) mustBe SEE_OTHER
              redirectLocation(result) mustEqual routes.PercentageOfCashPaymentOver15000Controller.get().url.some
            }
          }
        }
        "go to the CustomersOutsideIsUKController" when {
          "activities dot not include ASP and HVD however includes hvd" in new RenewalModelFormSubmissionFixture {
            setupBusinessMatching(Set(HighValueDealing))

            post() { result =>
              status(result) mustBe SEE_OTHER
              redirectLocation(result) mustEqual routes.CustomersOutsideIsUKController.get().url.some
            }
          }

          "activities dot not include ASP and HVD however includes ASP" in new RenewalModelFormSubmissionFixture {
            setupBusinessMatching(Set(AccountancyServices))

            post() { result =>
              status(result) mustBe SEE_OTHER
              redirectLocation(result) mustEqual routes.CustomersOutsideIsUKController.get().url.some
            }
          }

        }
        "go to SummaryController" when {
          "msb does not include CE or FX" in new RenewalModelFormSubmissionFixture {
            setupBusinessMatching(msbServices = Set(ChequeCashingScrapMetal))

            post() { result =>
              status(result) mustBe SEE_OTHER
              redirectLocation(result) mustEqual routes.SummaryController.get.url.some
            }
          }
        }

      }

      "edit is true" must {
        "go to SummaryController" in new RenewalModelFormSubmissionFixture {
          setupBusinessMatching(msbServices =
            Set(CurrencyExchange, ForeignExchange, TransmittingMoney, ChequeCashingScrapMetal)
          )

          post(edit = true) { result =>
            status(result) mustBe SEE_OTHER
            redirectLocation(result) mustEqual routes.SummaryController.get.url.some
          }
        }
      }
    }

    "throw exception when Msb services in Business Matching returns none" in new FormSubmissionFixture {
      val newRequest = FakeRequest(POST, routes.MostTransactionsController.post().url).withFormUrlEncodedBody(
        "mostTransactionsCountries[]" -> "GB"
      )

      val incomingModel = Renewal()

      when(cache.fetchAll(any()))
        .thenReturn(Future.successful(Some(cacheMap)))

      when(cacheMap.getEntry[BusinessMatching](BusinessMatching.key))
        .thenReturn(None)

      when(cacheMap.getEntry[Renewal](eqTo(Renewal.key))(any()))
        .thenReturn(Some(incomingModel))

      when(cache.save[Renewal](any(), eqTo(Renewal.key), any())(any())).thenReturn(Future.successful(Cache.empty))

      a[Exception] must be thrownBy {
        ScalaFutures.whenReady(controller.post(true)(newRequest))(x => x)
      }
    }
  }
}
