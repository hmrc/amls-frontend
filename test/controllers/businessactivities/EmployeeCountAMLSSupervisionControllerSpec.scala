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

package controllers.businessactivities

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import models.businessactivities.{BusinessActivities, HowManyEmployees}
import org.jsoup.Jsoup
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.{AuthorisedFixture, GenericTestHelper}

import scala.concurrent.Future

class EmployeeCountAMLSSupervisionControllerSpec extends GenericTestHelper with MockitoSugar with ScalaFutures {

  trait Fixture extends AuthorisedFixture {
    self => val request = addToken(authRequest)
    val controller = new EmployeeCountAMLSSupervisionController {
      override val dataCacheConnector: DataCacheConnector = mock[DataCacheConnector]
      override val authConnector: AuthConnector = self.authConnector
    }
  }

  val emptyCache = CacheMap("", Map.empty)

  "EmployeeCountAMLSSupervisionController" when {

    "get is called" must {
      "display the how many employees work on activities covered by AMLS page with an empty form" in new Fixture {

        when(controller.dataCacheConnector.fetch[BusinessActivities](any())(any(), any(), any()))
          .thenReturn(Future.successful(None))

        val result = controller.get()(request)
        status(result) must be(OK)

        val document = Jsoup.parse(contentAsString(result))

        document.select("input[name=employeeCountAMLSSupervision]").`val` must be("")
      }

      "display the how many employees work on activities covered by AMLS page with pre populated data" in new Fixture {

        when(controller.dataCacheConnector.fetch[BusinessActivities](any())(any(), any(), any()))
          .thenReturn(Future.successful(Some(BusinessActivities(howManyEmployees = Some(HowManyEmployees(Some("163"), Some("17")))))))

        val result = controller.get()(request)
        status(result) must be(OK)

        val document = Jsoup.parse(contentAsString(result))

        document.select("input[name=employeeCountAMLSSupervision]").`val` must be("17")

      }
    }

    "post is called" must {
      "respond with BAD_REQUEST when given invalid data" in new Fixture {
        val newRequest = request.withFormUrlEncodedBody(
          "employeeCountAMLSSupervision" -> ""
        )
        val result = controller.post()(newRequest)
        status(result) must be(BAD_REQUEST)
      }

      "redirect to the TransactionRecordController when given valid data and edit is false" in new Fixture {

        val newRequest = request.withFormUrlEncodedBody(
          "employeeCountAMLSSupervision" -> "123"
        )

        when(controller.dataCacheConnector.fetch[BusinessActivities](any())
          (any(), any(), any())).thenReturn(Future.successful(None))

        when(controller.dataCacheConnector.save[BusinessActivities](any(), any())
          (any(), any(), any())).thenReturn(Future.successful(emptyCache))

        val result = controller.post(false)(newRequest)
        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some(routes.HowManyEmployeesController.get().url))
      }

      "redirect to the SummaryController when given valid data and edit is true" in new Fixture {

        val newRequest = request.withFormUrlEncodedBody(
          "employeeCountAMLSSupervision" -> "12345"
        )

        when(controller.dataCacheConnector.fetch[BusinessActivities](any())
          (any(), any(), any())).thenReturn(Future.successful(None))

        when(controller.dataCacheConnector.save[BusinessActivities](any(), any())
          (any(), any(), any())).thenReturn(Future.successful(emptyCache))

        val resultTrue = controller.post(true)(newRequest)
        status(resultTrue) must be(SEE_OTHER)
        redirectLocation(resultTrue) must be(Some(routes.SummaryController.get().url))

      }

    }
  }

  it must {
    "use correct services" in new Fixture {
      BusinessFranchiseController.authConnector must be(AMLSAuthConnector)
      BusinessFranchiseController.dataCacheConnector must be(DataCacheConnector)
    }
  }
}
