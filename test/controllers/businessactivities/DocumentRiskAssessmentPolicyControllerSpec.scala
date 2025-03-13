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
import models.businessmatching.{BusinessActivities => BMBusinessActivities, BusinessMatching}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc.{AnyContentAsEmpty, AnyContentAsFormUrlEncoded, Request, Result}
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Injecting}
import services.businessactivities.DocumentRiskAssessmentService
import services.cache.Cache
import utils.AmlsSpec
import views.html.businessactivities.DocumentRiskAssessmentPolicyView

import scala.concurrent.Future

class DocumentRiskAssessmentPolicyControllerSpec
    extends AmlsSpec
    with MockitoSugar
    with Injecting
    with BeforeAndAfterEach {

  val mockService: DocumentRiskAssessmentService = mock[DocumentRiskAssessmentService]

  trait Fixture {
    self =>
    val request: Request[AnyContentAsEmpty.type]    = addToken(authRequest)
    lazy val view: DocumentRiskAssessmentPolicyView = inject[DocumentRiskAssessmentPolicyView]

    val controller = new DocumentRiskAssessmentController(
      SuccessfulAuthAction,
      ds = commonDependencies,
      cc = mockMcc,
      service = mockService,
      formProvider = inject[DocumentRiskAssessmentPolicyFormProvider],
      view = view,
      error = errorView
    )
  }

  val emptyCache: Cache = Cache.empty

  override def beforeEach(): Unit = reset(mockService)

  "DocumentRiskAssessmentController" when {

    "get is called" must {
      "load the Document Risk assessment Page" in new Fixture {

        when(mockService.getRiskAssessmentPolicy(any())).thenReturn(Future.successful(None))

        val result: Future[Result] = controller.get()(request)
        status(result) must be(OK)

        val document: Document = Jsoup.parse(contentAsString(result))
        document.getElementById("riskassessments_1").hasAttr("checked") must be(false)
        document.getElementById("riskassessments_2").hasAttr("checked") must be(false)
      }

      "pre-populate the Document Risk assessment Page" in new Fixture {

        when(mockService.getRiskAssessmentPolicy(any()))
          .thenReturn(
            Future.successful(
              Some(
                RiskAssessmentPolicy(RiskAssessmentHasPolicy(true), RiskAssessmentTypes(Set(PaperBased, Digital)))
              )
            )
          )

        val result: Future[Result] = controller.get()(request)
        status(result) must be(OK)

        val document: Document = Jsoup.parse(contentAsString(result))
        document.getElementById("riskassessments_1").hasAttr("checked") must be(true)
        document.getElementById("riskassessments_2").hasAttr("checked") must be(true)
      }
    }

    "post is called" must {
      "when edit is false" must {
        "on post with valid data redirect to check your answers page when businessActivity is ASP" in new Fixture {

          val newRequest: FakeRequest[AnyContentAsFormUrlEncoded] =
            FakeRequest(POST, routes.DocumentRiskAssessmentController.post(false).url)
              .withFormUrlEncodedBody(
                "riskassessments[1]" -> "paperBased",
                "riskassessments[2]" -> "digital"
              )

          when(mockService.updateRiskAssessmentType(any(), any()))
            .thenReturn(
              Future.successful(
                Some(BusinessMatching(None, Some(BMBusinessActivities(Set(AccountancyServices, MoneyServiceBusiness)))))
              )
            )

          val result: Future[Result] = controller.post()(newRequest)
          status(result)           must be(SEE_OTHER)
          redirectLocation(result) must be(Some(controllers.businessactivities.routes.SummaryController.get.url))
        }

        "on post with valid data redirect to advice on MLR due to diligence page when businessActivity is not ASP" in new Fixture {

          val newRequest: FakeRequest[AnyContentAsFormUrlEncoded] =
            FakeRequest(POST, routes.DocumentRiskAssessmentController.post(false).url)
              .withFormUrlEncodedBody(
                "riskassessments[1]" -> "paperBased",
                "riskassessments[2]" -> "digital"
              )

          when(mockService.updateRiskAssessmentType(any(), any()))
            .thenReturn(
              Future.successful(
                Some(BusinessMatching(None, Some(BMBusinessActivities(Set(MoneyServiceBusiness)))))
              )
            )

          val result: Future[Result] = controller.post()(newRequest)
          status(result)           must be(SEE_OTHER)
          redirectLocation(result) must be(
            Some(controllers.businessactivities.routes.AccountantForAMLSRegulationsController.get().url)
          )
        }

        "respond with BAD_REQUEST" when {
          "riskassessments fields are missing" in new Fixture {

            val newRequest: FakeRequest[AnyContentAsFormUrlEncoded] =
              FakeRequest(POST, routes.DocumentRiskAssessmentController.post(false).url)
                .withFormUrlEncodedBody(
                )

            val result: Future[Result] = controller.post()(newRequest)
            status(result) must be(BAD_REQUEST)

            verifyNoInteractions(mockService)
          }

          "riskassessments fields are missing, represented by an empty string" in new Fixture {

            val newRequest: FakeRequest[AnyContentAsFormUrlEncoded] =
              FakeRequest(POST, routes.DocumentRiskAssessmentController.post(false).url)
                .withFormUrlEncodedBody(
                  "riskassessments[1]" -> "",
                  "riskassessments[2]" -> ""
                )

            val result: Future[Result] = controller.post()(newRequest)
            status(result) must be(BAD_REQUEST)

            verifyNoInteractions(mockService)
          }
        }
      }

      "when edit is true" must {
        "redirect to the SummaryController" in new Fixture {

          val newRequest: FakeRequest[AnyContentAsFormUrlEncoded] =
            FakeRequest(POST, routes.DocumentRiskAssessmentController.post(true).url)
              .withFormUrlEncodedBody(
                "riskassessments[1]" -> "paperBased",
                "riskassessments[2]" -> "digital"
              )

          when(mockService.updateRiskAssessmentType(any(), any()))
            .thenReturn(
              Future.successful(
                Some(BusinessMatching(None, Some(BMBusinessActivities(Set(MoneyServiceBusiness)))))
              )
            )

          val result: Future[Result] = controller.post(true)(newRequest)
          status(result)           must be(SEE_OTHER)
          redirectLocation(result) must be(Some(routes.SummaryController.get.url))
        }
      }
    }
  }
}
