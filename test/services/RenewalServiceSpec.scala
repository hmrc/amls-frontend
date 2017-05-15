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

package services

import connectors.DataCacheConnector
import models.Country
import models.businessmatching._
import models.registrationprogress.{Completed, NotStarted, Section, Started}
import models.renewal._
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import play.api.inject.bind
import play.api.inject.guice.GuiceInjectorBuilder
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http.HeaderCarrier
import utils.{AuthorisedFixture, GenericTestHelper}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class RenewalServiceSpec extends GenericTestHelper with MockitoSugar {

  implicit val hc = HeaderCarrier()

  trait Fixture extends AuthorisedFixture {

    val dataCache = mock[DataCacheConnector]
    implicit val authContext = mock[AuthContext]

    val injector = new GuiceInjectorBuilder()
      .overrides(bind[DataCacheConnector].to(dataCache))
      .build()

    val service = injector.instanceOf[RenewalService]

    val completeModel = Renewal(
      Some(InvolvedInOtherYes("test")),
      Some(BusinessTurnover.First),
      Some(AMLSTurnover.First),
      Some(CustomersOutsideUK(Some(Seq(Country("United Kingdom", "GB"))))),
      Some(PercentageOfCashPaymentOver15000.First),
      Some(ReceiveCashPayments(Some(PaymentMethods(true, true, Some("other"))))),
      Some(TotalThroughput("01")),
      Some(WhichCurrencies(Seq("EUR"), None, None, None, None)),
      Some(TransactionsInLast12Months("1500")),
      Some(SendTheLargestAmountsOfMoney(Country("us", "US"))),
      Some(MostTransactions(Seq(Country("United Kingdom", "GB")))),
      Some(CETransactionsInLast12Months("123")),
      // Add other models here
      true)

    val mockCacheMap = mock[CacheMap]

  }

  "The renewal service" must {

    "return the correct section" when {

      "the renewal hasn't been started" in new Fixture {

        when {
          dataCache.fetch[Renewal](eqTo(Renewal.key))(any(), any(), any())
        } thenReturn Future.successful(None)

        val section = await(service.getSection)

        section mustBe Section("renewal", NotStarted, hasChanged = false, controllers.renewal.routes.WhatYouNeedController.get())

      }

      "the renewal is complete and has been started" in new Fixture {

        when(dataCache.fetchAll(any(),any()))
          .thenReturn(Future.successful(Some(mockCacheMap)))
        when(mockCacheMap.getEntry[BusinessMatching](any())(any()))
          .thenReturn(Some(BusinessMatching(
            activities = Some(BusinessActivities(Set(
              MoneyServiceBusiness,
              HighValueDealing
            ))),
            msbServices = Some(MsbServices(Set(CurrencyExchange)))
          )))

        when {
          dataCache.fetch[Renewal](eqTo(Renewal.key))(any(), any(), any())
        } thenReturn Future.successful(Some(completeModel))

        val section = await(service.getSection)

        await(service.isRenewalComplete(completeModel)) mustBe true
        section mustBe Section("renewal", Completed, hasChanged = true, controllers.renewal.routes.SummaryController.get())

      }

      "the renewal model is not complete" in new Fixture {

        val renewal = mock[Renewal]
        when(renewal.hasChanged) thenReturn true

        when(dataCache.fetchAll(any(),any()))
          .thenReturn(Future.successful(Some(mockCacheMap)))
        when(mockCacheMap.getEntry[BusinessMatching](any())(any()))
          .thenReturn(Some(BusinessMatching()))

        when {
          dataCache.fetch[Renewal](eqTo(Renewal.key))(any(), any(), any())
        } thenReturn Future.successful(Some(renewal))

        val section = await(service.getSection)

        section mustBe Section("renewal", Started, hasChanged = true, controllers.renewal.routes.WhatYouNeedController.get())

      }

      "the renewal model is not complete and not started" in new Fixture {
        val renewal = Renewal(None)

        when(dataCache.fetchAll(any(),any()))
          .thenReturn(Future.successful(Some(mockCacheMap)))
        when(mockCacheMap.getEntry[BusinessMatching](any())(any()))
          .thenReturn(Some(BusinessMatching()))

        when {
          dataCache.fetch[Renewal](eqTo(Renewal.key))(any(), any(), any())
        } thenReturn Future.successful(Some(renewal))

        val section = await(service.getSection)

        section mustBe Section("renewal", NotStarted, hasChanged = false, controllers.renewal.routes.WhatYouNeedController.get())
      }

    }

  }

  "isRenewalComplete" must {
    "be true" when {
      "it is an MSB" when {
        "it is an HVD" when {
          "it is a CurrencyExchange" when {
            "involvedInOther is true" in new Fixture {

              when(dataCache.fetchAll(any(),any()))
                .thenReturn(Future.successful(Some(mockCacheMap)))

              when(mockCacheMap.getEntry[BusinessMatching](any())(any()))
                .thenReturn(Some(BusinessMatching(
                  activities = Some(BusinessActivities(Set(
                    MoneyServiceBusiness,
                    HighValueDealing
                  ))),
                  msbServices = Some(MsbServices(Set(CurrencyExchange)))
                )))

              val model = Renewal(
                Some(InvolvedInOtherYes("test")),
                Some(BusinessTurnover.First),
                Some(AMLSTurnover.First),
                Some(CustomersOutsideUK(Some(Seq(Country("United Kingdom", "GB"))))),
                Some(PercentageOfCashPaymentOver15000.First),
                Some(ReceiveCashPayments(Some(PaymentMethods(true, true, Some("other"))))),
                Some(TotalThroughput("01")),
                Some(WhichCurrencies(Seq("EUR"), None, None, None, None)),
                Some(TransactionsInLast12Months("1500")),
                Some(SendTheLargestAmountsOfMoney(Country("United Kingdom", "GB"))),
                Some(MostTransactions(Seq(Country("United Kingdom", "GB")))),
                Some(CETransactionsInLast12Months("123")),
                hasChanged = true
              )

              await(service.isRenewalComplete(model)) mustBe true
            }
            "involvedInOther is false" in new Fixture {

              when(dataCache.fetchAll(any(),any()))
                .thenReturn(Future.successful(Some(mockCacheMap)))

              when(mockCacheMap.getEntry[BusinessMatching](any())(any()))
                .thenReturn(Some(BusinessMatching(
                  activities = Some(BusinessActivities(Set(
                    MoneyServiceBusiness,
                    HighValueDealing
                  ))),
                  msbServices = Some(MsbServices(Set(CurrencyExchange)))
                )))

              val model = Renewal(
                Some(InvolvedInOtherNo),
                None,
                Some(AMLSTurnover.First),
                Some(CustomersOutsideUK(Some(Seq(Country("United Kingdom", "GB"))))),
                Some(PercentageOfCashPaymentOver15000.First),
                Some(ReceiveCashPayments(Some(PaymentMethods(true, true, Some("other"))))),
                Some(TotalThroughput("01")),
                Some(WhichCurrencies(Seq("EUR"), None, None, None, None)),
                Some(TransactionsInLast12Months("1500")),
                Some(SendTheLargestAmountsOfMoney(Country("United Kingdom", "GB"))),
                Some(MostTransactions(Seq(Country("United Kingdom", "GB")))),
                Some(CETransactionsInLast12Months("123")),
                hasChanged = true
              )

              await(service.isRenewalComplete(model)) mustBe true
            }
          }

          "it is NOT a CurrencyExchange" when {
            "involvedInOther is true" in new Fixture {

              when(dataCache.fetchAll(any(),any()))
                .thenReturn(Future.successful(Some(mockCacheMap)))

              when(mockCacheMap.getEntry[BusinessMatching](any())(any()))
                .thenReturn(Some(BusinessMatching(
                  activities = Some(BusinessActivities(Set(
                    MoneyServiceBusiness,
                    HighValueDealing
                  ))),
                  msbServices = Some(MsbServices(Set(TransmittingMoney)))
                )))

              val model = Renewal(
                Some(InvolvedInOtherYes("test")),
                Some(BusinessTurnover.First),
                Some(AMLSTurnover.First),
                Some(CustomersOutsideUK(Some(Seq(Country("United Kingdom", "GB"))))),
                Some(PercentageOfCashPaymentOver15000.First),
                Some(ReceiveCashPayments(Some(PaymentMethods(true, true, Some("other"))))),
                Some(TotalThroughput("01")),
                None,
                Some(TransactionsInLast12Months("1500")),
                Some(SendTheLargestAmountsOfMoney(Country("United Kingdom", "GB"))),
                Some(MostTransactions(Seq(Country("United Kingdom", "GB")))),
                None,
                hasChanged = true
              )

              await(service.isRenewalComplete(model)) mustBe true
            }
            "involvedInOther is false" in new Fixture {

              when(dataCache.fetchAll(any(),any()))
                .thenReturn(Future.successful(Some(mockCacheMap)))

              when(mockCacheMap.getEntry[BusinessMatching](any())(any()))
                .thenReturn(Some(BusinessMatching(
                  activities = Some(BusinessActivities(Set(
                    MoneyServiceBusiness,
                    HighValueDealing
                  ))),
                  msbServices = Some(MsbServices(Set(TransmittingMoney)))
                )))

              val model = Renewal(
                Some(InvolvedInOtherNo),
                None,
                Some(AMLSTurnover.First),
                Some(CustomersOutsideUK(Some(Seq(Country("United Kingdom", "GB"))))),
                Some(PercentageOfCashPaymentOver15000.First),
                Some(ReceiveCashPayments(Some(PaymentMethods(true, true, Some("other"))))),
                Some(TotalThroughput("01")),
                None,
                Some(TransactionsInLast12Months("1500")),
                Some(SendTheLargestAmountsOfMoney(Country("United Kingdom", "GB"))),
                Some(MostTransactions(Seq(Country("United Kingdom", "GB")))),
                None,
                hasChanged = true
              )

              await(service.isRenewalComplete(model)) mustBe true
            }
          }

        }

        "it is NOT an HVD" when {
          "it is a CurrencyExchange" when {
            "involvedInOther is true" in new Fixture {

              when(dataCache.fetchAll(any(),any()))
                .thenReturn(Future.successful(Some(mockCacheMap)))

              when(mockCacheMap.getEntry[BusinessMatching](any())(any()))
                .thenReturn(Some(BusinessMatching(
                  activities = Some(BusinessActivities(Set(
                    MoneyServiceBusiness
                  ))),
                  msbServices = Some(MsbServices(Set(CurrencyExchange)))
                )))

              val model = Renewal(
                Some(InvolvedInOtherYes("test")),
                Some(BusinessTurnover.First),
                Some(AMLSTurnover.First),
                Some(CustomersOutsideUK(Some(Seq(Country("United Kingdom", "GB"))))),
                None,
                None,
                Some(TotalThroughput("01")),
                Some(WhichCurrencies(Seq("EUR"), None, None, None, None)),
                Some(TransactionsInLast12Months("1500")),
                Some(SendTheLargestAmountsOfMoney(Country("United Kingdom", "GB"))),
                Some(MostTransactions(Seq(Country("United Kingdom", "GB")))),
                Some(CETransactionsInLast12Months("123")),
                hasChanged = true
              )

              await(service.isRenewalComplete(model)) mustBe true
            }
            "involvedInOther is false" in new Fixture {

              when(dataCache.fetchAll(any(),any()))
                .thenReturn(Future.successful(Some(mockCacheMap)))

              when(mockCacheMap.getEntry[BusinessMatching](any())(any()))
                .thenReturn(Some(BusinessMatching(
                  activities = Some(BusinessActivities(Set(
                    MoneyServiceBusiness
                  ))),
                  msbServices = Some(MsbServices(Set(CurrencyExchange)))
                )))

              val model = Renewal(
                Some(InvolvedInOtherNo),
                None,
                Some(AMLSTurnover.First),
                Some(CustomersOutsideUK(Some(Seq(Country("United Kingdom", "GB"))))),
                None,
                None,
                Some(TotalThroughput("01")),
                Some(WhichCurrencies(Seq("EUR"), None, None, None, None)),
                Some(TransactionsInLast12Months("1500")),
                Some(SendTheLargestAmountsOfMoney(Country("United Kingdom", "GB"))),
                Some(MostTransactions(Seq(Country("United Kingdom", "GB")))),
                Some(CETransactionsInLast12Months("123")),
                hasChanged = true
              )

              await(service.isRenewalComplete(model)) mustBe true
            }
          }

          "it is NOT a CurrencyExchange" when {
            "involvedInOther is true" in new Fixture {

              when(dataCache.fetchAll(any(),any()))
                .thenReturn(Future.successful(Some(mockCacheMap)))

              when(mockCacheMap.getEntry[BusinessMatching](any())(any()))
                .thenReturn(Some(BusinessMatching(
                  activities = Some(BusinessActivities(Set(
                    MoneyServiceBusiness
                  ))),
                  msbServices = Some(MsbServices(Set(TransmittingMoney)))
                )))

              val model = Renewal(
                Some(InvolvedInOtherYes("test")),
                Some(BusinessTurnover.First),
                Some(AMLSTurnover.First),
                Some(CustomersOutsideUK(Some(Seq(Country("United Kingdom", "GB"))))),
                None,
                None,
                Some(TotalThroughput("01")),
                None,
                Some(TransactionsInLast12Months("1500")),
                Some(SendTheLargestAmountsOfMoney(Country("United Kingdom", "GB"))),
                Some(MostTransactions(Seq(Country("United Kingdom", "GB")))),
                None,
                hasChanged = true
              )

              await(service.isRenewalComplete(model)) mustBe true
            }
            "involvedInOther is false" in new Fixture {

              when(dataCache.fetchAll(any(),any()))
                .thenReturn(Future.successful(Some(mockCacheMap)))

              when(mockCacheMap.getEntry[BusinessMatching](any())(any()))
                .thenReturn(Some(BusinessMatching(
                  activities = Some(BusinessActivities(Set(
                    MoneyServiceBusiness
                  ))),
                  msbServices = Some(MsbServices(Set(TransmittingMoney)))
                )))

              val model = Renewal(
                Some(InvolvedInOtherNo),
                None,
                Some(AMLSTurnover.First),
                Some(CustomersOutsideUK(Some(Seq(Country("United Kingdom", "GB"))))),
                None,
                None,
                Some(TotalThroughput("01")),
                None,
                Some(TransactionsInLast12Months("1500")),
                Some(SendTheLargestAmountsOfMoney(Country("United Kingdom", "GB"))),
                Some(MostTransactions(Seq(Country("United Kingdom", "GB")))),
                None,
                hasChanged = true
              )

              await(service.isRenewalComplete(model)) mustBe true
            }
          }

        }
      }

      "it is NOT an MSB" when {
        "it is an HVD" when {
          "involvedInOther is true" in new Fixture {

            when(dataCache.fetchAll(any(),any()))
              .thenReturn(Future.successful(Some(mockCacheMap)))

            when(mockCacheMap.getEntry[BusinessMatching](any())(any()))
              .thenReturn(Some(BusinessMatching(
                activities = Some(BusinessActivities(Set(
                  HighValueDealing
                )))
              )))

            val model = Renewal(
              Some(InvolvedInOtherYes("test")),
              Some(BusinessTurnover.First),
              Some(AMLSTurnover.First),
              Some(CustomersOutsideUK(Some(Seq(Country("United Kingdom", "GB"))))),
              Some(PercentageOfCashPaymentOver15000.First),
              Some(ReceiveCashPayments(Some(PaymentMethods(true, true, Some("other"))))),
              None,
              None,
              None,
              None,
              None,
              None,
              hasChanged = true
            )

            await(service.isRenewalComplete(model)) mustBe true
          }
          "involvedInOther is false" in new Fixture {

            when(dataCache.fetchAll(any(),any()))
              .thenReturn(Future.successful(Some(mockCacheMap)))

            when(mockCacheMap.getEntry[BusinessMatching](any())(any()))
              .thenReturn(Some(BusinessMatching(
                activities = Some(BusinessActivities(Set(
                  HighValueDealing
                )))
              )))

            val model = Renewal(
              Some(InvolvedInOtherNo),
              None,
              Some(AMLSTurnover.First),
              Some(CustomersOutsideUK(Some(Seq(Country("United Kingdom", "GB"))))),
              Some(PercentageOfCashPaymentOver15000.First),
              Some(ReceiveCashPayments(Some(PaymentMethods(true, true, Some("other"))))),
              None,
              None,
              None,
              None,
              None,
              None,
              hasChanged = true
            )

            await(service.isRenewalComplete(model)) mustBe true
          }
        }
        "it is NOT an HVD" when {
          "involvedInOther is true" in new Fixture {

            when(dataCache.fetchAll(any(),any()))
              .thenReturn(Future.successful(Some(mockCacheMap)))

            when(mockCacheMap.getEntry[BusinessMatching](any())(any()))
              .thenReturn(Some(BusinessMatching(
                activities = Some(BusinessActivities(Set(
                  TelephonePaymentService
                )))
              )))

            val model = Renewal(
              Some(InvolvedInOtherYes("test")),
              Some(BusinessTurnover.First),
              Some(AMLSTurnover.First),
              Some(CustomersOutsideUK(Some(Seq(Country("United Kingdom", "GB"))))),
              None,
              None,
              None,
              None,
              None,
              None,
              None,
              None,
              hasChanged = true
            )

            await(service.isRenewalComplete(model)) mustBe true
          }
          "involvedInOther is false" in new Fixture {

            when(dataCache.fetchAll(any(),any()))
              .thenReturn(Future.successful(Some(mockCacheMap)))

            when(mockCacheMap.getEntry[BusinessMatching](any())(any()))
              .thenReturn(Some(BusinessMatching(
                activities = Some(BusinessActivities(Set(
                  TelephonePaymentService
                )))
              )))

            val model = Renewal(
              Some(InvolvedInOtherNo),
              None,
              Some(AMLSTurnover.First),
              Some(CustomersOutsideUK(Some(Seq(Country("United Kingdom", "GB"))))),
              None,
              None,
              None,
              None,
              None,
              None,
              None,
              None,
              hasChanged = true
            )

            await(service.isRenewalComplete(model)) mustBe true
          }
        }
      }
    }

    "be false" when {
      "it is an MSB" when {
        "it is an HVD" when {
          "it is a CurrencyExchange" in new Fixture {

            when(dataCache.fetchAll(any(),any()))
              .thenReturn(Future.successful(Some(mockCacheMap)))

            when(mockCacheMap.getEntry[BusinessMatching](any())(any()))
              .thenReturn(Some(BusinessMatching(
                activities = Some(BusinessActivities(Set(
                  MoneyServiceBusiness,
                  HighValueDealing
                ))),
                msbServices = Some(MsbServices(Set(CurrencyExchange)))
              )))

            val model = Renewal(
              Some(InvolvedInOtherYes("test")),
              Some(BusinessTurnover.First),
              Some(AMLSTurnover.First),
              Some(CustomersOutsideUK(Some(Seq(Country("United Kingdom", "GB"))))),
              Some(PercentageOfCashPaymentOver15000.First),
              Some(ReceiveCashPayments(Some(PaymentMethods(true, true, Some("other"))))),
              Some(TotalThroughput("01")),
              Some(WhichCurrencies(Seq("EUR"), None, None, None, None)),
              Some(TransactionsInLast12Months("1500")),
              Some(SendTheLargestAmountsOfMoney(Country("United Kingdom", "GB"))),
              Some(MostTransactions(Seq(Country("United Kingdom", "GB")))),
              None,
              hasChanged = true
            )

            await(service.isRenewalComplete(model)) mustBe false

          }
          "it is not a CurrencyExchange" in new Fixture {

            when(dataCache.fetchAll(any(),any()))
              .thenReturn(Future.successful(Some(mockCacheMap)))

            when(mockCacheMap.getEntry[BusinessMatching](any())(any()))
              .thenReturn(Some(BusinessMatching(
                activities = Some(BusinessActivities(Set(
                  MoneyServiceBusiness,
                  HighValueDealing
                )))
              )))

            val model = Renewal(
              Some(InvolvedInOtherYes("test")),
              Some(BusinessTurnover.First),
              Some(AMLSTurnover.First),
              Some(CustomersOutsideUK(Some(Seq(Country("United Kingdom", "GB"))))),
              Some(PercentageOfCashPaymentOver15000.First),
              Some(ReceiveCashPayments(Some(PaymentMethods(true, true, Some("other"))))),
              Some(TotalThroughput("01")),
              None,
              None,
              Some(SendTheLargestAmountsOfMoney(Country("United Kingdom", "GB"))),
              Some(MostTransactions(Seq(Country("United Kingdom", "GB")))),
              None,
              hasChanged = true
            )

            await(service.isRenewalComplete(model)) mustBe false

          }
        }
        "it is NOT an HVD" when {
          "it is a CurrencyExchange" in new Fixture {

            when(dataCache.fetchAll(any(),any()))
              .thenReturn(Future.successful(Some(mockCacheMap)))

            when(mockCacheMap.getEntry[BusinessMatching](any())(any()))
              .thenReturn(Some(BusinessMatching(
                activities = Some(BusinessActivities(Set(
                  MoneyServiceBusiness
                ))),
                msbServices = Some(MsbServices(Set(CurrencyExchange)))
              )))

            val model = Renewal(
              Some(InvolvedInOtherYes("test")),
              Some(BusinessTurnover.First),
              Some(AMLSTurnover.First),
              Some(CustomersOutsideUK(Some(Seq(Country("United Kingdom", "GB"))))),
              None,
              None,
              Some(TotalThroughput("01")),
              None,
              Some(TransactionsInLast12Months("1500")),
              None,
              Some(MostTransactions(Seq(Country("United Kingdom", "GB")))),
              None,
              hasChanged = true
            )

            await(service.isRenewalComplete(model)) mustBe false

          }
          "it is NOT a CurrencyExchange" in new Fixture {

            when(dataCache.fetchAll(any(),any()))
              .thenReturn(Future.successful(Some(mockCacheMap)))

            when(mockCacheMap.getEntry[BusinessMatching](any())(any()))
              .thenReturn(Some(BusinessMatching(
                activities = Some(BusinessActivities(Set(
                  MoneyServiceBusiness
                )))
              )))

            val model = Renewal(
              Some(InvolvedInOtherYes("test")),
              Some(BusinessTurnover.First),
              Some(AMLSTurnover.First),
              Some(CustomersOutsideUK(Some(Seq(Country("United Kingdom", "GB"))))),
              None,
              None,
              Some(TotalThroughput("01")),
              None,
              Some(TransactionsInLast12Months("1500")),
              None,
              Some(MostTransactions(Seq(Country("United Kingdom", "GB")))),
              None,
              hasChanged = true
            )

            await(service.isRenewalComplete(model)) mustBe false

          }
        }
      }
      "It is not an MSB" when {
        "it is an HVD" in new Fixture {

            when(dataCache.fetchAll(any(),any()))
              .thenReturn(Future.successful(Some(mockCacheMap)))

            when(mockCacheMap.getEntry[BusinessMatching](any())(any()))
              .thenReturn(Some(BusinessMatching(
                activities = Some(BusinessActivities(Set(
                  MoneyServiceBusiness,
                  HighValueDealing
                )))
              )))

            val model = Renewal(
              Some(InvolvedInOtherYes("test")),
              Some(BusinessTurnover.First),
              Some(AMLSTurnover.First),
              Some(CustomersOutsideUK(Some(Seq(Country("United Kingdom", "GB"))))),
              Some(PercentageOfCashPaymentOver15000.First),
              None,
              None,
              None,
              None,
              None,
              None,
              None,
              hasChanged = true
            )

            await(service.isRenewalComplete(model)) mustBe false

        }
        "it is NOT an HVD" in new Fixture {

            when(dataCache.fetchAll(any(),any()))
              .thenReturn(Future.successful(Some(mockCacheMap)))

            when(mockCacheMap.getEntry[BusinessMatching](any())(any()))
              .thenReturn(Some(BusinessMatching(
                activities = Some(BusinessActivities(Set(
                  MoneyServiceBusiness
                )))
              )))

            val model = Renewal(
              Some(InvolvedInOtherYes("test")),
              None,
              None,
              None,
              None,
              None,
              None,
              None,
              None,
              None,
              None,
              None,
              hasChanged = true
            )

            await(service.isRenewalComplete(model)) mustBe false

        }
      }
    }
  }
}
