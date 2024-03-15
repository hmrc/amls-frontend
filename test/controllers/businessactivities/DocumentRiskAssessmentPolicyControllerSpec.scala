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
import forms.businessactivities.DocumentRiskAssessmentPolicyFormProvider
import models.businessactivities._
import models.businessmatching.BusinessActivity.{AccountancyServices, MoneyServiceBusiness}
import models.businessmatching.{BusinessMatching, BusinessActivities => BMBusinessActivities}
import org.jsoup.Jsoup
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Injecting}
import services.businessactivities.DocumentRiskAssessmentService
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.AmlsSpec
import views.html.businessactivities.DocumentRiskAssessmentPolicyView

import scala.concurrent.Future

class DocumentRiskAssessmentPolicyControllerSpec extends AmlsSpec with MockitoSugar with Injecting with BeforeAndAfterEach {

  val mockService = mock[DocumentRiskAssessmentService]

  trait Fixture {
    self => val request = addToken(authRequest)
    lazy val view = inject[DocumentRiskAssessmentPolicyView]

    val controller = new DocumentRiskAssessmentController(
      SuccessfulAuthAction,
      ds = commonDependencies,
      cc = mockMcc,
      service = mockService,
      formProvider = inject[DocumentRiskAssessmentPolicyFormProvider],
      view = view,
      error = errorView)
  }

  val emptyCache = CacheMap("", Map.empty)

  override def beforeEach(): Unit = reset(mockService)

  "DocumentRiskAssessmentController" when {

    "get is called" must {
      "load the Document Risk assessment Page" in new Fixture {

        when(mockService.getRiskAssessmentPolicy(any())(any())).thenReturn(Future.successful(None))

        val result = controller.get()(request)
        status(result) must be(OK)

        val document = Jsoup.parse(contentAsString(result))
        document.getElementById("riskassessments_1").hasAttr("checked") must be(false)
        document.getElementById("riskassessments_2").hasAttr("checked") must be(false)
      }

      "pre-populate the Document Risk assessment Page" in new Fixture {

        when(mockService.getRiskAssessmentPolicy(any())(any()))
          .thenReturn(Future.successful(Some(
            RiskAssessmentPolicy(RiskAssessmentHasPolicy(true), RiskAssessmentTypes(Set(PaperBased, Digital)))
          )))

        val result = controller.get()(request)
        status(result) must be(OK)

        val document = Jsoup.parse(contentAsString(result))
        document.getElementById("riskassessments_1").hasAttr("checked") must be(true)
        document.getElementById("riskassessments_2").hasAttr("checked") must be(true)
      }
    }

    "post is called" must {
      "when edit is false" must {
        "on post with valid data redirect to check your answers page when businessActivity is ASP" in new Fixture {

          val newRequest = FakeRequest(POST, routes.DocumentRiskAssessmentController.post(false).url)
          .withFormUrlEncodedBody(
            "riskassessments[1]" -> "paperBased",
            "riskassessments[2]" -> "digital"
          )

          when(mockService.updateRiskAssessmentType(any(), any())(any()))
            .thenReturn(Future.successful(
              Some(BusinessMatching(None, Some(BMBusinessActivities(Set(AccountancyServices, MoneyServiceBusiness)))))
            ))

          val result = controller.post()(newRequest)
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(controllers.businessactivities.routes.SummaryController.get.url))
        }

        "on post with valid data redirect to advice on MLR due to diligence page when businessActivity is not ASP" in new Fixture {

          val newRequest = FakeRequest(POST, routes.DocumentRiskAssessmentController.post(false).url)
          .withFormUrlEncodedBody(
            "riskassessments[1]" -> "paperBased",
            "riskassessments[2]" -> "digital"
          )

          when(mockService.updateRiskAssessmentType(any(), any())(any()))
            .thenReturn(Future.successful(
              Some(BusinessMatching(None, Some(BMBusinessActivities(Set(MoneyServiceBusiness)))))
            ))

          val result = controller.post()(newRequest)
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(controllers.businessactivities.routes.AccountantForAMLSRegulationsController.get().url))
        }

        "respond with BAD_REQUEST" when {
          "riskassessments fields are missing" in new Fixture {

            val newRequest = FakeRequest(POST, routes.DocumentRiskAssessmentController.post(false).url)
            .withFormUrlEncodedBody(
            )

            val result = controller.post()(newRequest)
            status(result) must be(BAD_REQUEST)

            verifyZeroInteractions(mockService)
          }

          "riskassessments fields are missing, represented by an empty string" in new Fixture {

            val newRequest = FakeRequest(POST, routes.DocumentRiskAssessmentController.post(false).url)
            .withFormUrlEncodedBody(
              "riskassessments[1]" -> "",
              "riskassessments[2]" -> ""
            )

            val result = controller.post()(newRequest)
            status(result) must be(BAD_REQUEST)

            verifyZeroInteractions(mockService)
          }
        }
      }

      "when edit is true" must {
        "redirect to the SummaryController" in new Fixture {

          val newRequest = FakeRequest(POST, routes.DocumentRiskAssessmentController.post(true).url)
          .withFormUrlEncodedBody(
            "riskassessments[1]" -> "paperBased",
            "riskassessments[2]" -> "digital"
          )

          when(mockService.updateRiskAssessmentType(any(), any())(any()))
            .thenReturn(Future.successful(
              Some(BusinessMatching(None, Some(BMBusinessActivities(Set(MoneyServiceBusiness)))))
            ))

          val result = controller.post(true)(newRequest)
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(routes.SummaryController.get.url))
        }
      }
    }
  }
}
