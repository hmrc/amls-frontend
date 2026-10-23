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
    val mockHelper = mock[ChangeSubSectorHelper]
    lazy val view = app.injector.instanceOf[FrnNeedMoreInfoView]
    val controller = new FrnNeedMoreInfoController(
      SuccessfulAuthAction,
      ds = commonDependencies,
      cc = mockMcc,
      createRouter2[ChangeSubSectorFlowModel],
      mockCacheConnector,
      mockHelper,
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

        "respond with SEE_OTHER and redirect to correct page router when includeCompanyNotRegistered is false" in new Fixture {

          val flowModel = ChangeSubSectorFlowModel(Some(Set(TransmittingMoney)), Some(BusinessAppliedForFrnYes("1234567")))
          val pageId = FrnNeedMoreInfoPageId

          when(mockCacheConnector.update[ChangeSubSectorFlowModel](any(), any())(any()) (any()))
            .thenReturn(Future.successful(Some(flowModel)))

          when(mockHelper.updateSubSectors(any(), any())(any()))
            .thenReturn(Future.successful((mock[MoneyServiceBusiness], mock[BusinessMatching], Seq.empty)))

          val result = controller.post(includeCompanyNotRegistered = false)(request)

          status(result) mustBe SEE_OTHER
          controller.router.verify(
            "internalId",
            pageId,
            flowModel,
            edit = false,
            includeCompanyNotRegistered = false
          )
        }

        "respond with SEE_OTHER and redirect to the correct page router when includeCompanyNotRegistered is true" in new Fixture {

          val flowModel = ChangeSubSectorFlowModel(Some(Set(TransmittingMoney)), Some(BusinessAppliedForFrnYes("1234567")))
          val pageId = FrnNeedMoreInfoPageId

          when(mockCacheConnector.update[ChangeSubSectorFlowModel](any(), any())(any()) (any()))
            .thenReturn(Future.successful(Some(flowModel)))

          when(mockHelper.updateSubSectors(any(), any())(any()))
            .thenReturn(Future.successful((mock[MoneyServiceBusiness], mock[BusinessMatching], Seq.empty)))

          val result = controller.post(includeCompanyNotRegistered = true)(request)

          status(result) mustBe SEE_OTHER
          controller.router.verify(
            "internalId",
            pageId,
            flowModel,
            edit = false,
            includeCompanyNotRegistered = true
          )
        }

        "respond with SEE_OTHER, redirect to correct page router and frn is transformed when 7 digits" in new Fixture {
          val transformedFrn: String = "700000"
          val flowModel = ChangeSubSectorFlowModel(Some(Set(TransmittingMoney)), Some(BusinessAppliedForFrnYes("1234567")))
          val pageId = FrnNeedMoreInfoPageId


          when(mockCacheConnector.update[ChangeSubSectorFlowModel](any(), any())(any())(any()))
            .thenAnswer { cacheUpdateCall =>
               val updateFunction = cacheUpdateCall.getArgument[Option[ChangeSubSectorFlowModel] => ChangeSubSectorFlowModel](2)
               Future.successful(Some(updateFunction(Some(flowModel))))
             }

          when(mockHelper.updateSubSectors(any(), any())(any()))
            .thenReturn(Future.successful((mock[MoneyServiceBusiness], mock[BusinessMatching], Seq.empty)))

          val result = controller.post(includeCompanyNotRegistered = false)(request)

          status(result) mustBe SEE_OTHER
          controller.router.verify(
            "internalId",
            pageId,
            ChangeSubSectorFlowModel(Some(Set(TransmittingMoney)), Some(BusinessAppliedForFrnYes(transformedFrn))),
            edit = false,
            includeCompanyNotRegistered = false
          )
        }

        "respond with SEE_OTHER and redirect to correct page route and frn is not 7 digits" in new Fixture {
          val flowModel = ChangeSubSectorFlowModel(Some(Set(TransmittingMoney)), Some(BusinessAppliedForFrnYes("123456")))
          val pageId = FrnNeedMoreInfoPageId


          when(mockCacheConnector.update[ChangeSubSectorFlowModel](any(), any())(any())(any()))
            .thenAnswer { cacheUpdateCall =>
              val updateFunction = cacheUpdateCall.getArgument[Option[ChangeSubSectorFlowModel] => ChangeSubSectorFlowModel](2)
              Future.successful(Some(updateFunction(Some(flowModel))))
            }

          when(mockHelper.updateSubSectors(any(), any())(any()))
            .thenReturn(Future.successful((mock[MoneyServiceBusiness], mock[BusinessMatching], Seq.empty)))

          val result = controller.post(includeCompanyNotRegistered = false)(request)

          status(result) mustBe SEE_OTHER
          controller.router.verify(
            "internalId",
            pageId,
            flowModel,
            edit = false,
            includeCompanyNotRegistered = false
          )
        }
      }

      "the flow model is not found" must {

        "throw an exception" in new Fixture {

          when(mockCacheConnector.update[ChangeSubSectorFlowModel](any(), any())(any())(any()))
            .thenAnswer { cacheUpdateCall =>
               val updateFunction = cacheUpdateCall.getArgument[Option[ChangeSubSectorFlowModel] => ChangeSubSectorFlowModel](2)
               Future.successful(Some(updateFunction(None)))
             }

          val result = controller.post(includeCompanyNotRegistered = false)(request)

          val exception = intercept[Exception] {
            await(result)
          }

          exception.getMessage mustBe "Flow model not found"

          verify(controller.router, never())
            .getRoute(any(), any(), any(), any(), any())(any(), any())
        }
        }

      "the flow model does not update" must {

        "throw an exception" in new Fixture {
          val flowModel = ChangeSubSectorFlowModel(Some(Set(TransmittingMoney)), Some(BusinessAppliedForFrnYes("1234567")))


          when(mockCacheConnector.update[ChangeSubSectorFlowModel](any(), any())(any())(any()))
            .thenReturn(Future.successful(None))

          when(mockHelper.updateSubSectors(any(), any())(any()))
            .thenReturn(Future.successful(None))

          val result = controller.post(includeCompanyNotRegistered = false)(request)

          val exception = intercept[Exception] {
            await(result)
          }

          exception.getMessage mustBe "Flow model not updated"

          verify(controller.router, never())
            .getRoute(any(), any(), any(), any(), any())(any(), any())
        }
      }
      }
    }
  }

