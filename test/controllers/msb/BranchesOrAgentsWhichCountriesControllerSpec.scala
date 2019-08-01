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
import models.Country
import models.moneyservicebusiness.{BranchesOrAgents, BranchesOrAgentsHasCountries, BranchesOrAgentsWhichCountries, MoneyServiceBusiness}
import org.jsoup.Jsoup
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.{AmlsSpec, AuthorisedFixture, DependencyMocksNewAuth}

import scala.concurrent.Future

class BranchesOrAgentsWhichCountriesControllerSpec extends AmlsSpec with MockitoSugar {

  trait Fixture extends AuthorisedFixture with DependencyMocksNewAuth {
    self => val request = addToken(authRequest)

    val controller = new BranchesOrAgentsWhichCountriesController(mockCacheConnector, authAction = SuccessfulAuthAction, mockAutoComplete)
  }

  val modelBefore = MoneyServiceBusiness(
    branchesOrAgents = Some(BranchesOrAgents(
      BranchesOrAgentsHasCountries(true),
      None
    ))
  )

  val modelAfter = MoneyServiceBusiness(
    branchesOrAgents = Some(BranchesOrAgents(
      BranchesOrAgentsHasCountries(true),
      Some(BranchesOrAgentsWhichCountries(Seq(Country("United Kingdom", "GB"))))
    ))
  )

  "BranchesOrAgentsWhichCountriesController" must {


    "show a prefilled form when store contains data" in new Fixture {

      when(mockCacheConnector.fetch[MoneyServiceBusiness](any(), eqTo(MoneyServiceBusiness.key))(any(), any()))
        .thenReturn(Future.successful(Some(modelAfter)))

      val result = controller.get()(request)
      val document = Jsoup.parse(contentAsString(result))

      status(result) mustEqual OK

      document.select("option[value=GB][selected]").size mustEqual 1
    }

    "return a Bad request with prefilled form on invalid submission" in new Fixture {
      when(mockCacheConnector.fetch[MoneyServiceBusiness](any(), eqTo(MoneyServiceBusiness.key))(any(), any()))
        .thenReturn(Future.successful(Some(modelBefore)))


      val newRequest = request.withFormUrlEncodedBody(
        "country_1" -> "GBasdadsdas"
      )

      val result = controller.post()(newRequest)
      val document = Jsoup.parse(contentAsString(result))

      status(result) mustEqual BAD_REQUEST
    }

    "return a redirect to the 'Linked Transactions' page on valid submission" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "countries[0]" -> "GB"
      )

      when(mockCacheConnector.fetch[MoneyServiceBusiness](any(), eqTo(MoneyServiceBusiness.key))(any(), any()))
        .thenReturn(Future.successful(Some(modelBefore)))

      when(mockCacheConnector.save[MoneyServiceBusiness](any(), eqTo(MoneyServiceBusiness.key), any())(any(), any()))
        .thenReturn(Future.successful(new CacheMap("", Map.empty)))

      val result = controller.post(edit = false)(newRequest)

      status(result) mustEqual SEE_OTHER
      redirectLocation(result) mustEqual Some(routes.IdentifyLinkedTransactionsController.get().url)
    }

    "return a redirect to the 'Linked Transactions' page when the user has filled the mandatory auto suggested country field" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "countries[0]" -> "GB"
      )

      when(mockCacheConnector.fetch[MoneyServiceBusiness](any(), eqTo(MoneyServiceBusiness.key))(any(), any()))
        .thenReturn(Future.successful(Some(modelBefore)))

      when(mockCacheConnector.save[MoneyServiceBusiness](any(), eqTo(MoneyServiceBusiness.key), any())(any(), any()))
        .thenReturn(Future.successful(new CacheMap("", Map.empty)))

      val result = controller.post(edit = false)(newRequest)

      status(result) mustEqual SEE_OTHER
      redirectLocation(result) mustEqual Some(routes.IdentifyLinkedTransactionsController.get().url)
    }

    "return a redirect to the 'Summary page' page on valid submission when edit flag is set" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "countries[0]" -> "GB"
      )

      when(mockCacheConnector.fetch[MoneyServiceBusiness](any(), eqTo(MoneyServiceBusiness.key))(any(), any()))
        .thenReturn(Future.successful(Some(modelBefore)))

      when(mockCacheConnector.save[MoneyServiceBusiness](any(), eqTo(MoneyServiceBusiness.key), any())(any(), any()))
        .thenReturn(Future.successful(new CacheMap("", Map.empty)))

      val result = controller.post(edit = true)(newRequest)

      status(result) mustEqual SEE_OTHER
      redirectLocation(result) mustEqual Some(routes.SummaryController.get().url)
    }
  }
}
