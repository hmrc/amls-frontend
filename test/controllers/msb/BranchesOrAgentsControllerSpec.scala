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

package controllers.msb

import connectors.DataCacheConnector
import models.Country
import models.moneyservicebusiness.{BranchesOrAgents, MoneyServiceBusiness}
import org.jsoup.Jsoup
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.{GenericTestHelper, AuthorisedFixture}

import scala.concurrent.Future

class BranchesOrAgentsControllerSpec extends GenericTestHelper with MockitoSugar {

  trait Fixture extends AuthorisedFixture {
    self => val request = addToken(authRequest)

    val cache: DataCacheConnector = mock[DataCacheConnector]

    val controller = new BranchesOrAgentsController {
      override def cache: DataCacheConnector = self.cache
      override protected def authConnector: AuthConnector = self.authConnector
    }
  }

  "BranchesOrAgentsController" must {

    "show an empty form on get with no data in store" in new Fixture {

      when(cache.fetch[MoneyServiceBusiness](eqTo(MoneyServiceBusiness.key))(any(), any(), any()))
        .thenReturn(Future.successful(None))

      val result = controller.get()(request)
      val document = Jsoup.parse(contentAsString(result))

      status(result) mustEqual OK

      document.select("input[name=hasCountries]").size mustEqual 2
      document.select("input[name=hasCountries][checked]").size mustEqual 0
    }

    "show a prefilled form when store contains data" in new Fixture {

      val model = MoneyServiceBusiness(
        branchesOrAgents = Some(
          BranchesOrAgents(Some(Seq(Country("United Kingdom", "GB"))))
        )
      )

      when(cache.fetch[MoneyServiceBusiness](eqTo(MoneyServiceBusiness.key))(any(), any(), any()))
        .thenReturn(Future.successful(Some(model)))

      val result = controller.get()(request)
      val document = Jsoup.parse(contentAsString(result))

      status(result) mustEqual OK

      document.select("input[name=hasCountries]").size mustEqual 2
      document.select("input[name=hasCountries][checked]").`val` mustEqual "true"
      document.select("option[value=GB][selected]").size mustEqual 1
    }

    "return a Bad request with prefilled form on invalid submission" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "hasCountries" -> "true"
      )

      val result = controller.post()(newRequest)
      val document = Jsoup.parse(contentAsString(result))

      status(result) mustEqual BAD_REQUEST
    }

    "return a redirect to the 'Linked Transactions' page on valid submission" in new Fixture {

      val model = MoneyServiceBusiness(
        branchesOrAgents = Some(BranchesOrAgents(None)),
        hasChanged = true
      )

      val newRequest = request.withFormUrlEncodedBody(
        "hasCountries" -> "false"
      )

      when(cache.fetch[MoneyServiceBusiness](eqTo(MoneyServiceBusiness.key))(any(), any(), any()))
        .thenReturn(Future.successful(None))

      when(cache.save[MoneyServiceBusiness](eqTo(MoneyServiceBusiness.key), eqTo(model))(any(), any(), any()))
        .thenReturn(Future.successful(new CacheMap("", Map.empty)))

      val result = controller.post(edit = false)(newRequest)

      status(result) mustEqual SEE_OTHER
      redirectLocation(result) mustEqual Some(routes.IdentifyLinkedTransactionsController.get().url)
    }

    "return a redirect to the 'Linked Transactions' page when the user has selected 'yes' from options and has filled " +
      "the mandatory auto suggested country field" in new Fixture {

      val model = MoneyServiceBusiness(
        branchesOrAgents = Some(BranchesOrAgents(Some(Seq(Country("United Kingdom", "GB"))))),
        hasChanged = true
      )

      val newRequest = request.withFormUrlEncodedBody(
        "hasCountries" -> "true",
        "countries" -> "GB"
      )

      when(cache.fetch[MoneyServiceBusiness](eqTo(MoneyServiceBusiness.key))(any(), any(), any()))
        .thenReturn(Future.successful(None))

      when(cache.save[MoneyServiceBusiness](eqTo(MoneyServiceBusiness.key), eqTo(model))(any(), any(), any()))
        .thenReturn(Future.successful(new CacheMap("", Map.empty)))

      val result = controller.post(edit = false)(newRequest)

      status(result) mustEqual SEE_OTHER
      redirectLocation(result) mustEqual Some(routes.IdentifyLinkedTransactionsController.get().url)
    }

    "return a redirect to the 'Summary page' page on valid submission when edit flag is set" in new Fixture {

      val model = MoneyServiceBusiness(
        branchesOrAgents = Some(BranchesOrAgents(None)),
        hasChanged = true
      )

      val newRequest = request.withFormUrlEncodedBody(
        "hasCountries" -> "false"
      )

      when(cache.fetch[MoneyServiceBusiness](eqTo(MoneyServiceBusiness.key))(any(), any(), any()))
        .thenReturn(Future.successful(None))

      when(cache.save[MoneyServiceBusiness](eqTo(MoneyServiceBusiness.key), eqTo(model))(any(), any(), any()))
        .thenReturn(Future.successful(new CacheMap("", Map.empty)))

      val result = controller.post(edit = true)(newRequest)

      status(result) mustEqual SEE_OTHER
      redirectLocation(result) mustEqual Some(routes.SummaryController.get().url)
    }
  }
}
