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
import forms.renewal.WhichCurrenciesFormProvider
import models.businessmatching.BusinessActivity._
import models.businessmatching.BusinessMatchingMsbService.TransmittingMoney
import models.businessmatching._
import models.renewal.{MoneySources, Renewal, UsesForeignCurrenciesYes, WhichCurrencies}
import org.jsoup.Jsoup
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Injecting}
import services.{CurrencyAutocompleteService, RenewalService}
import services.cache.Cache
import utils.AmlsSpec
import views.html.renewal.WhichCurrenciesView

import scala.concurrent.Future

class WhichCurrenciesControllerSpec extends AmlsSpec with MockitoSugar with Injecting {

  trait Fixture {
    self =>
    val renewalService     = mock[RenewalService]
    val request            = addToken(authRequest)
    val dataCacheConnector = mock[DataCacheConnector]
    val cacheMap           = mock[Cache]
    lazy val view          = inject[WhichCurrenciesView]
    lazy val controller    = new WhichCurrenciesController(
      SuccessfulAuthAction,
      ds = commonDependencies,
      renewalService,
      dataCacheConnector,
      cc = mockMcc,
      autocompleteService = inject[CurrencyAutocompleteService],
      formProvider = inject[WhichCurrenciesFormProvider],
      view = view
    )

    when {
      renewalService.getRenewal(any())
    } thenReturn Future.successful(Renewal().some)

    when(dataCacheConnector.fetchAll(any()))
      .thenReturn(Future.successful(Some(cacheMap)))
  }

  trait FormSubmissionFixture extends Fixture {
    val validFormRequest = FakeRequest(POST, routes.WhichCurrenciesController.post().url).withFormUrlEncodedBody(
      "currencies[0]" -> "USD",
      "currencies[1]" -> "GBP",
      "currencies[2]" -> "BOB"
    )

    when {
      renewalService.updateRenewal(any(), any())
    } thenReturn Future.successful(mock[Cache])
  }

  trait RoutingFixture extends FormSubmissionFixture {
    val renewal = Renewal()

    val msbServices = Some(
      BusinessMatchingMsbServices(
        Set(
          TransmittingMoney
        )
      )
    )

    val businessActivities = Some(
      BusinessActivities(Set(HighValueDealing, AccountancyServices))
    )

    val whichCurrencies =
      WhichCurrencies(Seq("USD"), Some(UsesForeignCurrenciesYes), Some(MoneySources(None, None, Some(true))))

    val expectedRenewal = renewal.copy(
      whichCurrencies = Some(whichCurrencies),
      hasChanged = true
    )

    when(cacheMap.getEntry[Renewal](eqTo(Renewal.key))(any()))
      .thenReturn(Some(renewal))

    when(cacheMap.getEntry[BusinessMatching](BusinessMatching.key))
      .thenReturn(Some(BusinessMatching(msbServices = msbServices, activities = businessActivities)))

    when(dataCacheConnector.save[Renewal](any(), eqTo(Renewal.key), eqTo(expectedRenewal))(any()))
      .thenReturn(Future.successful(Cache.empty))

    def setupBusinessMatching(activities: Set[BusinessActivity], msbServices: Set[BusinessMatchingMsbService]) = when {
      cacheMap.getEntry[BusinessMatching](BusinessMatching.key)
    } thenReturn Some(
      BusinessMatching(
        msbServices = Some(BusinessMatchingMsbServices(msbServices)),
        activities = Some(BusinessActivities(activities))
      )
    )
  }

  "Calling the GET action" must {
    "return the correct view" when {
      "edit is false" in new Fixture {
        val result = controller.get()(request)

        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.getElementsByTag("h1").text mustBe messages("renewal.msb.whichcurrencies.header")
      }

      "edit is true" in new Fixture {
        val result = controller.get(true)(request)

        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.select("form").first.attr("action") mustBe routes.WhichCurrenciesController.post(true).url
      }

      "reads the current value from the renewals model" in new Fixture {
        when {
          renewalService.getRenewal(any())
        } thenReturn Future.successful(
          Renewal(whichCurrencies = WhichCurrencies(Seq("EUR"), None, MoneySources(None, None, None).some).some).some
        )

        val result = controller.get(true)(request)
        val doc    = Jsoup.parse(contentAsString(result))

        doc.select("select[name=currencies[0]] option[selected]").attr("value") mustBe "EUR"

        verify(renewalService).getRenewal(any())
      }
    }
  }

  "Calling the POST action" when {
    "posting valid data" must {
      "redirect to the summary page" when {
        "editing" in new RoutingFixture {
          setupBusinessMatching(Set(HighValueDealing), Set(TransmittingMoney))

          val result = controller.post(edit = true)(validFormRequest)

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe controllers.renewal.routes.SummaryController.get.url.some
        }
      }

      "save the model data into the renewal object" in new RoutingFixture {
        await(controller.post()(validFormRequest))
        val captor = ArgumentCaptor.forClass(classOf[Renewal])

        verify(renewalService).updateRenewal(any(), captor.capture())

        captor.getValue.whichCurrencies mustBe Some(WhichCurrencies(Seq("USD", "GBP", "BOB")))
      }
    }

    "return a bad request" when {
      "the form fails validation" in new FormSubmissionFixture {
        val newRequest = FakeRequest(POST, routes.WhichCurrenciesController.post().url).withFormUrlEncodedBody(
          "currencies[0]" -> "1dfasdffds"
        )

        val result = controller.post()(newRequest)

        status(result) mustBe BAD_REQUEST
        verify(renewalService, never()).updateRenewal(any(), any())
      }
    }
  }
}
