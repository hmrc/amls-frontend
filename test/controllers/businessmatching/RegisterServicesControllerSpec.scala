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

package controllers.businessmatching

import connectors.DataCacheConnector
import models.businessmatching._
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import  utils.GenericTestHelper
import play.api.i18n.Messages
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.AuthorisedFixture

import scala.concurrent.Future

class RegisterServicesControllerSpec extends GenericTestHelper with MockitoSugar with ScalaFutures {

  trait Fixture extends AuthorisedFixture {
    self => val request = addToken(authRequest)

    val controller = new RegisterServicesController {
      override val dataCacheConnector = mock[DataCacheConnector]
      override val authConnector = self.authConnector
    }
  }

  val emptyCache = CacheMap("", Map.empty)

  "RegisterServicesController" must {

    val activityData1:Set[BusinessActivity] = Set(AccountancyServices, BillPaymentServices, EstateAgentBusinessService)
    val activityData2:Set[BusinessActivity] = Set(HighValueDealing, MoneyServiceBusiness)
    val activityData3:Set[BusinessActivity] = Set(TrustAndCompanyServices, TelephonePaymentService)

    val businessActivities1 = BusinessActivities(activityData1)
    val businessActivities2 = BusinessActivities(activityData2)

    val businessMatching1 = BusinessMatching(None, Some(businessActivities1))


    "on get display who is your agent page" in new Fixture {
      when(controller.dataCacheConnector.fetch[BusinessMatching](any())
        (any(), any(), any())).thenReturn(Future.successful(None))

      val result = controller.get()(request)
      status(result) must be(OK)
      contentAsString(result) must include("   ")
    }

    "on get() display the who is your agent page with pre populated data" in new Fixture {

      when(controller.dataCacheConnector.fetch[BusinessMatching](any())
        (any(), any(), any())).thenReturn(Future.successful(Some(businessMatching1)))

      val result = controller.get()(request)
      status(result) must be(OK)

      val document = Jsoup.parse(contentAsString(result))
      private val checkbox = document.select("input[id=businessActivities-01]")
      checkbox.attr("checked") must be("checked")
    }

    "on post with valid data" in new Fixture {

      val businessActivitiesWithData = BusinessActivities(businessActivities = activityData1)
      val businessMatchingWithData = BusinessMatching(None, Some(businessActivitiesWithData))

      val newRequest = request.withFormUrlEncodedBody(
        "businessActivities" -> "01",
        "businessActivities" -> "02",
        "businessActivities" -> "03")

      when(controller.dataCacheConnector.fetch[BusinessMatching](any())
        (any(), any(), any())).thenReturn(Future.successful(Some(businessMatchingWithData)))

      when(controller.dataCacheConnector.save[BusinessActivities](any(), any())
        (any(), any(), any())).thenReturn(Future.successful(emptyCache))

      val result = controller.post()(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(routes.SummaryController.get().url))
    }

    "on post with invalid data" in new Fixture {

      val businessActivitiesWithData = BusinessActivities(businessActivities = activityData1)
      val businessMatchingWithData = BusinessMatching(None, Some(businessActivitiesWithData))

      val newRequest = request.withFormUrlEncodedBody(
        "businessActivities" -> "11")


      when(controller.dataCacheConnector.fetch[BusinessMatching](any())
        (any(), any(), any())).thenReturn(Future.successful(Some(businessMatchingWithData)))

      when(controller.dataCacheConnector.save[BusinessMatching](any(), any())
        (any(), any(), any())).thenReturn(Future.successful(emptyCache))

      val result = controller.post()(newRequest)
      status(result) must be(BAD_REQUEST)

    }

    // to be valid after summary edit page is ready
    "on post with valid data in edit mode" in new Fixture {

      val businessActivitiesWithData = BusinessActivities(businessActivities = activityData2)
      val businessMatchingWithData = BusinessMatching(None, Some(businessActivitiesWithData))


      val newRequest = request.withFormUrlEncodedBody(
        "businessActivities" -> "01",
        "businessActivities" -> "02",
        "businessActivities" -> "03")

      when(controller.dataCacheConnector.fetch[BusinessMatching](any())
        (any(), any(), any())).thenReturn(Future.successful(Some(businessMatchingWithData)))

      when(controller.dataCacheConnector.save[BusinessMatching](any(), any())
        (any(), any(), any())).thenReturn(Future.successful(emptyCache))

      val result = controller.post(true)(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(routes.SummaryController.get().url))
    }

    "on post with valid data when option msb selected navigate to msb services page" in new Fixture {

      val businessActivities = BusinessActivities(businessActivities = Set(HighValueDealing, MoneyServiceBusiness))
      val bm = BusinessMatching(None, Some(businessActivities))


      val newRequest = request.withFormUrlEncodedBody(
        "businessActivities[0]" -> "04",
        "businessActivities[1]" -> "05")

      when(controller.dataCacheConnector.fetch[BusinessMatching](any())
        (any(), any(), any())).thenReturn(Future.successful(Some(bm)))

      when(controller.dataCacheConnector.save[BusinessMatching](any(), any())
        (any(), any(), any())).thenReturn(Future.successful(emptyCache))

      val result = controller.post(true)(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(routes.ServicesController.get(false).url))
    }


    "fail submission when no check boxes were selected" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(

      )

      when(controller.dataCacheConnector.fetch[BusinessMatching](any())
        (any(), any(), any())).thenReturn(Future.successful(None))

      when(controller.dataCacheConnector.save[BusinessMatching](any(), any())
        (any(), any(), any())).thenReturn(Future.successful(emptyCache))

      val result = controller.post()(newRequest)
      status(result) must be(BAD_REQUEST)
      val document: Document = Jsoup.parse(contentAsString(result))
      document.select("a[href=#businessActivities]").html() must include(Messages("error.required.bm.register.service"))
    }

  }
}

