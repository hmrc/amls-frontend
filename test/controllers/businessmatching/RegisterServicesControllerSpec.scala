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

  "RegisterServicesController" when {

    val activityData1:Set[BusinessActivity] = Set(AccountancyServices, BillPaymentServices, EstateAgentBusinessService)
    val activityData2:Set[BusinessActivity] = Set(HighValueDealing, MoneyServiceBusiness)

    val businessActivities1 = BusinessActivities(activityData1)

    val businessMatching1 = BusinessMatching(None, Some(businessActivities1))

    "get is called" must {

      "display who is your agent page" which {
        "shows empty fields" in new Fixture {

          when(controller.dataCacheConnector.fetch[BusinessMatching](any())
            (any(), any(), any())).thenReturn(Future.successful(None))

          val result = controller.get()(request)
          status(result) must be(OK)
          contentAsString(result) must include("   ")
        }

        "populates fields" in new Fixture {

          when(controller.dataCacheConnector.fetch[BusinessMatching](any())
            (any(), any(), any())).thenReturn(Future.successful(Some(businessMatching1)))

          val result = controller.get()(request)
          status(result) must be(OK)

          val document = Jsoup.parse(contentAsString(result))
          private val checkbox = document.select("input[id=businessActivities-01]")
          checkbox.attr("checked") must be("checked")
        }
      }
    }

    "post is called" when {
      "request data is valid" must {
        "redirect to SummaryController" when {
          "edit is false" in new Fixture {

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

          "edit is true" in new Fixture {

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
        }

        "redirect to ServicesController" when {
          "msb is selected" in new Fixture {

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
        }
      }

      "request data is invalid" must {
        "return BAD_REQUEST" in new Fixture {

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
      }
    }
  }

}