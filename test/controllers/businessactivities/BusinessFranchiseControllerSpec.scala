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
import forms.businessactivities.BusinessFranchiseFormProvider
import models.businessactivities.BusinessFranchiseYes
import org.jsoup.Jsoup
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures

import play.api.test.Helpers._
import play.api.test.{FakeRequest, Injecting}
import services.businessactivities.BusinessFranchiseService
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.AmlsSpec
import views.html.businessactivities.BusinessFranchiseNameView

import scala.concurrent.Future

class BusinessFranchiseControllerSpec extends AmlsSpec  with ScalaFutures with Injecting with BeforeAndAfterEach {

  val mockService = mock[BusinessFranchiseService]

  trait Fixture {
    self => val request = addToken(authRequest)

    lazy val view = inject[BusinessFranchiseNameView]

    val controller = new BusinessFranchiseController (
      SuccessfulAuthAction,
      ds = commonDependencies,
      cc = mockMcc,
      service = mockService,
      formProvider = inject[BusinessFranchiseFormProvider],
      view = view)
  }

  val emptyCache = CacheMap("", Map.empty)

  override protected def beforeEach(): Unit = reset(mockService)

  "BusinessFranchiseController" when {

    "get is called" must {
      "on get display the is your business a franchise page" in new Fixture {
        when(mockService.getBusinessFranchise(any())(any()))
          .thenReturn(Future.successful(None))
        val result = controller.get()(request)
        status(result) must be(OK)
        contentAsString(result) must include(messages("businessactivities.businessfranchise.title"))

        val htmlValue = Jsoup.parse(contentAsString(result))

        htmlValue.getElementById("businessFranchise-true").hasAttr("checked") must be(false)
        htmlValue.getElementById("businessFranchise-false").hasAttr("checked") must be(false)
        htmlValue.select("input[name=franchiseName]").`val` must be("")
      }

      "on get display the is your business a franchise page with pre populated data" in new Fixture {
        when(mockService.getBusinessFranchise(any())(any()))
          .thenReturn(Future.successful(Some(BusinessFranchiseYes("test test"))))
        val result = controller.get()(request)
        status(result) must be(OK)

        val htmlValue = Jsoup.parse(contentAsString(result))

        htmlValue.getElementById("businessFranchise-true").hasAttr("checked") must be(true)
        htmlValue.getElementById("businessFranchise-false").hasAttr("checked") must be(false)
        htmlValue.select("input[name=franchiseName]").`val` must be("test test")

      }
    }

    "post is called" must {

      val franchiseName = "Company Inc."

      "respond with SEE_OTHER" when {
        "edit is false and given valid data" in new Fixture {

          val newRequest = FakeRequest(POST, routes.BusinessFranchiseController.post().url).withFormUrlEncodedBody(
            "businessFranchise" -> "true",
            "franchiseName" -> franchiseName
          )

          when(mockService.updateBusinessFranchise(any(), eqTo(BusinessFranchiseYes(franchiseName)))(any()))
            .thenReturn(Future.successful(emptyCache))

          val result = controller.post(false)(newRequest)
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(routes.EmployeeCountAMLSSupervisionController.get().url))
        }

        "edit is true and given valid data" in new Fixture {
          val newRequest = FakeRequest(POST, routes.BusinessFranchiseController.post(true).url).withFormUrlEncodedBody(
            "businessFranchise" -> "true",
            "franchiseName" -> franchiseName
          )

          when(mockService.updateBusinessFranchise(any(), eqTo(BusinessFranchiseYes(franchiseName)))(any()))
            .thenReturn(Future.successful(emptyCache))

          val result = controller.post(true)(newRequest)
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(routes.SummaryController.get.url))
        }
      }

      "respond with BAD_REQUEST" when {
        "given invalid data" in new Fixture {
          val newRequest = FakeRequest(POST, routes.BusinessFranchiseController.post().url).withFormUrlEncodedBody(
            "businessFranchise" -> "test"
          )

          val result = controller.post()(newRequest)
          status(result) must be(BAD_REQUEST)

          verifyZeroInteractions(mockService)
        }
      }
    }
  }
}
