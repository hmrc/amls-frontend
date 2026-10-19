/*
 * Copyright 2026 HM Revenue & Customs
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

import cats.data.OptionT
import cats.implicits.*
import controllers.actions.SuccessfulAuthAction
import controllers.businessmatching.updateservice.ChangeSubSectorHelper
import forms.businessmatching.FrnFormProvider
import generators.businessmatching.BusinessMatchingGenerator
import models.businessmatching.*
import models.businessmatching.BusinessMatchingMsbService.*
import models.businessmatching.updateservice.ServiceChangeRegister
import models.flowmanagement.{ChangeSubSectorFlowModel, FrnNeedMoreInfoPageId}
import models.moneyservicebusiness.MoneyServiceBusiness
import models.status.NotCompleted
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.ArgumentMatchers.{eq as eqTo, *}
import org.mockito.Mockito.*
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc.Results.Redirect
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import services.businessmatching.BusinessMatchingService
import services.cache.Cache
import utils.{AmlsSpec, DependencyMocks}
import views.html.businessmatching.{FrnNeedMoreInfoView, FrnNumberView}

import scala.concurrent.Future

class FrnNeedMoreInfoControllerSpec extends AmlsSpec with MockitoSugar with ScalaFutures with BusinessMatchingGenerator {

  trait Fixture extends DependencyMocks {
    self =>

    val request = addToken(authRequest)
    lazy val view = app.injector.instanceOf[FrnNeedMoreInfoView]
    val controller = new FrnNeedMoreInfoController(
      SuccessfulAuthAction,
      ds = commonDependencies,
      cc = mockMcc,
      createRouter2[ChangeSubSectorFlowModel],
      mockCacheConnector,
      mock[ChangeSubSectorHelper],
      view = view
    )

    when {
      mockStatusService.isPreSubmission(Some(any()), any(), any())(any(), any(), any())
    } thenReturn Future.successful(true)

    when {
      mockStatusService.isPending(any())
    } thenReturn false

    when {
      mockStatusService.isPreSubmission(any())
    } thenReturn true

    mockApplicationStatus(NotCompleted)

    val businessMatching = businessMatchingGen.sample.get

    val emptyCache = Cache.empty
  }
  
  "FrnNeedMoreInfoController" when {

    "get is called" must {

      "render the correct view" in new Fixture {

        val result = controller.get()(request)

        status(result) mustBe OK
        contentAsString(result) must include(messages("businessmatching.frnNeedMoreInfo.title"))
      }
    }

    "post is called" must {

      "the flow model exists" must {
        "respond with SEE_OTHER and redirect to correct page route when includeCompanyNotRegistered is false" in new Fixture {
          val flowModel = ChangeSubSectorFlowModel(Some(Set(TransmittingMoney)))
          val pageId = FrnNeedMoreInfoPageId

          when(mockCacheConnector.fetch[ChangeSubSectorFlowModel](any(), any())(any()))
            .thenReturn(Future.successful(Some(flowModel)))

          val result = controller.post(includeCompanyNotRegistered = false)(request)

          status(result) mustBe SEE_OTHER
          controller.router.verify(
            "internalId",
            pageId,
            ChangeSubSectorFlowModel(Some(Set(TransmittingMoney))),
            edit = false,
            includeCompanyNotRegistered = false
          )
        }

        "respond with SEE_OTHER and redirect to correct page route when includeCompanyNotRegistered is true" in new Fixture {
          val flowModel = ChangeSubSectorFlowModel(Some(Set(TransmittingMoney)))
          val pageId = FrnNeedMoreInfoPageId

          when(mockCacheConnector.fetch[ChangeSubSectorFlowModel](any(), any())(any()))
            .thenReturn(Future.successful(Some(flowModel)))

          val result = controller.post(includeCompanyNotRegistered = true)(request)

          status(result) mustBe SEE_OTHER
          controller.router.verify(
            "internalId",
            pageId,
            ChangeSubSectorFlowModel(Some(Set(TransmittingMoney))),
            edit = false,
            includeCompanyNotRegistered = true
          )
        }
      }

      "the flow model is not found" must {
        "throw an exception" in new Fixture {

          when(mockCacheConnector.fetch[ChangeSubSectorFlowModel](any(), any())(any()))
            .thenReturn(Future.successful(None))

          val result = controller.post(includeCompanyNotRegistered = false)(request)

          val exception = intercept[Exception] {
            await(result)
          }

          exception.getMessage mustBe "Flow model not found"

          verify(controller.router, never())
            .getRoute(any(), any(), any(), any(), any())(any(), any())
        }
        }
      }
    }
  }

