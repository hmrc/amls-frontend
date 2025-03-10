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

package controllers.msb

import controllers.actions.SuccessfulAuthAction
import forms.msb.BranchesOrAgentsFormProvider
import models.Country
import models.moneyservicebusiness.{BranchesOrAgents, BranchesOrAgentsHasCountries, BranchesOrAgentsWhichCountries, MoneyServiceBusiness}
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Injecting}
import services.cache.Cache
import utils.{AmlsSpec, DependencyMocks}
import views.html.msb.BranchesOrAgentsView

import scala.concurrent.Future

class BranchesOrAgentsHasCountriesControllerSpec extends AmlsSpec with MockitoSugar with Injecting {

  trait Fixture extends DependencyMocks {
    self =>
    val request    = addToken(authRequest)
    lazy val view  = inject[BranchesOrAgentsView]
    val controller = new BranchesOrAgentsController(
      mockCacheConnector,
      authAction = SuccessfulAuthAction,
      ds = commonDependencies,
      mockAutoComplete,
      mockMcc,
      formProvider = inject[BranchesOrAgentsFormProvider],
      view = view
    )
  }

  "BranchesOrAgentsHasCountriesController" must {

    "show an empty form on get with no data in store" in new Fixture {

      when(mockCacheConnector.fetch[MoneyServiceBusiness](any(), eqTo(MoneyServiceBusiness.key))(any()))
        .thenReturn(Future.successful(None))

      val result   = controller.get()(request)
      val document = Jsoup.parse(contentAsString(result))

      status(result) mustEqual OK

      document.select("input[name=hasCountries]").size mustEqual 2
      document.select("input[checked]").size mustEqual 0
    }

    "show a prefilled form when store contains data" in new Fixture {

      val model = MoneyServiceBusiness(
        branchesOrAgents = Some(
          BranchesOrAgents(
            BranchesOrAgentsHasCountries(true),
            Some(BranchesOrAgentsWhichCountries(Seq(Country("United Kingdom", "GB"))))
          )
        )
      )

      when(mockCacheConnector.fetch[MoneyServiceBusiness](any(), eqTo(MoneyServiceBusiness.key))(any()))
        .thenReturn(Future.successful(Some(model)))

      val result   = controller.get()(request)
      val document = Jsoup.parse(contentAsString(result))

      status(result) mustEqual OK

      document.select("input[name=hasCountries]").size mustEqual 2
      document.select("input[name=hasCountries][checked]").`val` mustEqual "true"
    }

    "return a redirect to the 'Linked Transactions' page on valid submission" in new Fixture {

      val model = MoneyServiceBusiness(
        branchesOrAgents = Some(BranchesOrAgents(BranchesOrAgentsHasCountries(false), None)),
        hasChanged = true
      )

      val newRequest = FakeRequest(POST, routes.BranchesOrAgentsController.post().url)
        .withFormUrlEncodedBody(
          "hasCountries" -> "false"
        )

      when(mockCacheConnector.fetch[MoneyServiceBusiness](any(), eqTo(MoneyServiceBusiness.key))(any()))
        .thenReturn(Future.successful(Some(model)))

      when(mockCacheConnector.save[MoneyServiceBusiness](any(), eqTo(MoneyServiceBusiness.key), any())(any()))
        .thenReturn(Future.successful(Cache.empty))

      val result = controller.post(edit = false)(newRequest)

      status(result) mustEqual SEE_OTHER
      redirectLocation(result) mustEqual Some(routes.IdentifyLinkedTransactionsController.get().url)
    }

    "return a redirect to the 'Which Countries' page when the user has selected 'yes' from options" in new Fixture {

      val newRequest = FakeRequest(POST, routes.BranchesOrAgentsController.post().url)
        .withFormUrlEncodedBody(
          "hasCountries" -> "true"
        )

      when(mockCacheConnector.fetch[MoneyServiceBusiness](any(), eqTo(MoneyServiceBusiness.key))(any()))
        .thenReturn(Future.successful(None))

      when(mockCacheConnector.save[MoneyServiceBusiness](any(), eqTo(MoneyServiceBusiness.key), any())(any()))
        .thenReturn(Future.successful(Cache.empty))

      val result = controller.post(edit = false)(newRequest)

      status(result) mustEqual SEE_OTHER
      redirectLocation(result) mustEqual Some(routes.BranchesOrAgentsWhichCountriesController.get().url)
    }

    "return a redirect to the 'Summary page' page on valid submission when edit flag is set and answering no" in new Fixture {

      val model = MoneyServiceBusiness(
        branchesOrAgents = Some(BranchesOrAgents(BranchesOrAgentsHasCountries(false), None)),
        hasChanged = true
      )

      val newRequest = FakeRequest(POST, routes.BranchesOrAgentsController.post().url)
        .withFormUrlEncodedBody(
          "hasCountries" -> "false"
        )

      when(mockCacheConnector.fetch[MoneyServiceBusiness](any(), eqTo(MoneyServiceBusiness.key))(any()))
        .thenReturn(Future.successful(Some(model)))

      when(mockCacheConnector.save[MoneyServiceBusiness](any(), eqTo(MoneyServiceBusiness.key), any())(any()))
        .thenReturn(Future.successful(Cache.empty))

      val result = controller.post(edit = true)(newRequest)

      status(result) mustEqual SEE_OTHER
      redirectLocation(result) mustEqual Some(routes.SummaryController.get.url)
    }

    "return a redirect to the 'Which Countries' page on valid submission when edit flag is set and answering yes" in new Fixture {

      val model = MoneyServiceBusiness(
        branchesOrAgents = Some(BranchesOrAgents(BranchesOrAgentsHasCountries(true), None)),
        hasChanged = true
      )

      val newRequest = FakeRequest(POST, routes.BranchesOrAgentsController.post().url)
        .withFormUrlEncodedBody(
          "hasCountries" -> "true"
        )

      when(mockCacheConnector.fetch[MoneyServiceBusiness](any(), eqTo(MoneyServiceBusiness.key))(any()))
        .thenReturn(Future.successful(Some(model)))

      when(mockCacheConnector.save[MoneyServiceBusiness](any(), eqTo(MoneyServiceBusiness.key), any())(any()))
        .thenReturn(Future.successful(Cache.empty))

      val result = controller.post(edit = true)(newRequest)

      status(result) mustEqual SEE_OTHER
      redirectLocation(result) mustEqual Some(routes.BranchesOrAgentsWhichCountriesController.get(true).url)
    }
  }
}
