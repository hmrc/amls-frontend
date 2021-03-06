/*
 * Copyright 2021 HM Revenue & Customs
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
import models.businessactivities._
import models.businessmatching.{AccountancyServices, BusinessMatching, MoneyServiceBusiness, BusinessActivities => BMBusinessActivities}
import org.jsoup.Jsoup
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import utils.AmlsSpec
import play.api.i18n.Messages
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap

import scala.concurrent.Future
import uk.gov.hmrc.http.HeaderCarrier
import views.html.businessactivities.risk_assessment_policy

class RiskAssessmentPolicyControllerSpec extends AmlsSpec with MockitoSugar {

  trait Fixture {
    self => val request = addToken(authRequest)
    lazy val view = app.injector.instanceOf[risk_assessment_policy]
    val controller = new RiskAssessmentController (
      dataCacheConnector = mock[DataCacheConnector],
      SuccessfulAuthAction,
      ds = commonDependencies,
      cc = mockMcc,
      risk_assessment_policy = view,
      errorView)
  }

  val emptyCache = CacheMap("", Map.empty)

  "RiskAssessmentController" when {

    "get is called" must {
      "load the Risk assessment Page" in new Fixture {

        when(controller.dataCacheConnector.fetch[BusinessActivities](any(), any())(any(), any()))
          .thenReturn(Future.successful(None))

        val result = controller.get()(request)
        status(result) must be(OK)

        val document = Jsoup.parse(contentAsString(result))
        document.getElementById("hasPolicy-true").hasAttr("checked") must be(false)
        document.getElementById("hasPolicy-false").hasAttr("checked") must be(false)
      }

      "pre-populate the Risk assessment Page" in new Fixture {

        when(controller.dataCacheConnector.fetch[BusinessActivities](any(), any())(any(), any()))
          .thenReturn(Future.successful(Some(BusinessActivities(
            riskAssessmentPolicy = Some(RiskAssessmentPolicy(RiskAssessmentHasPolicy(true), RiskAssessmentTypes(Set(PaperBased, Digital))))
          ))))

        val result = controller.get()(request)
        status(result) must be(OK)

        val document = Jsoup.parse(contentAsString(result))
        document.getElementById("hasPolicy-true").hasAttr("checked") must be(true)
      }
    }

    "post is called" must {
      "when edit is false" must {
        "on post with valid data redirect to check your answers page when businessActivity is ASP and hasPolicy is false" in new Fixture {

          val newRequest = requestWithUrlEncodedBody(
            "hasPolicy" -> "false"
          )

          val mockCacheMap = mock[CacheMap]

          when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
            .thenReturn(Some(BusinessMatching(None, Some(BMBusinessActivities(Set(AccountancyServices, MoneyServiceBusiness))))))

          when(controller.dataCacheConnector.fetch[BusinessActivities](any(), any())(any(), any()))
            .thenReturn(Future.successful(Some(BusinessActivities(riskAssessmentPolicy = Some(RiskAssessmentPolicy(RiskAssessmentHasPolicy(false), RiskAssessmentTypes(Set())))))))

          when(controller.dataCacheConnector.save(any(), any(), any())(any(), any()))
            .thenReturn(Future.successful(mockCacheMap))

          when(controller.dataCacheConnector.fetchAll(any())(any[HeaderCarrier]))
            .thenReturn(Future.successful(Some(mockCacheMap)))

          val result = controller.post()(newRequest)
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(controllers.businessactivities.routes.SummaryController.get().url))
        }
        "on post with valid data redirect to DocumentRiskAssessment page when businessActivity is ASP and hasPolicy is true" in new Fixture {

          val newRequest = requestWithUrlEncodedBody(
            "hasPolicy" -> "true"
          )

          val mockCacheMap = mock[CacheMap]

          when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
            .thenReturn(Some(BusinessMatching(None, Some(BMBusinessActivities(Set(AccountancyServices, MoneyServiceBusiness))))))

          when(controller.dataCacheConnector.fetch[BusinessActivities](any(), any())(any(), any()))
            .thenReturn(Future.successful(Some(BusinessActivities(riskAssessmentPolicy = Some(RiskAssessmentPolicy(RiskAssessmentHasPolicy(false), RiskAssessmentTypes(Set())))))))

          when(controller.dataCacheConnector.save(any(), any(), any())(any(), any()))
            .thenReturn(Future.successful(mockCacheMap))

          when(controller.dataCacheConnector.fetchAll(any())(any[HeaderCarrier]))
            .thenReturn(Future.successful(Some(mockCacheMap)))

          val result = controller.post()(newRequest)
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(controllers.businessactivities.routes.DocumentRiskAssessmentController.get().url))
        }

        "on post with valid data redirect to DocumentRiskAssessment page when businessActivity is not ASP and hasPolicy is true" in new Fixture {

          val newRequest = requestWithUrlEncodedBody(
            "hasPolicy" -> "true"
          )

          val mockCacheMap = mock[CacheMap]

          when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
            .thenReturn(Some(BusinessMatching(None, Some(BMBusinessActivities(Set(MoneyServiceBusiness))))))

          when(controller.dataCacheConnector.fetch[BusinessActivities](any(), any())(any(), any()))
            .thenReturn(Future.successful(Some(BusinessActivities(riskAssessmentPolicy = Some(RiskAssessmentPolicy(RiskAssessmentHasPolicy(false), RiskAssessmentTypes(Set())))))))

          when(controller.dataCacheConnector.save(any(), any(), any())(any(), any()))
            .thenReturn(Future.successful(mockCacheMap))

          when(controller.dataCacheConnector.fetchAll(any())(any[HeaderCarrier]))
            .thenReturn(Future.successful(Some(mockCacheMap)))

          val result = controller.post()(newRequest)
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(controllers.businessactivities.routes.DocumentRiskAssessmentController.get().url))
        }
        "on post with valid data redirect to advice on MLR due to diligence page when businessActivity is not ASP and hasPolicy is false" in new Fixture {

          val newRequest = requestWithUrlEncodedBody(
            "hasPolicy" -> "false"
          )

          val mockCacheMap = mock[CacheMap]

          when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
            .thenReturn(Some(BusinessMatching(None, Some(BMBusinessActivities(Set(MoneyServiceBusiness))))))

          when(controller.dataCacheConnector.fetch[BusinessActivities](any(), any())(any(), any()))
            .thenReturn(Future.successful(Some(BusinessActivities(riskAssessmentPolicy = Some(RiskAssessmentPolicy(RiskAssessmentHasPolicy(false), RiskAssessmentTypes(Set())))))))

          when(controller.dataCacheConnector.save(any(), any(), any())(any(), any()))
            .thenReturn(Future.successful(mockCacheMap))

          when(controller.dataCacheConnector.fetchAll(any())(any[HeaderCarrier]))
            .thenReturn(Future.successful(Some(mockCacheMap)))

          val result = controller.post()(newRequest)
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(controllers.businessactivities.routes.AccountantForAMLSRegulationsController.get().url))
        }

        "respond with BAD_REQUEST" when {
          "hasPolicy field is missing" in new Fixture {

            val newRequest = requestWithUrlEncodedBody(
            )

            when(controller.dataCacheConnector.fetch[BusinessActivities](any(),any())(any(), any()))
              .thenReturn(Future.successful(None))

            when(controller.dataCacheConnector.save[BusinessActivities](any(), any(), any())(any(), any()))
              .thenReturn(Future.successful(emptyCache))

            val result = controller.post()(newRequest)
            status(result) must be(BAD_REQUEST)

            val document = Jsoup.parse(contentAsString(result))
            document.select("a[href=#hasPolicy]").html() must include(Messages("error.required.ba.option.risk.assessment"))
          }

          "hasPolicy field is missing, represented by an empty string" in new Fixture {

            val newRequest = requestWithUrlEncodedBody(
              "hasPolicy" -> ""
            )

            when(controller.dataCacheConnector.fetch[BusinessActivities](any(),any())(any(), any()))
              .thenReturn(Future.successful(None))

            when(controller.dataCacheConnector.save[BusinessActivities](any(), any(), any())(any(), any()))
              .thenReturn(Future.successful(emptyCache))

            val result = controller.post()(newRequest)
            status(result) must be(BAD_REQUEST)

            val document = Jsoup.parse(contentAsString(result))
            document.select("a[href=#hasPolicy]").html() must include(Messages("error.required.ba.option.risk.assessment"))
          }
        }
      }

      "clicking continue" must {
        "redirect to the SummaryController when hasPolicy is false and is AccountancyService" in new Fixture {

          val newRequest = requestWithUrlEncodedBody(
            "hasPolicy" -> "false"
          )

          val mockCacheMap = mock[CacheMap]

          when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
            .thenReturn(Some(BusinessMatching(None, Some(BMBusinessActivities(Set(MoneyServiceBusiness, AccountancyServices))))))

          when(controller.dataCacheConnector.fetch[BusinessActivities](any(), any())(any(), any()))
            .thenReturn(Future.successful(Some(BusinessActivities(riskAssessmentPolicy = Some(RiskAssessmentPolicy(RiskAssessmentHasPolicy(false), RiskAssessmentTypes(Set())))))))

          when(controller.dataCacheConnector.save(any(), any(), any())(any(), any()))
            .thenReturn(Future.successful(mockCacheMap))

          when(controller.dataCacheConnector.fetchAll(any())(any[HeaderCarrier]))
            .thenReturn(Future.successful(Some(mockCacheMap)))

          val result = controller.post(true)(newRequest)
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(routes.SummaryController.get().url))
        }
        "redirect to the DocumentRiskAssessmentController when hasPolicy is true" in new Fixture {

          val newRequest = requestWithUrlEncodedBody(
            "hasPolicy" -> "true"
          )

          val mockCacheMap = mock[CacheMap]

          when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
            .thenReturn(Some(BusinessMatching(None, Some(BMBusinessActivities(Set(MoneyServiceBusiness))))))

          when(controller.dataCacheConnector.fetch[BusinessActivities](any(), any())(any(), any()))
            .thenReturn(Future.successful(Some(BusinessActivities(riskAssessmentPolicy = Some(RiskAssessmentPolicy(RiskAssessmentHasPolicy(false), RiskAssessmentTypes(Set())))))))

          when(controller.dataCacheConnector.save(any(), any(), any())(any(), any()))
            .thenReturn(Future.successful(mockCacheMap))

          when(controller.dataCacheConnector.fetchAll(any())(any[HeaderCarrier]))
            .thenReturn(Future.successful(Some(mockCacheMap)))

          val result = controller.post(true)(newRequest)
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(routes.DocumentRiskAssessmentController.get().url))
        }
        "redirect to the AccountantForAMLSRegulationsController when hasPolicy is false and not accountancy service" in new Fixture {

          val newRequest = requestWithUrlEncodedBody(
            "hasPolicy" -> "false"
          )

          val mockCacheMap = mock[CacheMap]

          when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
            .thenReturn(Some(BusinessMatching(None, Some(BMBusinessActivities(Set(MoneyServiceBusiness))))))

          when(controller.dataCacheConnector.fetch[BusinessActivities](any(), any())(any(), any()))
            .thenReturn(Future.successful(Some(BusinessActivities(riskAssessmentPolicy = Some(RiskAssessmentPolicy(RiskAssessmentHasPolicy(false), RiskAssessmentTypes(Set())))))))

          when(controller.dataCacheConnector.save(any(), any(), any())(any(), any()))
            .thenReturn(Future.successful(mockCacheMap))

          when(controller.dataCacheConnector.fetchAll(any())(any[HeaderCarrier]))
            .thenReturn(Future.successful(Some(mockCacheMap)))

          val result = controller.post(true)(newRequest)
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(routes.AccountantForAMLSRegulationsController.get().url))
        }
      }
    }
  }
}
