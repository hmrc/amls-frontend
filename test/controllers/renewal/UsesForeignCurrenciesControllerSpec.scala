/*
 * Copyright 2023 HM Revenue & Customs
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
import models.businessmatching._
import models.businessmatching.BusinessActivity._
import models.renewal._
import org.jsoup.Jsoup
import org.mockito.ArgumentCaptor
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import play.api.i18n.Messages
import play.api.test.Helpers._
import services.RenewalService
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.AmlsSpec
import views.html.renewal.uses_foreign_currencies

import scala.concurrent.Future

class UsesForeignCurrenciesControllerSpec extends AmlsSpec with MockitoSugar {

  trait Fixture {
    self =>
    val renewalService = mock[RenewalService]
    val request = addToken(authRequest)
    val dataCacheConnector = mock[DataCacheConnector]
    val cacheMap = mock[CacheMap]
    lazy val view = app.injector.instanceOf[uses_foreign_currencies]
    lazy val controller = new UsesForeignCurrenciesController(
      SuccessfulAuthAction,
      ds = commonDependencies,
      renewalService,
      dataCacheConnector,
      cc = mockMcc,
      uses_foreign_currencies = view)

    when {
      renewalService.getRenewal(any())(any(), any())
    } thenReturn Future.successful(Renewal().some)

    when(dataCacheConnector.fetchAll(any())(any()))
      .thenReturn(Future.successful(Some(cacheMap)))
  }

  trait FormSubmissionFixture extends Fixture {
    val validFormRequest = requestWithUrlEncodedBody(
      "currencies[0]" -> "USD",
      "currencies[1]" -> "GBP",
      "currencies[2]" -> "BOB",
      "usesForeignCurrencies" -> "true",
      "bankMoneySource" -> "Yes",
      "bankNames" -> "Bank names",
      "wholesalerMoneySource" -> "Yes",
      "wholesalerNames" -> "wholesaler names",
      "customerMoneySource" -> "Yes"
    )

    when {
      renewalService.updateRenewal(any(),any())(any(), any())
    } thenReturn Future.successful(mock[CacheMap])
  }

  trait RoutingFixture extends FormSubmissionFixture {
    val whichCurrencies = WhichCurrencies(
      Seq("USD"),
      Some(UsesForeignCurrenciesYes),
      Some(MoneySources(None,
        None,
        None)))

    val renewal = Renewal(whichCurrencies = Some(whichCurrencies))

    val msbServices = Some(
      BusinessMatchingMsbServices(
        Set(
          TransmittingMoney
        )
      )
    )

    val businessActivities = Some(
      BusinessActivities(Set(HighValueDealing,  AccountancyServices))
    )


    val expectedRenewal = renewal.copy(
      whichCurrencies = Some(whichCurrencies), hasChanged = true
    )

    when(cacheMap.getEntry[Renewal](eqTo(Renewal.key))(any()))
      .thenReturn(Some(renewal))

    when(cacheMap.getEntry[BusinessMatching](BusinessMatching.key))
      .thenReturn(Some(BusinessMatching(msbServices = msbServices, activities = businessActivities)))

    when(dataCacheConnector.save[Renewal](any(), eqTo(Renewal.key), eqTo(expectedRenewal))(any(), any()))
      .thenReturn(Future.successful(new CacheMap("", Map.empty)))

    def setupBusinessMatching(activities: Set[BusinessActivity], msbServices: Set[BusinessMatchingMsbService]) = when {
      cacheMap.getEntry[BusinessMatching](BusinessMatching.key)
    } thenReturn Some(BusinessMatching(msbServices = Some(BusinessMatchingMsbServices(msbServices)), activities = Some(BusinessActivities(activities))))
  }

  "Calling the GET action" must {
    "return the correct view" when {
      "edit is false" in new Fixture {
        val result = controller.get()(request)

        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.select(".heading-xlarge").text mustBe Messages("renewal.msb.foreign_currencies.header")
      }
    }
  }

  "Calling the POST action" when {
    "posting valid data" must {
      "redirect to the summary page" when {
        "editing and answer is no" in new RoutingFixture {
          setupBusinessMatching(Set(HighValueDealing), Set(TransmittingMoney))

          val validFormRequest2 = requestWithUrlEncodedBody(
            "currencies[0]" -> "USD",
            "currencies[1]" -> "GBP",
            "currencies[2]" -> "BOB",
            "usesForeignCurrencies" -> "false",
            "bankMoneySource" -> "Yes",
            "bankNames" -> "Bank names",
            "wholesalerMoneySource" -> "Yes",
            "wholesalerNames" -> "wholesaler names",
            "customerMoneySource" -> "Yes"
          )

          val result = controller.post(edit = true)(validFormRequest2)

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe controllers.renewal.routes.SummaryController.get.url.some
        }
      }
      "Redirect to the MoneySources page" when {
        "editing and answer is yes" in new RoutingFixture {
          setupBusinessMatching(Set(HighValueDealing), Set(TransmittingMoney))

          val result = controller.post(edit = true)(validFormRequest)

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe controllers.renewal.routes.MoneySourcesController.get(true).url.some
        }
      }

      "save the model data into the renewal object" in new RoutingFixture {

        val result = await(controller.post()(validFormRequest))
        val captor = ArgumentCaptor.forClass(classOf[Renewal])

        verify(renewalService).updateRenewal(any(), captor.capture())(any(), any())

        captor.getValue.whichCurrencies mustBe Some(WhichCurrencies(
          Seq("USD"),
          Some(UsesForeignCurrenciesYes),
          Some(MoneySources(None,
            None,
            None))
        ))
      }
    }

    "return a bad request" when {
      "the form fails validation" in new FormSubmissionFixture {
        val result = controller.post()(request)

        status(result) mustBe BAD_REQUEST
        verify(renewalService, never()).updateRenewal(any(),any())(any(), any())
      }
    }
  }
}
