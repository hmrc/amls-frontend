/*
 * Copyright 2020 HM Revenue & Customs
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

package controllers.businessactivities

import connectors.DataCacheConnector
import controllers.actions.SuccessfulAuthAction
import models.businessactivities.{BusinessActivities, HowManyEmployees}
import org.jsoup.Jsoup
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.AmlsSpec
import views.html.businessactivities.business_employees

import scala.concurrent.Future

class HowManyEmployeesControllerSpec extends AmlsSpec with MockitoSugar with ScalaFutures {

  trait Fixture {
    self => val request = addToken(authRequest)

    lazy val view = app.injector.instanceOf[business_employees]
    val controller = new HowManyEmployeesController (
      dataCacheConnector = mock[DataCacheConnector],
      SuccessfulAuthAction,
      ds = commonDependencies,
      cc = mockMcc,
      business_employees = view)
  }

  val emptyCache = CacheMap("", Map.empty)

  "HowManyEmployeesController" when {

    "get is called" must {
      "display the how many employees page with an empty form" in new Fixture {

        when(controller.dataCacheConnector.fetch[BusinessActivities](any(), any())(any(), any()))
          .thenReturn(Future.successful(None))

        val result = controller.get()(request)
        status(result) must be(OK)

        val document = Jsoup.parse(contentAsString(result))

        document.select("input[name=employeeCount]").`val` must be("")
      }

      "display the how many employees page with pre populated data" in new Fixture {

        when(controller.dataCacheConnector.fetch[BusinessActivities](any(), any())(any(), any()))
          .thenReturn(Future.successful(Some(BusinessActivities(howManyEmployees = Some(HowManyEmployees(Some("163"), Some("17")))))))

        val result = controller.get()(request)
        status(result) must be(OK)

        val document = Jsoup.parse(contentAsString(result))

        document.select("input[name=employeeCount]").`val` must be("163")

      }
    }

    "post is called" must {
      "respond with BAD_REQUEST when given invalid data" in new Fixture {
        val newRequest = requestWithUrlEncodedBody(
          "employeeCount" -> ""
        )
        val result = controller.post()(newRequest)
        status(result) must be(BAD_REQUEST)
      }

      "redirect to the EmployeeCountAMLSSupervisionController when given valid data and edit is false" in new Fixture {

        val newRequest = requestWithUrlEncodedBody(
          "employeeCount" -> "456"
        )

        when(controller.dataCacheConnector.fetch[BusinessActivities](any(), any())(any(), any()))
          .thenReturn(Future.successful(None))

        when(controller.dataCacheConnector.save[BusinessActivities](any(), any(), any())(any(), any()))
          .thenReturn(Future.successful(emptyCache))

        val result = controller.post(false)(newRequest)
        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some(routes.TransactionRecordController.get().url))
      }

      "redirect to the SummaryController when given valid data and edit is true" in new Fixture {

        val newRequest = requestWithUrlEncodedBody(
          "employeeCount" -> "54321"
        )

        when(controller.dataCacheConnector.fetch[BusinessActivities](any(), any())(any(), any()))
          .thenReturn(Future.successful(None))

        when(controller.dataCacheConnector.save[BusinessActivities](any(), any(), any())(any(), any()))
          .thenReturn(Future.successful(emptyCache))

        val resultTrue = controller.post(true)(newRequest)
        status(resultTrue) must be(SEE_OTHER)
        redirectLocation(resultTrue) must be(Some(routes.SummaryController.get().url))

      }
    }
  }
}
