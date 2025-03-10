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

package controllers.businessactivities

import controllers.actions.SuccessfulAuthAction
import forms.businessactivities.EmployeeCountAMLSSupervisionFormProvider
import models.businessactivities.EmployeeCountAMLSSupervision
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.ArgumentMatchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc.{AnyContentAsEmpty, AnyContentAsFormUrlEncoded, Request, Result}
import play.api.test
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Injecting}
import services.businessactivities.EmployeeCountAMLSSupervisionService
import services.cache.Cache
import utils.AmlsSpec
import views.html.businessactivities.BusinessEmployeesAMLSSupervisionView

import scala.concurrent.Future

class EmployeeCountAMLSSupervisionControllerSpec extends AmlsSpec with MockitoSugar with ScalaFutures with Injecting {

  val mockService: EmployeeCountAMLSSupervisionService = mock[EmployeeCountAMLSSupervisionService]

  trait Fixture {
    self =>
    val request: Request[AnyContentAsEmpty.type]        = addToken(authRequest)
    lazy val view: BusinessEmployeesAMLSSupervisionView = app.injector.instanceOf[BusinessEmployeesAMLSSupervisionView]
    val controller                                      = new EmployeeCountAMLSSupervisionController(
      SuccessfulAuthAction,
      ds = commonDependencies,
      cc = mockMcc,
      service = mockService,
      formProvider = inject[EmployeeCountAMLSSupervisionFormProvider],
      view = view
    )
  }

  val emptyCache: Cache = Cache.empty

  "EmployeeCountAMLSSupervisionController" when {

    "get is called" must {
      "display the how many employees work on activities covered by AMLS page with an empty form" in new Fixture {

        when(mockService.getEmployeeCountAMLSSupervision(any()))
          .thenReturn(Future.successful(None))

        val result: Future[Result] = controller.get()(request)
        status(result) must be(OK)

        val document: Document = Jsoup.parse(contentAsString(result))

        document.select("input[name=employeeCountAMLSSupervision]").`val` must be("")
      }

      "display the how many employees work on activities covered by AMLS page with pre populated data" in new Fixture {

        val count = "17"

        when(mockService.getEmployeeCountAMLSSupervision(any()))
          .thenReturn(Future.successful(Some(count)))

        val result: Future[Result] = controller.get()(request)
        status(result) must be(OK)

        val document: Document = Jsoup.parse(contentAsString(result))

        document.select("input[name=employeeCountAMLSSupervision]").`val` must be(count)
      }
    }

    "post is called" must {
      "respond with BAD_REQUEST when given invalid data" in new Fixture {
        val newRequest: FakeRequest[AnyContentAsFormUrlEncoded] =
          FakeRequest(POST, routes.EmployeeCountAMLSSupervisionController.post().url)
            .withFormUrlEncodedBody(
              "employeeCountAMLSSupervision" -> ""
            )
        val result: Future[Result]                              = controller.post()(newRequest)
        status(result) must be(BAD_REQUEST)
      }

      "redirect to the TransactionRecordController when given valid data and edit is false" in new Fixture {

        val count                                               = "123"
        val newRequest: FakeRequest[AnyContentAsFormUrlEncoded] = test
          .FakeRequest(POST, routes.EmployeeCountAMLSSupervisionController.post().url)
          .withFormUrlEncodedBody(
            "employeeCountAMLSSupervision" -> count
          )

        when(mockService.updateHowManyEmployees(any(), eqTo(EmployeeCountAMLSSupervision(count))))
          .thenReturn(Future.successful(Some(emptyCache)))

        val result: Future[Result] = controller.post(false)(newRequest)
        status(result)           must be(SEE_OTHER)
        redirectLocation(result) must be(Some(routes.HowManyEmployeesController.get().url))
      }

      "redirect to the SummaryController when given valid data and edit is true" in new Fixture {

        val count = "12345"

        val newRequest: FakeRequest[AnyContentAsFormUrlEncoded] = test
          .FakeRequest(POST, routes.EmployeeCountAMLSSupervisionController.post(true).url)
          .withFormUrlEncodedBody(
            "employeeCountAMLSSupervision" -> count
          )

        when(mockService.updateHowManyEmployees(any(), eqTo(EmployeeCountAMLSSupervision(count))))
          .thenReturn(Future.successful(Some(emptyCache)))

        val resultTrue: Future[Result] = controller.post(true)(newRequest)
        status(resultTrue)           must be(SEE_OTHER)
        redirectLocation(resultTrue) must be(Some(routes.SummaryController.get.url))
      }
    }
  }
}
