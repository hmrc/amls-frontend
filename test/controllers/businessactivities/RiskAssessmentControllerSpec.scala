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
import models.businessactivities._
import models.businessmatching.{BusinessActivities => BMBusinessActivities, MoneyServiceBusiness, AccountancyServices, BusinessMatching}
import org.jsoup.Jsoup
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import  utils.AmlsSpec
import play.api.i18n.Messages
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.AuthorisedFixture

import scala.concurrent.Future
import uk.gov.hmrc.http.HeaderCarrier

class RiskAssessmentControllerSpec extends AmlsSpec with MockitoSugar {

  trait Fixture extends AuthorisedFixture {
    self => val request = addToken(authRequest)

    val controller = new RiskAssessmentController {

      override val dataCacheConnector: DataCacheConnector = mock[DataCacheConnector]
      override protected def authConnector: AuthConnector = self.authConnector
    }
  }

  val emptyCache = CacheMap("", Map.empty)

  "RiskAssessmentController" when {

    "get is called" must {
      "load the Risk assessment Page" in new Fixture {

        when(controller.dataCacheConnector.fetch[BusinessActivities](any())(any(), any(), any()))
          .thenReturn(Future.successful(None))

        val result = controller.get()(request)
        status(result) must be(OK)

        val document = Jsoup.parse(contentAsString(result))
        document.getElementById("hasPolicy-true").hasAttr("checked") must be(false)
        document.getElementById("hasPolicy-false").hasAttr("checked") must be(false)
        document.getElementById("riskassessments-01").hasAttr("checked") must be(false)
        document.getElementById("riskassessments-02").hasAttr("checked") must be(false)
      }

      "pre-populate the Risk assessment Page" in new Fixture {

        when(controller.dataCacheConnector.fetch[BusinessActivities](any())(any(), any(), any()))
          .thenReturn(Future.successful(Some(BusinessActivities(
            riskAssessmentPolicy = Some(RiskAssessmentPolicyYes(Set(PaperBased, Digital)))
          ))))

        val result = controller.get()(request)
        status(result) must be(OK)

        val document = Jsoup.parse(contentAsString(result))
        document.getElementById("hasPolicy-true").hasAttr("checked") must be(true)
        document.getElementById("riskassessments-01").hasAttr("checked") must be(true)
        document.getElementById("riskassessments-02").hasAttr("checked") must be(true)
      }
    }

    "post is called" must {
      "when edit is false" must {
        "on post with valid data and load check your answers page when businessActivity is ASP" in new Fixture {

          val newRequest = request.withFormUrlEncodedBody(
            "hasPolicy" -> "true",
            "riskassessments[0]" -> "01",
            "riskassessments[1]" -> "02"
          )

          val mockCacheMap = mock[CacheMap]

          when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
            .thenReturn(Some(BusinessMatching(None, Some(BMBusinessActivities(Set(AccountancyServices, MoneyServiceBusiness))))))
          when(mockCacheMap.getEntry[BusinessActivities](BusinessActivities.key))
            .thenReturn(Some(BusinessActivities(riskAssessmentPolicy = Some(RiskAssessmentPolicyNo))))

          when(controller.dataCacheConnector.fetchAll(any[HeaderCarrier], any[AuthContext]))
            .thenReturn(Future.successful(Some(mockCacheMap)))

          val result = controller.post()(newRequest)
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(controllers.businessactivities.routes.SummaryController.get().url))
        }

        "on post with valid data and load advice on MLR due to diligence page when businessActivity is not ASP" in new Fixture {

          val newRequest = request.withFormUrlEncodedBody(
            "hasPolicy" -> "true",
            "riskassessments[0]" -> "01",
            "riskassessments[1]" -> "02"
          )

          val mockCacheMap = mock[CacheMap]

          when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
            .thenReturn(Some(BusinessMatching(None, Some(BMBusinessActivities(Set(MoneyServiceBusiness))))))
          when(mockCacheMap.getEntry[BusinessActivities](BusinessActivities.key))
            .thenReturn(Some(BusinessActivities(riskAssessmentPolicy = Some(RiskAssessmentPolicyNo))))

          when(controller.dataCacheConnector.fetchAll(any[HeaderCarrier], any[AuthContext]))
            .thenReturn(Future.successful(Some(mockCacheMap)))

          val result = controller.post()(newRequest)
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(controllers.businessactivities.routes.AccountantForAMLSRegulationsController.get().url))
        }

        "redirect to the SummaryController when there is no cache data" in new Fixture {

          val newRequest = request.withFormUrlEncodedBody(
            "hasPolicy" -> "true",
            "riskassessments[0]" -> "01",
            "riskassessments[1]" -> "02"
          )

          when(controller.dataCacheConnector.fetchAll(any[HeaderCarrier], any[AuthContext]))
            .thenReturn(Future.successful(None))

          val result = controller.post()(newRequest)
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(controllers.businessactivities.routes.SummaryController.get().url))
        }

        "respond with BAD_REQUEST" when {
          "hasPolicy field is missing" in new Fixture {

            val newRequest = request.withFormUrlEncodedBody(
              "riskassessments[0]" -> "01",
              "riskassessments[1]" -> "02"
            )

            when(controller.dataCacheConnector.fetch[BusinessActivities](any())
              (any(), any(), any())).thenReturn(Future.successful(None))

            when(controller.dataCacheConnector.save[BusinessActivities](any(), any())
              (any(), any(), any())).thenReturn(Future.successful(emptyCache))

            val result = controller.post()(newRequest)
            status(result) must be(BAD_REQUEST)

            val document = Jsoup.parse(contentAsString(result))
            document.select("a[href=#hasPolicy]").html() must include(Messages("error.required.ba.option.risk.assessment"))
          }

          "hasPolicy field is missing, represented by an empty string" in new Fixture {

            val newRequest = request.withFormUrlEncodedBody(
              "hasPolicy" -> "",
              "riskassessments[0]" -> "01",
              "riskassessments[1]" -> "02"
            )

            when(controller.dataCacheConnector.fetch[BusinessActivities](any())
              (any(), any(), any())).thenReturn(Future.successful(None))

            when(controller.dataCacheConnector.save[BusinessActivities](any(), any())
              (any(), any(), any())).thenReturn(Future.successful(emptyCache))

            val result = controller.post()(newRequest)
            status(result) must be(BAD_REQUEST)

            val document = Jsoup.parse(contentAsString(result))
            document.select("a[href=#hasPolicy]").html() must include(Messages("error.required.ba.option.risk.assessment"))
          }

          "riskassessments fields are missing" in new Fixture {

            val newRequest = request.withFormUrlEncodedBody(
              "hasPolicy" -> "true"
            )

            when(controller.dataCacheConnector.fetch[BusinessActivities](any())
              (any(), any(), any())).thenReturn(Future.successful(None))

            when(controller.dataCacheConnector.save[BusinessActivities](any(), any())
              (any(), any(), any())).thenReturn(Future.successful(emptyCache))

            val result = controller.post()(newRequest)
            status(result) must be(BAD_REQUEST)

            val document = Jsoup.parse(contentAsString(result))
            document.select("a[href=#riskassessments]").html() must include(Messages("error.required.ba.risk.assessment.format"))
          }

          "riskassessments fields are missing, represented by an empty string" in new Fixture {

            val newRequest = request.withFormUrlEncodedBody(
              "hasPolicy" -> "true",
              "riskassessments[0]" -> "",
              "riskassessments[1]" -> ""
            )

            when(controller.dataCacheConnector.fetch[BusinessActivities](any())
              (any(), any(), any())).thenReturn(Future.successful(None))

            when(controller.dataCacheConnector.save[BusinessActivities](any(), any())
              (any(), any(), any())).thenReturn(Future.successful(emptyCache))

            val result = controller.post()(newRequest)
            status(result) must be(BAD_REQUEST)

            val document = Jsoup.parse(contentAsString(result))

            document.select("a[href=#riskassessments[0]-riskassessments]").html() must include(Messages("error.invalid"))
          }
        }
      }

      "when edit is true" must {
        "redirect to the SummaryController" in new Fixture {

          val newRequest = request.withFormUrlEncodedBody(
            "hasPolicy" -> "true",
            "riskassessments[0]" -> "01",
            "riskassessments[1]" -> "02"
          )

          val mockCacheMap = mock[CacheMap]

          when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
            .thenReturn(Some(BusinessMatching(None, Some(BMBusinessActivities(Set(MoneyServiceBusiness))))))
          when(mockCacheMap.getEntry[BusinessActivities](BusinessActivities.key))
            .thenReturn(Some(BusinessActivities(riskAssessmentPolicy = Some(RiskAssessmentPolicyNo))))

          when(controller.dataCacheConnector.fetchAll(any[HeaderCarrier], any[AuthContext]))
            .thenReturn(Future.successful(Some(mockCacheMap)))

          val result = controller.post(true)(newRequest)
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(routes.SummaryController.get().url))
        }
      }
    }
  }

  it must {
    "use correct services" in new Fixture {
      RiskAssessmentController.authConnector must be(AMLSAuthConnector)
      RiskAssessmentController.dataCacheConnector must be(DataCacheConnector)
    }
  }
}
