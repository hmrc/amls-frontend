/*
 * Copyright 2017 HM Revenue & Customs
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
import models.Country
import models.businessmatching._
import models.moneyservicebusiness.{SendMoneyToOtherCountry, MoneyServiceBusiness => moneyServiceBusiness}
import models.renewal.{CustomersOutsideUK, Renewal, TransactionsInLast12Months}
import org.jsoup.Jsoup
import org.mockito.ArgumentCaptor
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import play.api.i18n.Messages
import play.api.mvc.Result
import play.api.test.Helpers._
import services.RenewalService
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.{AuthorisedFixture, GenericTestHelper}

import scala.concurrent.Future

class TransactionsInLast12MonthsControllerSpec extends GenericTestHelper with MockitoSugar {

  trait Fixture extends AuthorisedFixture {
    self =>
    val renewalService = mock[RenewalService]
    val request = addToken(authRequest)
    val mockDataCacheConnector = mock[DataCacheConnector]

    lazy val controller = new TransactionsInLast12MonthsController(self.authConnector, mockDataCacheConnector, renewalService)

    when {
      renewalService.getRenewal(any(), any(), any())
    } thenReturn Future.successful(Renewal().some)

    val msbModel = moneyServiceBusiness(sendMoneyToOtherCountry = Some(SendMoneyToOtherCountry(true)))
    val msbModelDoNotSendMoneyToOtherCountries = moneyServiceBusiness(sendMoneyToOtherCountry = Some(SendMoneyToOtherCountry(false)))
  }

  trait FormSubmissionFixture extends Fixture {
    def formData(valid: Boolean) = if (valid) {"txnAmount" -> "1500"} else {"txnAmount" -> "abc"}
    def formRequest(valid: Boolean) = request.withFormUrlEncodedBody(formData(valid))

    val cache = mock[CacheMap]

    when {
      renewalService.updateRenewal(any())(any(), any(), any())
    } thenReturn Future.successful(cache)

    when {
      mockDataCacheConnector.fetchAll(any(), any())
    } thenReturn Future.successful(Some(cache))

    def post(edit: Boolean = false, valid: Boolean = true)(block: Result => Unit) =
      block(await(controller.post(edit)(formRequest(valid))))
  }

  "Calling the GET action" must {
    "return the correct view" when {
      "edit is false" in new Fixture {
        val result = controller.get()(request)

        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.select(".heading-xlarge").text mustBe Messages("renewal.msb.transfers.header")
      }

      "edit is true" in new Fixture {
        val result = controller.get(true)(request)

        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.select("form").first.attr("action") mustBe routes.TransactionsInLast12MonthsController.post(true).url
      }

      "reads the current value from the renewals model" in new Fixture {

        when {
          renewalService.getRenewal(any(), any(), any())
        } thenReturn Future.successful(Renewal(transactionsInLast12Months = TransactionsInLast12Months("2500").some).some)

        val result = controller.get(true)(request)
        val doc = Jsoup.parse(contentAsString(result))

        doc.select("input[name=txnAmount]").first.attr("value") mustBe "2500"

        verify(renewalService).getRenewal(any(), any(), any())
      }
    }
  }

  "Calling the POST action" when {
    "posting valid data" when {

      "msb is Money Transfers" must {
        "redirect to SendMoneyToOtherCountriesController" in new FormSubmissionFixture {

          when(cache.getEntry[Renewal](eqTo(Renewal.key))(any()))
            .thenReturn(Some(Renewal(
              customersOutsideUK = Some(CustomersOutsideUK(Some(Seq(Country("GB","GB")))))
            )))

          when(cache.getEntry[BusinessMatching](eqTo(BusinessMatching.key))(any()))
            .thenReturn(Some(BusinessMatching(
              msbServices = Some(MsbServices(Set(TransmittingMoney)))
            )))

          when(cache.getEntry[moneyServiceBusiness](eqTo(moneyServiceBusiness.key))(any()))
            .thenReturn(Some(msbModel))

          post() { result =>
            result.header.status mustBe SEE_OTHER
            result.header.headers.get("Location") mustBe routes.SendMoneyToOtherCountryController.get().url.some
          }

        }
      }

      /*"they do send money to other countries" must {
        "redirect to SendTheLargestAmountsOfMoneyController" in new FormSubmissionFixture {

          when(cache.getEntry[Renewal](eqTo(Renewal.key))(any()))
            .thenReturn(Some(Renewal(
              customersOutsideUK = Some(CustomersOutsideUK(Some(Seq(Country("GB","GB")))))
            )))

          when(cache.getEntry[BusinessMatching](eqTo(BusinessMatching.key))(any()))
            .thenReturn(Some(BusinessMatching(
              activities = Some(BusinessActivities(Set(HighValueDealing))),
              msbServices = Some(MsbServices(Set(TransmittingMoney)))
            )))

          when(cache.getEntry[moneyServiceBusiness](eqTo(moneyServiceBusiness.key))(any()))
            .thenReturn(Some(msbModel))

          post() { result =>
            result.header.status mustBe SEE_OTHER
            result.header.headers.get("Location") mustBe routes.SendTheLargestAmountsOfMoneyController.get().url.some
          }
        }
      }*/

      /*"they do not send money to other countries" when {
        "msb is CurrencyExchange" must {
          "redirect to CETransactionsInLast12MonthsController" in new FormSubmissionFixture {

            when(cache.getEntry[Renewal](eqTo(Renewal.key))(any()))
              .thenReturn(Some(Renewal(
                customersOutsideUK = Some(CustomersOutsideUK(None))
              )))

            when(cache.getEntry[BusinessMatching](eqTo(BusinessMatching.key))(any()))
              .thenReturn(Some(BusinessMatching(
                activities = Some(BusinessActivities(Set(HighValueDealing))),
                msbServices = Some(MsbServices(Set(TransmittingMoney, CurrencyExchange)))
              )))

            when(cache.getEntry[moneyServiceBusiness](eqTo(moneyServiceBusiness.key))(any()))
              .thenReturn(Some(msbModelDoNotSendMoneyToOtherCountries))

            post() { result =>
              result.header.status mustBe SEE_OTHER
              result.header.headers.get("Location") mustBe routes.CETransactionsInLast12MonthsController.get().url.some
            }
          }
        }
        "msb is not CurrenyExchange" when {
          "business activities include hvd" must {
            "redirect to PercentageOfCashPaymentOver15000Controller" in new FormSubmissionFixture {

              when(cache.getEntry[Renewal](eqTo(Renewal.key))(any()))
                .thenReturn(Some(Renewal(
                  customersOutsideUK = Some(CustomersOutsideUK(None))
                )))

              when(cache.getEntry[BusinessMatching](eqTo(BusinessMatching.key))(any()))
                .thenReturn(Some(BusinessMatching(
                  activities = Some(BusinessActivities(Set(HighValueDealing))),
                  msbServices = Some(MsbServices(Set(TransmittingMoney)))
                )))

              when(cache.getEntry[moneyServiceBusiness](eqTo(moneyServiceBusiness.key))(any()))
                .thenReturn(Some(msbModelDoNotSendMoneyToOtherCountries))

              post() { result =>
                result.header.status mustBe SEE_OTHER
                result.header.headers.get("Location") mustBe routes.PercentageOfCashPaymentOver15000Controller.get().url.some
              }
            }
          }
          "business activities do not include hvd" must {
            "redirect to SummaryController" in new FormSubmissionFixture {

              when(cache.getEntry[Renewal](eqTo(Renewal.key))(any()))
                .thenReturn(Some(Renewal(
                  customersOutsideUK = Some(CustomersOutsideUK(None))
                )))

              when(cache.getEntry[BusinessMatching](eqTo(BusinessMatching.key))(any()))
                .thenReturn(Some(BusinessMatching(
                  activities = Some(BusinessActivities(Set(MoneyServiceBusiness))),
                  msbServices = Some(MsbServices(Set(TransmittingMoney)))
                )))

              when(cache.getEntry[moneyServiceBusiness](eqTo(moneyServiceBusiness.key))(any()))
                .thenReturn(Some(msbModelDoNotSendMoneyToOtherCountries))

              post() { result =>
                result.header.status mustBe SEE_OTHER
                result.header.headers.get("Location") mustBe routes.SummaryController.get().url.some
              }
            }
          }
        }
      }*/

      "redirect to the summary page when edit = true" in new FormSubmissionFixture {

        when(cache.getEntry[Renewal](eqTo(Renewal.key))(any()))
          .thenReturn(Some(Renewal(
            customersOutsideUK = Some(CustomersOutsideUK(Some(Seq(Country("","")))))
          )))

        when(cache.getEntry[BusinessMatching](eqTo(BusinessMatching.key))(any()))
          .thenReturn(Some(BusinessMatching(
            activities = Some(BusinessActivities(Set(HighValueDealing))),
            msbServices = Some(MsbServices(Set(TransmittingMoney)))
          )))

        when(cache.getEntry[moneyServiceBusiness](eqTo(moneyServiceBusiness.key))(any()))
          .thenReturn(Some(msbModel))

        post(edit = true) { result =>
          result.header.status mustBe SEE_OTHER
          result.header.headers.get("Location") mustBe routes.SummaryController.get().url.some
        }
      }

      "return a bad request" when {
        "the form fails validation" in new FormSubmissionFixture {

          when(cache.getEntry[Renewal](eqTo(Renewal.key))(any()))
            .thenReturn(Some(Renewal(
              customersOutsideUK = Some(CustomersOutsideUK(Some(Seq(Country("","")))))
            )))

          when(cache.getEntry[BusinessMatching](eqTo(BusinessMatching.key))(any()))
            .thenReturn(Some(BusinessMatching(
              activities = Some(BusinessActivities(Set(HighValueDealing))),
              msbServices = Some(MsbServices(Set(TransmittingMoney)))
            )))

          when(cache.getEntry[moneyServiceBusiness](eqTo(moneyServiceBusiness.key))(any()))
            .thenReturn(Some(msbModel))

          post(valid = false) { result =>
            result.header.status mustBe BAD_REQUEST
            verify(renewalService, never()).updateRenewal(any())(any(), any(), any())
          }
        }
      }

      "save the model data into the renewal object" in new FormSubmissionFixture {

        when(cache.getEntry[Renewal](eqTo(Renewal.key))(any()))
          .thenReturn(Some(Renewal(
            customersOutsideUK = Some(CustomersOutsideUK(Some(Seq(Country("","")))))
          )))

        when(cache.getEntry[BusinessMatching](eqTo(BusinessMatching.key))(any()))
          .thenReturn(Some(BusinessMatching(
            activities = Some(BusinessActivities(Set(HighValueDealing))),
            msbServices = Some(MsbServices(Set(TransmittingMoney)))
          )))

        when(cache.getEntry[moneyServiceBusiness](eqTo(moneyServiceBusiness.key))(any()))
          .thenReturn(Some(msbModel))

        post() { _ =>
          val captor = ArgumentCaptor.forClass(classOf[Renewal])

          verify(renewalService).updateRenewal(captor.capture())(any(), any(), any())

          captor.getValue.transactionsInLast12Months mustBe TransactionsInLast12Months("1500").some
        }
      }
    }
  }
}
