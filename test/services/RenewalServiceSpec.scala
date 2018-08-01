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

package services

import connectors.DataCacheConnector
import models.Country
import models.businessmatching._
import models.moneyservicebusiness.{MoneyServiceBusiness => moneyServiceBusiness}
import models.registrationprogress.{Completed, NotStarted, Section, Started}
import models.renewal._
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatest.MustMatchers
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.OneAppPerSuite
import play.api.inject.bind
import play.api.inject.guice.GuiceInjectorBuilder
import play.api.mvc.Call
import play.api.test.FakeApplication
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.frontend.auth.AuthContext
import utils.{AuthorisedFixture, AmlsSpec}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import uk.gov.hmrc.http.HeaderCarrier


class RenewalServiceSpec extends AmlsSpec with MockitoSugar {

  implicit val hc = HeaderCarrier()

  trait Fixture extends AuthorisedFixture {

    val dataCache = mock[DataCacheConnector]
    implicit val authContext = mock[AuthContext]

    val service = new RenewalService(dataCache)

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
      None,
      true,
      Some(SendMoneyToOtherCountry(true))
    )

    val mockCacheMap = mock[CacheMap]

    //val msbModel = moneyServiceBusiness(sendMoneyToOtherCountry = Some(SendMoneyToOtherCountry(true)))
    //val msbModelDoNotSendMoneyToOtherCountries = moneyServiceBusiness(sendMoneyToOtherCountry = Some(SendMoneyToOtherCountry(false)))

  }

  "The renewal service" must {

    "return the correct section" when {

      "the renewal hasn't been started" in new Fixture {

        when {
          dataCache.fetch[Renewal](eqTo(Renewal.key))(any(), any(), any())
        } thenReturn Future.successful(None)

        val section = await(service.getSection)

        section mustBe Section(Renewal.sectionKey, NotStarted, hasChanged = false, controllers.renewal.routes.WhatYouNeedController.get())

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
            msbServices = Some(BusinessMatchingMsbServices(Set(CurrencyExchange, TransmittingMoney)))
          )))

        when {
          dataCache.fetch[Renewal](eqTo(Renewal.key))(any(), any(), any())
        } thenReturn Future.successful(Some(completeModel))

        val section = await(service.getSection)

        await(service.isRenewalComplete(completeModel)) mustBe true
        section mustBe Section(Renewal.sectionKey, Completed, hasChanged = true, controllers.renewal.routes.SummaryController.get())

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

        section mustBe Section(Renewal.sectionKey, Started, hasChanged = true, controllers.renewal.routes.WhatYouNeedController.get())

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

        section mustBe Section(Renewal.sectionKey, NotStarted, hasChanged = false, controllers.renewal.routes.WhatYouNeedController.get())
      }

    }

  }

  "isRenewalComplete" must {
    "be true" when {
      "it is an HVD and customers outside the UK is set" in new Fixture {
        when(dataCache.fetchAll(any(),any()))
          .thenReturn(Future.successful(Some(mockCacheMap)))

        when(mockCacheMap.getEntry[BusinessMatching](any())(any()))
          .thenReturn(Some(BusinessMatching(
            activities = Some(BusinessActivities(Set(
              HighValueDealing
            ))),
            msbServices = Some(BusinessMatchingMsbServices(Set(CurrencyExchange)))
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
          None,
          hasChanged = true,
          None
        )

        await(service.isRenewalComplete(model)) mustBe true
      }

      "it is an ASP and customers outside the UK is set" in new Fixture {
        when(dataCache.fetchAll(any(),any()))
          .thenReturn(Future.successful(Some(mockCacheMap)))

        when(mockCacheMap.getEntry[BusinessMatching](any())(any()))
          .thenReturn(Some(BusinessMatching(
            activities = Some(BusinessActivities(Set(
              AccountancyServices
            ))),
            msbServices = Some(BusinessMatchingMsbServices(Set(CurrencyExchange)))
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
          None,
          hasChanged = true,
          None
        )

        await(service.isRenewalComplete(model)) mustBe true
      }

      "it is an MSB" when {
        "it is an HVD" when {
          "it is a CurrencyExchange" when {
            "it is a TransmittingMoney" when {
              "there are customers outside the UK" when {
                "involvedInOther is true" in new Fixture {

                  when(dataCache.fetchAll(any(), any()))
                    .thenReturn(Future.successful(Some(mockCacheMap)))

                  when(mockCacheMap.getEntry[BusinessMatching](any())(any()))
                    .thenReturn(Some(BusinessMatching(
                      activities = Some(BusinessActivities(Set(
                        MoneyServiceBusiness,
                        HighValueDealing
                      ))),
                      msbServices = Some(BusinessMatchingMsbServices(Set(CurrencyExchange, TransmittingMoney)))
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
                    None,
                    hasChanged = true,
                    Some(SendMoneyToOtherCountry(true))
                  )

                  await(service.isRenewalComplete(model)) mustBe true
                }

                "involvedInOther is false" in new Fixture {

                  when(dataCache.fetchAll(any(), any()))
                    .thenReturn(Future.successful(Some(mockCacheMap)))

                  when(mockCacheMap.getEntry[BusinessMatching](any())(any()))
                    .thenReturn(Some(BusinessMatching(
                      activities = Some(BusinessActivities(Set(
                        MoneyServiceBusiness,
                        HighValueDealing
                      ))),
                      msbServices = Some(BusinessMatchingMsbServices(Set(CurrencyExchange, TransmittingMoney)))
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
                    None,
                    hasChanged = true,
                    Some(SendMoneyToOtherCountry(true))
                  )

                  await(service.isRenewalComplete(model)) mustBe true
                }
              }
              "they do not send money to other countries" when {
                "involvedInOther is true" in new Fixture {

                  when(dataCache.fetchAll(any(), any()))
                    .thenReturn(Future.successful(Some(mockCacheMap)))

                  when(mockCacheMap.getEntry[BusinessMatching](any())(any()))
                    .thenReturn(Some(BusinessMatching(
                      activities = Some(BusinessActivities(Set(
                        MoneyServiceBusiness,
                        HighValueDealing
                      ))),
                      msbServices = Some(BusinessMatchingMsbServices(Set(CurrencyExchange, TransmittingMoney)))
                    )))

                  val model = Renewal(
                    Some(InvolvedInOtherYes("test")),
                    Some(BusinessTurnover.First),
                    Some(AMLSTurnover.First),
                    Some(CustomersOutsideUK(None)),
                    Some(PercentageOfCashPaymentOver15000.First),
                    Some(ReceiveCashPayments(Some(PaymentMethods(true, true, Some("other"))))),
                    Some(TotalThroughput("01")),
                    Some(WhichCurrencies(Seq("EUR"), None, None, None, None)),
                    Some(TransactionsInLast12Months("1500")),
                    None,
                    None,
                    Some(CETransactionsInLast12Months("123")),
                    None,
                    hasChanged = true,
                    Some(SendMoneyToOtherCountry(false))
                  )

                  await(service.isRenewalComplete(model)) mustBe true
                }
                "involvedInOther is false" in new Fixture {

                  when(dataCache.fetchAll(any(), any()))
                    .thenReturn(Future.successful(Some(mockCacheMap)))

                  when(mockCacheMap.getEntry[BusinessMatching](any())(any()))
                    .thenReturn(Some(BusinessMatching(
                      activities = Some(BusinessActivities(Set(
                        MoneyServiceBusiness,
                        HighValueDealing
                      ))),
                      msbServices = Some(BusinessMatchingMsbServices(Set(CurrencyExchange, TransmittingMoney)))
                    )))


                  val model = Renewal(
                    Some(InvolvedInOtherNo),
                    None,
                    Some(AMLSTurnover.First),
                    Some(CustomersOutsideUK(None)),
                    Some(PercentageOfCashPaymentOver15000.First),
                    Some(ReceiveCashPayments(Some(PaymentMethods(true, true, Some("other"))))),
                    Some(TotalThroughput("01")),
                    Some(WhichCurrencies(Seq("EUR"), None, None, None, None)),
                    Some(TransactionsInLast12Months("1500")),
                    None,
                    None,
                    Some(CETransactionsInLast12Months("123")),
                    None,
                    hasChanged = true,
                    Some(SendMoneyToOtherCountry(false))
                  )

                  await(service.isRenewalComplete(model)) mustBe true
                }
              }
            }
            "it is NOT a TransmittingMoney" when {
              "involvedInOther is true" in new Fixture {

                when(dataCache.fetchAll(any(), any()))
                  .thenReturn(Future.successful(Some(mockCacheMap)))

                when(mockCacheMap.getEntry[BusinessMatching](any())(any()))
                  .thenReturn(Some(BusinessMatching(
                    activities = Some(BusinessActivities(Set(
                      MoneyServiceBusiness,
                      HighValueDealing
                    ))),
                    msbServices = Some(BusinessMatchingMsbServices(Set(CurrencyExchange)))
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
                  None,
                  None,
                  None,
                  Some(CETransactionsInLast12Months("123")),
                  None,
                  hasChanged = true,
                  Some(SendMoneyToOtherCountry(false))
                )

                await(service.isRenewalComplete(model)) mustBe true
              }
              "involvedInOther is false" in new Fixture {

                when(dataCache.fetchAll(any(), any()))
                  .thenReturn(Future.successful(Some(mockCacheMap)))

                when(mockCacheMap.getEntry[BusinessMatching](any())(any()))
                  .thenReturn(Some(BusinessMatching(
                    activities = Some(BusinessActivities(Set(
                      MoneyServiceBusiness,
                      HighValueDealing
                    ))),
                    msbServices = Some(BusinessMatchingMsbServices(Set(CurrencyExchange)))
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
                  None,
                  None,
                  None,
                  Some(CETransactionsInLast12Months("123")),
                  None,
                  hasChanged = true,
                  Some(SendMoneyToOtherCountry(false))
                )

                await(service.isRenewalComplete(model)) mustBe true
              }
            }
          }
          "it is NOT a CurrencyExchange" when {
            "it is a TransmittingMoney" when {
              "there are customers outside the UK" when {
                "involvedInOther is true" in new Fixture {

                  when(dataCache.fetchAll(any(), any()))
                    .thenReturn(Future.successful(Some(mockCacheMap)))

                  when(mockCacheMap.getEntry[BusinessMatching](any())(any()))
                    .thenReturn(Some(BusinessMatching(
                      activities = Some(BusinessActivities(Set(
                        MoneyServiceBusiness,
                        HighValueDealing
                      ))),
                      msbServices = Some(BusinessMatchingMsbServices(Set(TransmittingMoney)))
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
                    None,
                    hasChanged = true,
                    Some(SendMoneyToOtherCountry(true))
                  )

                  await(service.isRenewalComplete(model)) mustBe true
                }
                "involvedInOther is false" in new Fixture {

                  when(dataCache.fetchAll(any(), any()))
                    .thenReturn(Future.successful(Some(mockCacheMap)))

                  when(mockCacheMap.getEntry[BusinessMatching](any())(any()))
                    .thenReturn(Some(BusinessMatching(
                      activities = Some(BusinessActivities(Set(
                        MoneyServiceBusiness,
                        HighValueDealing
                      ))),
                      msbServices = Some(BusinessMatchingMsbServices(Set(TransmittingMoney)))
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
                    None,
                    hasChanged = true,
                    Some(SendMoneyToOtherCountry(true))
                  )

                  await(service.isRenewalComplete(model)) mustBe true
                }
              }
              "they do not send money to other countries" when {
                "involvedInOther is true" in new Fixture {

                  when(dataCache.fetchAll(any(), any()))
                    .thenReturn(Future.successful(Some(mockCacheMap)))

                  when(mockCacheMap.getEntry[BusinessMatching](any())(any()))
                    .thenReturn(Some(BusinessMatching(
                      activities = Some(BusinessActivities(Set(
                        MoneyServiceBusiness,
                        HighValueDealing
                      ))),
                      msbServices = Some(BusinessMatchingMsbServices(Set(TransmittingMoney)))
                    )))


                  val model = Renewal(
                    Some(InvolvedInOtherYes("test")),
                    Some(BusinessTurnover.First),
                    Some(AMLSTurnover.First),
                    Some(CustomersOutsideUK(None)),
                    Some(PercentageOfCashPaymentOver15000.First),
                    Some(ReceiveCashPayments(Some(PaymentMethods(true, true, Some("other"))))),
                    Some(TotalThroughput("01")),
                    None,
                    Some(TransactionsInLast12Months("1500")),
                    None,
                    None,
                    None,
                    None,
                    hasChanged = true,
                    Some(SendMoneyToOtherCountry(false))
                  )

                  await(service.isRenewalComplete(model)) mustBe true
                }
                "involvedInOther is false" in new Fixture {

                  when(dataCache.fetchAll(any(), any()))
                    .thenReturn(Future.successful(Some(mockCacheMap)))

                  when(mockCacheMap.getEntry[BusinessMatching](any())(any()))
                    .thenReturn(Some(BusinessMatching(
                      activities = Some(BusinessActivities(Set(
                        MoneyServiceBusiness,
                        HighValueDealing
                      ))),
                      msbServices = Some(BusinessMatchingMsbServices(Set(TransmittingMoney)))
                    )))

                  val model = Renewal(
                    Some(InvolvedInOtherNo),
                    None,
                    Some(AMLSTurnover.First),
                    Some(CustomersOutsideUK(None)),
                    Some(PercentageOfCashPaymentOver15000.First),
                    Some(ReceiveCashPayments(Some(PaymentMethods(true, true, Some("other"))))),
                    Some(TotalThroughput("01")),
                    None,
                    Some(TransactionsInLast12Months("1500")),
                    None,
                    None,
                    None,
                    None,
                    hasChanged = true,
                    Some(SendMoneyToOtherCountry(false))
                  )

                  await(service.isRenewalComplete(model)) mustBe true
                }
              }
            }
            "it is NOT a TransmittingMoney" when {
              "involvedInOther is true" in new Fixture {

                when(dataCache.fetchAll(any(), any()))
                  .thenReturn(Future.successful(Some(mockCacheMap)))

                when(mockCacheMap.getEntry[BusinessMatching](any())(any()))
                  .thenReturn(Some(BusinessMatching(
                    activities = Some(BusinessActivities(Set(
                      MoneyServiceBusiness,
                      HighValueDealing
                    ))),
                    msbServices = Some(BusinessMatchingMsbServices(Set(ChequeCashingNotScrapMetal)))
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
                  None,
                  None,
                  None,
                  None,
                  hasChanged = true,
                  Some(SendMoneyToOtherCountry(false))
                )

                await(service.isRenewalComplete(model)) mustBe true
              }
              "involvedInOther is false" in new Fixture {

                when(dataCache.fetchAll(any(), any()))
                  .thenReturn(Future.successful(Some(mockCacheMap)))

                when(mockCacheMap.getEntry[BusinessMatching](any())(any()))
                  .thenReturn(Some(BusinessMatching(
                    activities = Some(BusinessActivities(Set(
                      MoneyServiceBusiness,
                      HighValueDealing
                    ))),
                    msbServices = Some(BusinessMatchingMsbServices(Set(ChequeCashingNotScrapMetal)))
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
                  None,
                  None,
                  None,
                  None,
                  None,
                  hasChanged = true,
                  Some(SendMoneyToOtherCountry(false))
                )

                await(service.isRenewalComplete(model)) mustBe true
              }
            }

          }
        }

        "it is NOT an HVD" when {
          "it is a CurrencyExchange" when {
            "it is a TransmittingMoney" when {
              "there are customers outside the UK" when {
                "involvedInOther is true" in new Fixture {

                  when(dataCache.fetchAll(any(), any()))
                    .thenReturn(Future.successful(Some(mockCacheMap)))

                  when(mockCacheMap.getEntry[BusinessMatching](any())(any()))
                    .thenReturn(Some(BusinessMatching(
                      activities = Some(BusinessActivities(Set(
                        MoneyServiceBusiness
                      ))),
                      msbServices = Some(BusinessMatchingMsbServices(Set(CurrencyExchange, TransmittingMoney)))
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
                    None,
                    hasChanged = true,
                    Some(SendMoneyToOtherCountry(true))
                  )

                  await(service.isRenewalComplete(model)) mustBe true
                }
                "involvedInOther is false" in new Fixture {

                  when(dataCache.fetchAll(any(), any()))
                    .thenReturn(Future.successful(Some(mockCacheMap)))

                  when(mockCacheMap.getEntry[BusinessMatching](any())(any()))
                    .thenReturn(Some(BusinessMatching(
                      activities = Some(BusinessActivities(Set(
                        MoneyServiceBusiness
                      ))),
                      msbServices = Some(BusinessMatchingMsbServices(Set(CurrencyExchange, TransmittingMoney)))
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
                    None,
                    hasChanged = true,
                    Some(SendMoneyToOtherCountry(true))
                  )

                  await(service.isRenewalComplete(model)) mustBe true
                }
              }
              "they do not send money to other countries" when {
                "involvedInOther is true" in new Fixture {

                  when(dataCache.fetchAll(any(), any()))
                    .thenReturn(Future.successful(Some(mockCacheMap)))

                  when(mockCacheMap.getEntry[BusinessMatching](any())(any()))
                    .thenReturn(Some(BusinessMatching(
                      activities = Some(BusinessActivities(Set(
                        MoneyServiceBusiness
                      ))),
                      msbServices = Some(BusinessMatchingMsbServices(Set(CurrencyExchange, TransmittingMoney)))
                    )))

                  val model = Renewal(
                    Some(InvolvedInOtherYes("test")),
                    Some(BusinessTurnover.First),
                    Some(AMLSTurnover.First),
                    Some(CustomersOutsideUK(None)),
                    None,
                    None,
                    Some(TotalThroughput("01")),
                    Some(WhichCurrencies(Seq("EUR"), None, None, None, None)),
                    Some(TransactionsInLast12Months("1500")),
                    None,
                    None,
                    Some(CETransactionsInLast12Months("123")),
                    None,
                    hasChanged = true,
                    Some(SendMoneyToOtherCountry(false))
                  )

                  await(service.isRenewalComplete(model)) mustBe true
                }
                "involvedInOther is false" in new Fixture {

                  when(dataCache.fetchAll(any(), any()))
                    .thenReturn(Future.successful(Some(mockCacheMap)))

                  when(mockCacheMap.getEntry[BusinessMatching](any())(any()))
                    .thenReturn(Some(BusinessMatching(
                      activities = Some(BusinessActivities(Set(
                        MoneyServiceBusiness
                      ))),
                      msbServices = Some(BusinessMatchingMsbServices(Set(CurrencyExchange, TransmittingMoney)))
                    )))

                  val model = Renewal(
                    Some(InvolvedInOtherNo),
                    None,
                    Some(AMLSTurnover.First),
                    Some(CustomersOutsideUK(None)),
                    None,
                    None,
                    Some(TotalThroughput("01")),
                    Some(WhichCurrencies(Seq("EUR"), None, None, None, None)),
                    Some(TransactionsInLast12Months("1500")),
                    None,
                    None,
                    Some(CETransactionsInLast12Months("123")),
                    None,
                    hasChanged = true,
                    Some(SendMoneyToOtherCountry(false))
                  )

                  await(service.isRenewalComplete(model)) mustBe true
                }
              }
            }
            "it is NOT a TransmittingMoney" when {
              "involvedInOther is true" in new Fixture {

                when(dataCache.fetchAll(any(), any()))
                  .thenReturn(Future.successful(Some(mockCacheMap)))

                when(mockCacheMap.getEntry[BusinessMatching](any())(any()))
                  .thenReturn(Some(BusinessMatching(
                    activities = Some(BusinessActivities(Set(
                      MoneyServiceBusiness
                    ))),
                    msbServices = Some(BusinessMatchingMsbServices(Set(CurrencyExchange)))
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
                  None,
                  None,
                  None,
                  Some(CETransactionsInLast12Months("123")),
                  None,
                  hasChanged = true,
                  None
                )

                await(service.isRenewalComplete(model)) mustBe true
              }
              "involvedInOther is false" in new Fixture {

                when(dataCache.fetchAll(any(), any()))
                  .thenReturn(Future.successful(Some(mockCacheMap)))

                when(mockCacheMap.getEntry[BusinessMatching](any())(any()))
                  .thenReturn(Some(BusinessMatching(
                    activities = Some(BusinessActivities(Set(
                      MoneyServiceBusiness
                    ))),
                    msbServices = Some(BusinessMatchingMsbServices(Set(CurrencyExchange)))
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
                  None,
                  None,
                  None,
                  Some(CETransactionsInLast12Months("123")),
                  None,
                  hasChanged = true,
                  None
                )

                await(service.isRenewalComplete(model)) mustBe true
              }
            }
          }

          "it is NOT a CurrencyExchange" when {
            "it is a TransmittingMoney" when {
              "there are customers outside the UK" when {
                "involvedInOther is true" in new Fixture {

                  when(dataCache.fetchAll(any(), any()))
                    .thenReturn(Future.successful(Some(mockCacheMap)))

                  when(mockCacheMap.getEntry[BusinessMatching](any())(any()))
                    .thenReturn(Some(BusinessMatching(
                      activities = Some(BusinessActivities(Set(
                        MoneyServiceBusiness
                      ))),
                      msbServices = Some(BusinessMatchingMsbServices(Set(TransmittingMoney)))
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
                    None,
                    hasChanged = true,
                    Some(SendMoneyToOtherCountry(true))
                  )

                  await(service.isRenewalComplete(model)) mustBe true
                }
                "involvedInOther is false" in new Fixture {

                  when(dataCache.fetchAll(any(), any()))
                    .thenReturn(Future.successful(Some(mockCacheMap)))

                  when(mockCacheMap.getEntry[BusinessMatching](any())(any()))
                    .thenReturn(Some(BusinessMatching(
                      activities = Some(BusinessActivities(Set(
                        MoneyServiceBusiness
                      ))),
                      msbServices = Some(BusinessMatchingMsbServices(Set(TransmittingMoney)))
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
                    None,
                    hasChanged = true,
                    Some(SendMoneyToOtherCountry(true))
                  )

                  await(service.isRenewalComplete(model)) mustBe true
                }
              }
              "they do not send money to other countries" when {
                "involvedInOther is true" in new Fixture {

                  when(dataCache.fetchAll(any(), any()))
                    .thenReturn(Future.successful(Some(mockCacheMap)))

                  when(mockCacheMap.getEntry[BusinessMatching](any())(any()))
                    .thenReturn(Some(BusinessMatching(
                      activities = Some(BusinessActivities(Set(
                        MoneyServiceBusiness
                      ))),
                      msbServices = Some(BusinessMatchingMsbServices(Set(TransmittingMoney)))
                    )))


                  val model = Renewal(
                    Some(InvolvedInOtherYes("test")),
                    Some(BusinessTurnover.First),
                    Some(AMLSTurnover.First),
                    Some(CustomersOutsideUK(None)),
                    None,
                    None,
                    Some(TotalThroughput("01")),
                    None,
                    Some(TransactionsInLast12Months("1500")),
                    None,
                    None,
                    None,
                    None,
                    hasChanged = true,
                    Some(SendMoneyToOtherCountry(false))
                  )

                  await(service.isRenewalComplete(model)) mustBe true
                }
                "involvedInOther is false" in new Fixture {

                  when(dataCache.fetchAll(any(), any()))
                    .thenReturn(Future.successful(Some(mockCacheMap)))

                  when(mockCacheMap.getEntry[BusinessMatching](any())(any()))
                    .thenReturn(Some(BusinessMatching(
                      activities = Some(BusinessActivities(Set(
                        MoneyServiceBusiness
                      ))),
                      msbServices = Some(BusinessMatchingMsbServices(Set(TransmittingMoney)))
                    )))

                  val model = Renewal(
                    Some(InvolvedInOtherNo),
                    None,
                    Some(AMLSTurnover.First),
                    Some(CustomersOutsideUK(None)),
                    None,
                    None,
                    Some(TotalThroughput("01")),
                    None,
                    Some(TransactionsInLast12Months("1500")),
                    None,
                    None,
                    None,
                    None,
                    hasChanged = true,
                    Some(SendMoneyToOtherCountry(false))
                  )

                  await(service.isRenewalComplete(model)) mustBe true
                }
              }
            }

            "it is NOT a TransmittingMoney" when {
              "involvedInOther is true" in new Fixture {

                when(dataCache.fetchAll(any(), any()))
                  .thenReturn(Future.successful(Some(mockCacheMap)))

                when(mockCacheMap.getEntry[BusinessMatching](any())(any()))
                  .thenReturn(Some(BusinessMatching(
                    activities = Some(BusinessActivities(Set(
                      MoneyServiceBusiness
                    ))),
                    msbServices = Some(BusinessMatchingMsbServices(Set(ChequeCashingNotScrapMetal)))
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
                  None,
                  None,
                  None,
                  None,
                  hasChanged = true,
                  sendMoneyToOtherCountry = None
                )

                await(service.isRenewalComplete(model)) mustBe true
              }
              "involvedInOther is false" in new Fixture {

                when(dataCache.fetchAll(any(), any()))
                  .thenReturn(Future.successful(Some(mockCacheMap)))

                when(mockCacheMap.getEntry[BusinessMatching](any())(any()))
                  .thenReturn(Some(BusinessMatching(
                    activities = Some(BusinessActivities(Set(
                      MoneyServiceBusiness
                    ))),
                    msbServices = Some(BusinessMatchingMsbServices(Set(ChequeCashingNotScrapMetal)))
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
                  None,
                  None,
                  None,
                  None,
                  None,
                  hasChanged = true,
                  None
                )

                await(service.isRenewalComplete(model)) mustBe true
              }
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
              None,
              hasChanged = true,
              None
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
              None,
              hasChanged = true,
              None
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
              None,
              hasChanged = true,
              None
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
              None,
              hasChanged = true,
              None
            )

            await(service.isRenewalComplete(model)) mustBe true
          }
        }
      }
    }

    "be false" when {
      "it is an HVD and customers outside the UK is not set" in new Fixture {
        when(dataCache.fetchAll(any(),any()))
          .thenReturn(Future.successful(Some(mockCacheMap)))

        when(mockCacheMap.getEntry[BusinessMatching](any())(any()))
          .thenReturn(Some(BusinessMatching(
            activities = Some(BusinessActivities(Set(
              HighValueDealing
            ))),
            msbServices = Some(BusinessMatchingMsbServices(Set(CurrencyExchange)))
          )))


        val model = Renewal(
          Some(InvolvedInOtherYes("test")),
          Some(BusinessTurnover.First),
          Some(AMLSTurnover.First),
          None,
          Some(PercentageOfCashPaymentOver15000.First),
          Some(ReceiveCashPayments(Some(PaymentMethods(true, true, Some("other"))))),
          None,
          None,
          None,
          None,
          None,
          None,
          None,
          hasChanged = true,
          None
        )

        await(service.isRenewalComplete(model)) mustBe false
      }

      "it is an ASP and customers outside the UK is set" in new Fixture {
        when(dataCache.fetchAll(any(),any()))
          .thenReturn(Future.successful(Some(mockCacheMap)))

        when(mockCacheMap.getEntry[BusinessMatching](any())(any()))
          .thenReturn(Some(BusinessMatching(
            activities = Some(BusinessActivities(Set(
              AccountancyServices
            ))),
            msbServices = Some(BusinessMatchingMsbServices(Set(CurrencyExchange)))
          )))


        val model = Renewal(
          Some(InvolvedInOtherYes("test")),
          Some(BusinessTurnover.First),
          Some(AMLSTurnover.First),
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
          hasChanged = true,
          None
        )

        await(service.isRenewalComplete(model)) mustBe false
      }

      "it is an MSB" when {
        "it is an HVD" when {
          "it is a CurrencyExchange" when {
            "it is TransmittingMoney" in new Fixture {

              when(dataCache.fetchAll(any(),any()))
                .thenReturn(Future.successful(Some(mockCacheMap)))

              when(mockCacheMap.getEntry[BusinessMatching](any())(any()))
                .thenReturn(Some(BusinessMatching(
                  activities = Some(BusinessActivities(Set(
                    MoneyServiceBusiness,
                    HighValueDealing
                  ))),
                  msbServices = Some(BusinessMatchingMsbServices(Set(CurrencyExchange, TransmittingMoney)))
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
                None,
                hasChanged = true,
                Some(SendMoneyToOtherCountry(true))
              )

              await(service.isRenewalComplete(model)) mustBe false

            }
            "it is NOT TransmittingMoney" in new Fixture {

              when(dataCache.fetchAll(any(),any()))
                .thenReturn(Future.successful(Some(mockCacheMap)))

              when(mockCacheMap.getEntry[BusinessMatching](any())(any()))
                .thenReturn(Some(BusinessMatching(
                  activities = Some(BusinessActivities(Set(
                    MoneyServiceBusiness,
                    HighValueDealing
                  ))),
                  msbServices = Some(BusinessMatchingMsbServices(Set(CurrencyExchange)))
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
                None,
                None,
                None,
                None,
                None,
                hasChanged = true,
                None
              )

              await(service.isRenewalComplete(model)) mustBe false

            }
          }
          "it is not a CurrencyExchange" when {
            "it is TransmittingMoney" in new Fixture {

              when(dataCache.fetchAll(any(),any()))
                .thenReturn(Future.successful(Some(mockCacheMap)))

              when(mockCacheMap.getEntry[BusinessMatching](any())(any()))
                .thenReturn(Some(BusinessMatching(
                  activities = Some(BusinessActivities(Set(
                    MoneyServiceBusiness,
                    HighValueDealing
                  ))),
                  msbServices = Some(BusinessMatchingMsbServices(Set(TransmittingMoney)))
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
                None,
                hasChanged = true,
                Some(SendMoneyToOtherCountry(true))
              )

              await(service.isRenewalComplete(model)) mustBe false

            }
          }
        }
        "it is NOT an HVD" when {
          "it is a CurrencyExchange" when {
            "it is TransmittingMoney" in new Fixture {

              when(dataCache.fetchAll(any(),any()))
                .thenReturn(Future.successful(Some(mockCacheMap)))

              when(mockCacheMap.getEntry[BusinessMatching](any())(any()))
                .thenReturn(Some(BusinessMatching(
                  activities = Some(BusinessActivities(Set(
                    MoneyServiceBusiness
                  ))),
                  msbServices = Some(BusinessMatchingMsbServices(Set(CurrencyExchange, TransmittingMoney)))
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
                None,
                hasChanged = true,
                Some(SendMoneyToOtherCountry(true))
              )

              await(service.isRenewalComplete(model)) mustBe false

            }
            "it is NOT TransmittingMoney" in new Fixture {

              when(dataCache.fetchAll(any(),any()))
                .thenReturn(Future.successful(Some(mockCacheMap)))

              when(mockCacheMap.getEntry[BusinessMatching](any())(any()))
                .thenReturn(Some(BusinessMatching(
                  activities = Some(BusinessActivities(Set(
                    MoneyServiceBusiness
                  ))),
                  msbServices = Some(BusinessMatchingMsbServices(Set(CurrencyExchange)))
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
                None,
                None,
                None,
                None,
                None,
                hasChanged = true,
                None
              )

              await(service.isRenewalComplete(model)) mustBe false

            }
          }
          "it is NOT a CurrencyExchange" when {
            "it is TransmittingMoney" in new Fixture {

              when(dataCache.fetchAll(any(),any()))
                .thenReturn(Future.successful(Some(mockCacheMap)))

              when(mockCacheMap.getEntry[BusinessMatching](any())(any()))
                .thenReturn(Some(BusinessMatching(
                  activities = Some(BusinessActivities(Set(
                    MoneyServiceBusiness
                  ))),
                  msbServices = Some(BusinessMatchingMsbServices(Set(TransmittingMoney)))
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
                None,
                hasChanged = true,
                Some(SendMoneyToOtherCountry(true))
              )

              await(service.isRenewalComplete(model)) mustBe false

            }
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
              None,
              hasChanged = true,
              None
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
              None,
              hasChanged = true,
              None
            )

            await(service.isRenewalComplete(model)) mustBe false

        }
      }
    }
  }

  "canSubmit" must {
    "return true" when {
      "renewal has not started" when {
        "sections are completed and changed" in new Fixture {

          val renewal = Section("renewal", NotStarted, false, mock[Call])
          val sections = Seq(
            Section("", Completed, false, mock[Call]),
            Section("", Completed, true, mock[Call])
          )

          service.canSubmit(renewal, sections) must be(true)
        }
      }
      "renewal has started" when {
        "renewal section is complete and changed, sections are completed and changed" in new Fixture {

          val renewal = Section("renewal", Completed, true, mock[Call])
          val sections = Seq(
            Section("", Completed, false, mock[Call]),
            Section("", Completed, true, mock[Call])
          )

          service.canSubmit(renewal, sections) must be(true)
        }
        "renewal section is complete and changed, sections are completed and not changed" in new Fixture {

          val renewal = Section("renewal", Completed, true, mock[Call])
          val sections = Seq(
            Section("", Completed, false, mock[Call]),
            Section("", Completed, false, mock[Call])
          )

          service.canSubmit(renewal, sections) must be(true)
        }
      }
    }
    "return false" when {
      "renewal has started" when {
        "sections are completed and not changed" in new Fixture {

          val renewal = Section("renewal", Started, true, mock[Call])
          val sections = Seq(
            Section("", Completed, false, mock[Call]),
            Section("", Completed, false, mock[Call])
          )

          service.canSubmit(renewal, sections) must be(false)
        }
        "sections are completed and changed" in new Fixture {

          val renewal = Section("renewal", Started, true, mock[Call])
          val sections = Seq(
            Section("", Completed, false, mock[Call]),
            Section("", Completed, true, mock[Call])
          )

          service.canSubmit(renewal, sections) must be(false)
        }
      }
    }
  }
}
