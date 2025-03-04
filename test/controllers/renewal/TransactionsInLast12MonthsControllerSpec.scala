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
import forms.renewal.TransactionsInLast12MonthsFormProvider
import models.Country
import models.businessmatching.BusinessActivity._
import models.businessmatching.BusinessMatchingMsbService.{CurrencyExchange, TransmittingMoney}
import models.businessmatching._
import models.moneyservicebusiness.{MoneyServiceBusiness => moneyServiceBusiness, SendMoneyToOtherCountry}
import models.renewal.{CustomersOutsideUK, Renewal, TransactionsInLast12Months}
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc.Result
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Injecting}
import services.RenewalService
import services.cache.Cache
import utils.AmlsSpec
import views.html.renewal.TransactionsInLast12MonthsView

import scala.concurrent.Future

class TransactionsInLast12MonthsControllerSpec extends AmlsSpec with MockitoSugar with Injecting {

  trait Fixture {
    self =>
    val renewalService         = mock[RenewalService]
    val request                = addToken(authRequest)
    val mockDataCacheConnector = mock[DataCacheConnector]
    lazy val view              = inject[TransactionsInLast12MonthsView]
    lazy val controller        = new TransactionsInLast12MonthsController(
      SuccessfulAuthAction,
      ds = commonDependencies,
      mockDataCacheConnector,
      renewalService,
      cc = mockMcc,
      formProvider = inject[TransactionsInLast12MonthsFormProvider],
      view = view
    )

    when {
      renewalService.getRenewal(any())
    } thenReturn Future.successful(Renewal().some)

    val msbModel                               = moneyServiceBusiness(sendMoneyToOtherCountry = Some(SendMoneyToOtherCountry(true)))
    val msbModelDoNotSendMoneyToOtherCountries =
      moneyServiceBusiness(sendMoneyToOtherCountry = Some(SendMoneyToOtherCountry(false)))
  }

  trait FormSubmissionFixture extends Fixture {
    def formData(valid: Boolean)    = if (valid) { "txnAmount" -> "1500" }
    else { "txnAmount" -> "abc" }
    def formRequest(valid: Boolean) = FakeRequest(POST, routes.TransactionsInLast12MonthsController.post().url)
      .withFormUrlEncodedBody(formData(valid))

    val cache = mock[Cache]

    when {
      renewalService.updateRenewal(any(), any())
    } thenReturn Future.successful(cache)

    when {
      mockDataCacheConnector.fetchAll(any())
    } thenReturn Future.successful(Some(cache))

    def post(edit: Boolean = false, valid: Boolean = true)(block: Future[Result] => Unit) =
      block(controller.post(edit)(formRequest(valid)))
  }

  "Calling the GET action" must {
    "return the correct view" when {
      "edit is false" in new Fixture {
        val result = controller.get()(request)

        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.select("h1").text mustBe messages("renewal.msb.transfers.header")
      }

      "edit is true" in new Fixture {
        val result = controller.get(true)(request)

        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.select("form").first.attr("action") mustBe routes.TransactionsInLast12MonthsController.post(true).url
      }

      "reads the current value from the renewals model" in new Fixture {

        when {
          renewalService.getRenewal(any())
        } thenReturn Future.successful(
          Renewal(transactionsInLast12Months = TransactionsInLast12Months("2500").some).some
        )

        val result = controller.get(true)(request)
        val doc    = Jsoup.parse(contentAsString(result))

        doc.select("input[name=txnAmount]").first.attr("value") mustBe "2500"

        verify(renewalService).getRenewal(any())
      }
    }
  }

  "Calling the POST action" when {
    "posting valid data" when {

      "msb is Money Transfers" must {
        "redirect to SendMoneyToOtherCountriesController" in new FormSubmissionFixture {

          when(cache.getEntry[Renewal](eqTo(Renewal.key))(any()))
            .thenReturn(
              Some(
                Renewal(
                  customersOutsideUK = Some(CustomersOutsideUK(Some(Seq(Country("GB", "GB")))))
                )
              )
            )

          when(cache.getEntry[BusinessMatching](eqTo(BusinessMatching.key))(any()))
            .thenReturn(
              Some(
                BusinessMatching(
                  msbServices = Some(BusinessMatchingMsbServices(Set(TransmittingMoney)))
                )
              )
            )

          when(cache.getEntry[moneyServiceBusiness](eqTo(moneyServiceBusiness.key))(any()))
            .thenReturn(Some(msbModel))

          post() { result =>
            status(result) mustBe SEE_OTHER
            redirectLocation(result) mustBe routes.SendMoneyToOtherCountryController.get().url.some
          }

        }
      }

      "redirect to the summary page when edit = true" in new FormSubmissionFixture {

        when(cache.getEntry[Renewal](eqTo(Renewal.key))(any()))
          .thenReturn(
            Some(
              Renewal(
                customersOutsideUK = Some(CustomersOutsideUK(Some(Seq(Country("", "")))))
              )
            )
          )

        when(cache.getEntry[BusinessMatching](eqTo(BusinessMatching.key))(any()))
          .thenReturn(
            Some(
              BusinessMatching(
                activities = Some(BusinessActivities(Set(HighValueDealing))),
                msbServices = Some(BusinessMatchingMsbServices(Set(TransmittingMoney)))
              )
            )
          )

        when(cache.getEntry[moneyServiceBusiness](eqTo(moneyServiceBusiness.key))(any()))
          .thenReturn(Some(msbModel))

        post(edit = true) { result =>
          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe routes.SummaryController.get.url.some
        }
      }

      "redirect to the summary page when edit = false and TransmittingMoney is not present in MSB" in new FormSubmissionFixture {

        when(cache.getEntry[Renewal](eqTo(Renewal.key))(any()))
          .thenReturn(
            Some(
              Renewal(
                customersOutsideUK = Some(CustomersOutsideUK(Some(Seq(Country("", "")))))
              )
            )
          )

        when(cache.getEntry[BusinessMatching](eqTo(BusinessMatching.key))(any()))
          .thenReturn(
            Some(
              BusinessMatching(
                activities = Some(BusinessActivities(Set(HighValueDealing))),
                msbServices = Some(BusinessMatchingMsbServices(Set(CurrencyExchange)))
              )
            )
          )

        when(cache.getEntry[moneyServiceBusiness](eqTo(moneyServiceBusiness.key))(any()))
          .thenReturn(Some(msbModel))

        post() { result =>
          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe routes.SummaryController.get.url.some
        }
      }

      "return a bad request" when {
        "the form fails validation" in new FormSubmissionFixture {

          when(cache.getEntry[Renewal](eqTo(Renewal.key))(any()))
            .thenReturn(
              Some(
                Renewal(
                  customersOutsideUK = Some(CustomersOutsideUK(Some(Seq(Country("", "")))))
                )
              )
            )

          when(cache.getEntry[BusinessMatching](eqTo(BusinessMatching.key))(any()))
            .thenReturn(
              Some(
                BusinessMatching(
                  activities = Some(BusinessActivities(Set(HighValueDealing))),
                  msbServices = Some(BusinessMatchingMsbServices(Set(TransmittingMoney)))
                )
              )
            )

          when(cache.getEntry[moneyServiceBusiness](eqTo(moneyServiceBusiness.key))(any()))
            .thenReturn(Some(msbModel))

          post(valid = false) { result =>
            status(result) mustBe BAD_REQUEST
            verify(renewalService, never()).updateRenewal(any(), any())
          }
        }
      }

      "save the model data into the renewal object" in new FormSubmissionFixture {

        val renewal = Renewal(
          customersOutsideUK = Some(CustomersOutsideUK(Some(Seq(Country("", "")))))
        )

        val expected = renewal.transactionsInLast12Months(TransactionsInLast12Months("1500"))

        when(cache.getEntry[Renewal](eqTo(Renewal.key))(any()))
          .thenReturn(Some(renewal))

        when(cache.getEntry[BusinessMatching](eqTo(BusinessMatching.key))(any()))
          .thenReturn(
            Some(
              BusinessMatching(
                activities = Some(BusinessActivities(Set(HighValueDealing))),
                msbServices = Some(BusinessMatchingMsbServices(Set(TransmittingMoney)))
              )
            )
          )

        when(cache.getEntry[moneyServiceBusiness](eqTo(moneyServiceBusiness.key))(any()))
          .thenReturn(Some(msbModel))

        post() { result =>
          status(result) mustBe SEE_OTHER
          verify(renewalService).updateRenewal(any(), eqTo(expected))
        }
      }
    }
  }
}
