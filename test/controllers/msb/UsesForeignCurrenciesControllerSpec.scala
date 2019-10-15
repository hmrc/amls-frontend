/*
 * Copyright 2019 HM Revenue & Customs
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

package controllers.msb

import controllers.actions.SuccessfulAuthAction
import models.businessmatching.updateservice.ServiceChangeRegister
import models.businessmatching.{BusinessActivities, BusinessMatching, BusinessMatchingMsbServices, ForeignExchange, MoneyServiceBusiness => MoneyServiceBusinessActivity}
import models.moneyservicebusiness._
import models.status.NotCompleted
import org.jsoup.Jsoup
import org.mockito.ArgumentCaptor
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatest.MustMatchers
import org.scalatest.concurrent.{IntegrationPatience, PatienceConfiguration, ScalaFutures}
import org.scalatest.mockito.MockitoSugar
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.{AmlsSpec, AuthorisedFixture, DependencyMocks}

import scala.concurrent.{ExecutionContext, Future}

class UsesForeignCurrenciesControllerSpec extends AmlsSpec
                                    with MockitoSugar
                                    with MustMatchers
                                    with PatienceConfiguration
                                    with IntegrationPatience
                                    with ScalaFutures {

  trait Fixture extends DependencyMocks {
    self =>
    val request = addToken(authRequest)
    implicit val ec = app.injector.instanceOf[ExecutionContext]

    when(mockCacheConnector.fetch[MoneyServiceBusiness](any(), eqTo(MoneyServiceBusiness.key))(any(), any()))
      .thenReturn(Future.successful(None))

    when(mockCacheConnector.save[MoneyServiceBusiness](any(), any(), any())(any(), any()))
      .thenReturn(Future.successful(CacheMap("TESTID", Map())))

    val controller = new UsesForeignCurrenciesController(dataCacheConnector = mockCacheConnector,
      authAction = SuccessfulAuthAction, ds = commonDependencies,
      statusService = mockStatusService,
      serviceFlow = mockServiceFlow, cc = mockMcc)

    mockIsNewActivityNewAuth(false)
    mockCacheFetch[ServiceChangeRegister](None, Some(ServiceChangeRegister.key))

    val cacheMap = mock[CacheMap]

    when(controller.dataCacheConnector.fetchAll(any())(any()))
      .thenReturn(Future.successful(Some(cacheMap)))
    val msbServices = Some(BusinessMatchingMsbServices(Set()))
    when(cacheMap.getEntry[MoneyServiceBusiness](MoneyServiceBusiness.key))
      .thenReturn(Some(MoneyServiceBusiness()))
    when(cacheMap.getEntry[BusinessMatching](BusinessMatching.key))
      .thenReturn(Some(BusinessMatching(msbServices = msbServices)))
  }

  trait Fixture2 extends AuthorisedFixture with DependencyMocks with MoneyServiceBusinessTestData {
    self =>
    val request = addToken(authRequest)
    val controller = new UsesForeignCurrenciesController(SuccessfulAuthAction, ds = commonDependencies, mockCacheConnector, mockStatusService, mockServiceFlow, mockMcc)
    implicit val ec = app.injector.instanceOf[ExecutionContext]

    when {
      mockStatusService.isPreSubmission(any(), any(), any())(any(), any())
    } thenReturn Future.successful(true)

    val emptyCache = CacheMap("", Map.empty)

    val outgoingModel = completeMsb.copy(
      whichCurrencies = Some(WhichCurrencies(
        Seq("USD", "GBP", "EUR"),
        Some(UsesForeignCurrenciesNo),
        Some(MoneySources())
      )),
      hasChanged = true,
      hasAccepted = false
    )

    mockCacheFetch[MoneyServiceBusiness](Some(completeMsb), Some(MoneyServiceBusiness.key))

      when(mockCacheMap.getEntry[MoneyServiceBusiness](any())(any()))
      .thenReturn(Some(completeMsb))

      when(mockCacheMap.getEntry[BusinessMatching](eqTo(BusinessMatching.key))(any()))
      .thenReturn(Some(BusinessMatching(
        activities = Some(BusinessActivities(Set(MoneyServiceBusinessActivity))),
        msbServices = Some(BusinessMatchingMsbServices(Set(ForeignExchange)))
      )))

      when(controller.dataCacheConnector.fetchAll(any())(any()))
      .thenReturn(Future.successful(Some(mockCacheMap)))

    when(controller.dataCacheConnector.save(any(), eqTo(MoneyServiceBusiness.key), any())(any(), any()))
      .thenReturn(Future.successful(mockCacheMap))

    mockCacheGetEntry[ServiceChangeRegister](None, ServiceChangeRegister.key)

  }

  "UsesForeignCurrenciesController" when {
    "get is called" should {
      "succeed" when {
        "status is pre-submission" in new Fixture {
          mockApplicationStatus(NotCompleted)

          val resp = controller.get(false).apply(request)
          status(resp) must be(200)
        }

        "status is approved but the service has just been added" in new Fixture {
          mockApplicationStatus(NotCompleted)

          mockIsNewActivityNewAuth(true, Some(MoneyServiceBusinessActivity))

          val resp = controller.get(false).apply(request)
          status(resp) must be(200)
        }
      }

      "show a pre-populated form when model contains data" in new Fixture {

        val currentModel = WhichCurrencies(
          Seq("USD"),
          Some(UsesForeignCurrenciesYes),
          Some(MoneySources(None,
            None,
            Some(true))))

        mockApplicationStatus(NotCompleted)

        when(mockCacheConnector.fetch[MoneyServiceBusiness](any(), eqTo(MoneyServiceBusiness.key))(any(), any()))
          .thenReturn(Future.successful(Some(MoneyServiceBusiness(whichCurrencies = Some(currentModel)))))

        val result = controller.get()(request)
        val document = Jsoup.parse(contentAsString(result))

        status(result) mustEqual OK

        document.select("select[name=currencies[0]] > option[value=USD]").hasAttr("selected")
        document.select("input[name=usesForeignCurrencies][checked]").`val` mustEqual "true"
        document.select("input[name=bankMoneySource][checked]").`val` mustEqual ""
        document.select("input[name=wholesalerMoneySource][checked]").`val` mustEqual ""
        document.select("input[name=customerMoneySource][checked]").`val` mustEqual ""
      }
    }

    "post is called " when {
      "data is valid" should {
          "clear the foreign currency data when not using foreign currencies" in new Fixture2 {

            val newRequest = requestWithUrlEncodedBody(
              "usesForeignCurrencies" -> "false"
            )

            val result = controller.post()(newRequest)
            status(result) must be(SEE_OTHER)

            val captor = ArgumentCaptor.forClass(classOf[MoneyServiceBusiness])
            verify(controller.dataCacheConnector).save[MoneyServiceBusiness](any(), eqTo(MoneyServiceBusiness.key), captor.capture())(any(), any())
            captor.getValue match {
              case result: MoneyServiceBusiness => result must be(outgoingModel)
            }
          }

        "keep the foreign currency data when using foreign currencies" in new Fixture2 {

            val newRequest = requestWithUrlEncodedBody(
              "usesForeignCurrencies" -> "true"
            )

            val result = controller.post()(newRequest)
            status(result) must be(SEE_OTHER)

            val captor = ArgumentCaptor.forClass(classOf[MoneyServiceBusiness])
            verify(controller.dataCacheConnector).save[MoneyServiceBusiness](any(), eqTo(MoneyServiceBusiness.key), captor.capture())(any(), any())
            captor.getValue match {
              case result: MoneyServiceBusiness => result must be(completeMsb)
            }
          }
        }
      }
    }
}
