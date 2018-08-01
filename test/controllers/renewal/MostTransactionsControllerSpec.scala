/*
 * Copyright 2018 HM Revenue & Customs
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
import models.renewal.{CETransactionsInLast12Months, FXTransactionsInLast12Months, MostTransactions, Renewal}
import org.jsoup.Jsoup
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import play.api.mvc.Result
import play.api.test.Helpers._
import services.RenewalService
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.{AmlsSpec, AuthorisedFixture}

import scala.concurrent.Future

class MostTransactionsControllerSpec extends AmlsSpec with MockitoSugar {

  trait Fixture extends AuthorisedFixture {
    self =>
    val request = addToken(authRequest)

    val cache: DataCacheConnector = mock[DataCacheConnector]
    val cacheMap = mock[CacheMap]
    val emptyCache = CacheMap("", Map.empty)
    val mockRenewalService = mock[RenewalService]
    val controller = new MostTransactionsController(self.authConnector, self.cache,self.mockRenewalService)
  }

  trait FormSubmissionFixture extends Fixture {
    def formData(valid: Boolean) = if (valid) "mostTransactionsCountries[0]" -> "GB" else "mostTransactionsCountries[0]" -> ""
    def formRequest(valid: Boolean) = request.withFormUrlEncodedBody(formData(valid))

    when(mockRenewalService.getRenewal(any(), any(), any()))
      .thenReturn(Future.successful(None))

    when(mockRenewalService.updateRenewal(any())(any(), any(), any()))
      .thenReturn(Future.successful(emptyCache))

    def post(edit: Boolean = false, valid: Boolean = true)(block: Result => Unit) =
      block(await(controller.post(edit)(formRequest(valid))))
  }

  "MostTransactionsController" must {

    "show an empty form on get with no data in store" in new Fixture {

      when(cache.fetch[Renewal](eqTo(Renewal.key))(any(), any(), any()))
        .thenReturn(Future.successful(None))

      val result = controller.get()(request)
      val document = Jsoup.parse(contentAsString(result))

      status(result) mustEqual OK

      document.select("select").size mustEqual 3
      document.select("option[selected]").size mustEqual 0
      document.select(".amls-error-summary").size mustEqual 0
    }

    "show a prefilled form when there is data in the store" in new Fixture {

      val model = Renewal(
        mostTransactions = Some(
          MostTransactions(
            models.countries.take(3)
          )
        )
      )

      when(cache.fetch[Renewal](eqTo(Renewal.key))(any(), any(), any()))
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

    "on valid submission" when {
      "edit false" must {
        "go to CETransactionsInLast12MonthsController" when {
          "msb includes CE" in new FormSubmissionFixture {

            val msbServices = Some(
              BusinessMatchingMsbServices(
                Set(
                  CurrencyExchange
                )
              )
            )
            val incomingModel = Renewal()

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

            when(cacheMap.getEntry[Renewal](eqTo(Renewal.key))(any()))
              .thenReturn(Some(incomingModel))

            when(cacheMap.getEntry[BusinessMatching](BusinessMatching.key))
              .thenReturn(Some(BusinessMatching(
                msbServices = msbServices,
                activities = Some(BusinessActivities(Set.empty))
              )))

            when(cache.save[Renewal](eqTo(Renewal.key), eqTo(outgoingModel))(any(), any(), any()))
              .thenReturn(Future.successful(new CacheMap("", Map.empty)))

            post() { result =>
              result.header.status mustBe SEE_OTHER
              result.header.headers.get("Location") mustEqual routes.CETransactionsInLast12MonthsController.get().url.some
            }

          }

          "msb includes CE and FX" in new FormSubmissionFixture {

            val msbServices = Some(
              BusinessMatchingMsbServices(
                Set(
                  CurrencyExchange
                )
              )
            )
            val incomingModel = Renewal()

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

            when(cacheMap.getEntry[Renewal](eqTo(Renewal.key))(any()))
                    .thenReturn(Some(incomingModel))

            when(cacheMap.getEntry[BusinessMatching](BusinessMatching.key))
                    .thenReturn(Some(BusinessMatching(
                      msbServices = msbServices,
                      activities = Some(BusinessActivities(Set.empty))
                    )))

            when(cache.save[Renewal](eqTo(Renewal.key), eqTo(outgoingModel))(any(), any(), any()))
                    .thenReturn(Future.successful(new CacheMap("", Map.empty)))

            post() { result =>
              result.header.status mustBe SEE_OTHER
              result.header.headers.get("Location") mustEqual routes.CETransactionsInLast12MonthsController.get().url.some
            }

          }
        }

        "go to FXTransactionsInLast12MonthsController" when {
          "msb includes FX and not CE" in new FormSubmissionFixture {

            val msbServices = Some(
              BusinessMatchingMsbServices(
                Set(
                  ForeignExchange
                )
              )
            )
            val incomingModel = Renewal()

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

            when(cacheMap.getEntry[Renewal](eqTo(Renewal.key))(any()))
                    .thenReturn(Some(incomingModel))

            when(cacheMap.getEntry[BusinessMatching](BusinessMatching.key))
                    .thenReturn(Some(BusinessMatching(
                      msbServices = msbServices,
                      activities = Some(BusinessActivities(Set.empty))
                    )))

            when(cache.save[Renewal](eqTo(Renewal.key), eqTo(outgoingModel))(any(), any(), any()))
                    .thenReturn(Future.successful(new CacheMap("", Map.empty)))

            post() { result =>
              result.header.status mustBe SEE_OTHER
              result.header.headers.get("Location") mustEqual routes.FXTransactionsInLast12MonthsController.get().url.some
            }

          }
        }

        "go to PercentageOfCashPaymentOver15000Controller" when {
          "activities include hvd and asp" in new FormSubmissionFixture {
            val incomingModel = Renewal()

            val msbServices = Some(BusinessMatchingMsbServices(Set.empty))

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

            when(cacheMap.getEntry[Renewal](eqTo(Renewal.key))(any()))
              .thenReturn(Some(incomingModel))

            when(cacheMap.getEntry[BusinessMatching](BusinessMatching.key))
              .thenReturn(Some(BusinessMatching(
                msbServices = msbServices,
                activities = Some(BusinessActivities(Set(
                  HighValueDealing,
                  AccountancyServices
                )))
              )))

            when(cache.save[Renewal](eqTo(Renewal.key), eqTo(outgoingModel))(any(), any(), any()))
              .thenReturn(Future.successful(new CacheMap("", Map.empty)))

            post() { result =>
              result.header.status mustBe SEE_OTHER
              result.header.headers.get("Location") mustEqual routes.PercentageOfCashPaymentOver15000Controller.get().url.some
            }
          }
        }
        "go to the CustomersOutsideUKController" when {
          "activities include hvd and NOT asp" in new FormSubmissionFixture {
            val incomingModel = Renewal()

            val msbServices = Some(BusinessMatchingMsbServices(Set.empty))

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

            when(cacheMap.getEntry[Renewal](eqTo(Renewal.key))(any()))
              .thenReturn(Some(incomingModel))

            when(cacheMap.getEntry[BusinessMatching](BusinessMatching.key))
              .thenReturn(Some(BusinessMatching(
                msbServices = msbServices,
                activities = Some(BusinessActivities(Set(
                  HighValueDealing
                )))
              )))

            when(cache.save[Renewal](eqTo(Renewal.key), eqTo(outgoingModel))(any(), any(), any()))
              .thenReturn(Future.successful(new CacheMap("", Map.empty)))

            post() { result =>
              result.header.status mustBe SEE_OTHER
              result.header.headers.get("Location") mustEqual routes.CustomersOutsideUKController.get().url.some
            }
          }

        }
        "go to SummaryController" when {
          "msb does not include CE or FX" in new FormSubmissionFixture {

            val incomingModel = Renewal()

            val outgoingModel = incomingModel.copy(
              mostTransactions = Some(
                MostTransactions(
                  Seq(Country("United Kingdom", "GB"))
                )
              ), hasChanged = true
            )
            val msbServices = Some(
              BusinessMatchingMsbServices(
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

            when(cacheMap.getEntry[Renewal](eqTo(Renewal.key))(any()))
              .thenReturn(Some(incomingModel))

            when(cacheMap.getEntry[BusinessMatching](BusinessMatching.key))
              .thenReturn(Some(BusinessMatching(
                msbServices = msbServices,
                activities = Some(BusinessActivities(Set.empty))
              )))

            when(cache.save[Renewal](eqTo(Renewal.key), eqTo(outgoingModel))(any(), any(), any()))
              .thenReturn(Future.successful(new CacheMap("", Map.empty)))

            post() { result =>
              result.header.status mustBe SEE_OTHER
              result.header.headers.get("Location") mustEqual routes.SummaryController.get().url.some
            }
          }
        }

      }

      "edit is true" must {
        "go to SummaryController" in new FormSubmissionFixture {

          val incomingModel = Renewal(
            ceTransactionsInLast12Months = Some(CETransactionsInLast12Months(
              "1223131"
            )),
            fxTransactionsInLast12Months = Some(FXTransactionsInLast12Months(
              "1223131"
            ))
          )

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
            BusinessMatchingMsbServices(
              Set(
                CurrencyExchange,
                ForeignExchange,
                TransmittingMoney,
                ChequeCashingScrapMetal
              )
            )
          )
          when(cache.fetchAll(any(), any()))
            .thenReturn(Future.successful(Some(cacheMap)))
          when(cacheMap.getEntry[BusinessMatching](BusinessMatching.key))
            .thenReturn(Some(BusinessMatching(
              msbServices = msbServices,
              activities = Some(BusinessActivities(Set.empty))
            )))
          when(cacheMap.getEntry[Renewal](eqTo(Renewal.key))(any()))
            .thenReturn(Some(incomingModel))
          when(cache.save[Renewal](eqTo(Renewal.key), eqTo(outgoingModel))(any(), any(), any()))
            .thenReturn(Future.successful(new CacheMap("", Map.empty)))

          post(edit = true) { result =>
            result.header.status mustBe SEE_OTHER
            result.header.headers.get("Location") mustEqual routes.SummaryController.get().url.some
          }
        }
      }

    }

    "throw exception when Msb services in Business Matching returns none" in new FormSubmissionFixture {

      val newRequest = request.withFormUrlEncodedBody(
        "mostTransactionsCountries[]" -> "GB"
      )

      val incomingModel = Renewal()

      when(cache.fetchAll(any(), any()))
        .thenReturn(Future.successful(Some(cacheMap)))

      when(cacheMap.getEntry[BusinessMatching](BusinessMatching.key))
        .thenReturn(None)

      when(cacheMap.getEntry[Renewal](eqTo(Renewal.key))(any()))
        .thenReturn(Some(incomingModel))

      when(cache.save[Renewal](eqTo(Renewal.key), any())
        (any(), any(), any())).thenReturn(Future.successful(new CacheMap("", Map.empty)))


      a[Exception] must be thrownBy {
        ScalaFutures.whenReady(controller.post(true)(newRequest)) { x => x }
      }
    }

  }


}
