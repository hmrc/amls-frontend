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
import forms.msb.BranchesOrAgentsWhichCountriesFormProvider
import models.Country
import models.moneyservicebusiness.{BranchesOrAgents, BranchesOrAgentsHasCountries, BranchesOrAgentsWhichCountries, MoneyServiceBusiness}
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc.Results.Redirect
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Injecting}
import services.msb.BranchesOrAgentsWhichCountriesService
import utils.{AmlsSpec, DependencyMocks}
import views.html.msb.BranchesOrAgentsWhichCountriesView

import scala.concurrent.Future

class BranchesOrAgentsWhichCountriesControllerSpec extends AmlsSpec with MockitoSugar with Injecting {

  trait Fixture extends DependencyMocks {
    self =>
    val request = addToken(authRequest)

    val mockService: BranchesOrAgentsWhichCountriesService = mock[BranchesOrAgentsWhichCountriesService]
    lazy val view                                          = inject[BranchesOrAgentsWhichCountriesView]
    val controller                                         = new BranchesOrAgentsWhichCountriesController(
      authAction = SuccessfulAuthAction,
      ds = commonDependencies,
      mockAutoComplete,
      mockMcc,
      branchesOrAgentsWhichCountriesService = mockService,
      formProvider = inject[BranchesOrAgentsWhichCountriesFormProvider],
      view = view
    )
  }

  val modelBefore = MoneyServiceBusiness(
    branchesOrAgents = Some(
      BranchesOrAgents(
        BranchesOrAgentsHasCountries(true),
        None
      )
    )
  )

  val branchesOrAgentsAfter = BranchesOrAgentsWhichCountries(Seq(Country("United Kingdom", "GB")))

  val modelAfter = MoneyServiceBusiness(
    branchesOrAgents = Some(
      BranchesOrAgents(
        BranchesOrAgentsHasCountries(true),
        Some(branchesOrAgentsAfter)
      )
    )
  )

  "BranchesOrAgentsWhichCountriesController" must {

    "show a prefilled form when store contains data" in new Fixture {

      when(mockService.fetchBranchesOrAgents(any())).thenReturn(Future.successful(Some(branchesOrAgentsAfter)))

      val result   = controller.get()(request)
      val document = Jsoup.parse(contentAsString(result))

      status(result) mustEqual OK

      document.select("option[value=GB][selected]").size mustEqual 1
    }

    "return a Bad request with prefilled form on invalid submission" in new Fixture {
      when(mockCacheConnector.fetch[MoneyServiceBusiness](any(), eqTo(MoneyServiceBusiness.key))(any()))
        .thenReturn(Future.successful(Some(modelBefore)))

      val newRequest = FakeRequest(POST, routes.BranchesOrAgentsWhichCountriesController.post().url)
        .withFormUrlEncodedBody(
          "countries[0]" -> "GBasdadsdas"
        )

      val result = controller.post()(newRequest)

      status(result) mustEqual BAD_REQUEST
    }

    "return a redirect to the 'Linked Transactions' page on valid submission" in new Fixture {

      val newRequest = FakeRequest(POST, routes.BranchesOrAgentsWhichCountriesController.post().url)
        .withFormUrlEncodedBody(
          "countries[0]" -> "GB"
        )
      when(mockService.fetchAndSaveBranchesOrAgents(any(), any(), any())).thenReturn(
        Future.successful(Redirect(routes.IdentifyLinkedTransactionsController.get()))
      )

      val result = controller.post(edit = false)(newRequest)

      status(result) mustEqual SEE_OTHER
      redirectLocation(result) mustBe Some(routes.IdentifyLinkedTransactionsController.get().url)
    }

    "return a redirect to the 'Linked Transactions' page when the user has filled the mandatory auto suggested country field" in new Fixture {

      val newRequest = FakeRequest(POST, routes.BranchesOrAgentsWhichCountriesController.post().url)
        .withFormUrlEncodedBody(
          "countries[0]" -> "GB"
        )

      when(mockService.fetchAndSaveBranchesOrAgents(any(), any(), any())).thenReturn(
        Future.successful(Redirect(routes.IdentifyLinkedTransactionsController.get()))
      )

      val result = controller.post(edit = false)(newRequest)

      status(result) mustEqual SEE_OTHER
      redirectLocation(result) mustBe Some(routes.IdentifyLinkedTransactionsController.get().url)
    }

    "return a redirect to the 'Summary page' page on valid submission when edit flag is set" in new Fixture {

      val newRequest = FakeRequest(POST, routes.BranchesOrAgentsWhichCountriesController.post().url)
        .withFormUrlEncodedBody(
          "countries[0]" -> "GB"
        )

      when(mockService.fetchAndSaveBranchesOrAgents(any(), any(), any())).thenReturn(
        Future.successful(Redirect(routes.SummaryController.get))
      )

      val result = controller.post(edit = true)(newRequest)

      status(result) mustEqual SEE_OTHER
      redirectLocation(result) mustBe Some(routes.SummaryController.get.url)
    }
  }
}
