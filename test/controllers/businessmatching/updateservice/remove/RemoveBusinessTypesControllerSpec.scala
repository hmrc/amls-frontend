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

package controllers.businessmatching.updateservice.remove

import cats.data.OptionT
import controllers.actions.SuccessfulAuthAction
import controllers.businessmatching.updateservice.RemoveBusinessTypeHelper
import forms.businessmatching.RemoveBusinessActivitiesFormProvider
import models.DateOfChange
import models.businessmatching.BusinessActivity._
import models.businessmatching._
import models.flowmanagement.{RemoveBusinessTypeFlowModel, WhatBusinessTypesToRemovePageId}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.businessmatching.BusinessMatchingService
import utils.{AmlsSpec, DependencyMocks}
import views.html.businessmatching.updateservice.remove.RemoveActivitiesView

import java.time.LocalDate
import scala.concurrent.Future

class RemoveBusinessTypesControllerSpec extends AmlsSpec {

  trait Fixture extends DependencyMocks {
    self =>

    val request                     = addToken(authRequest)
    val mockBusinessMatchingService = mock[BusinessMatchingService]

    val mockRemoveBusinessTypeHelper = mock[RemoveBusinessTypeHelper]
    lazy val view                    = app.injector.instanceOf[RemoveActivitiesView]

    val controller = new RemoveBusinessTypesController(
      authAction = SuccessfulAuthAction,
      ds = commonDependencies,
      dataCacheConnector = mockCacheConnector,
      businessMatchingService = mockBusinessMatchingService,
      removeBusinessTypeHelper = mockRemoveBusinessTypeHelper,
      router = createRouter[RemoveBusinessTypeFlowModel],
      cc = mockMcc,
      formProvider = new RemoveBusinessActivitiesFormProvider,
      view = view
    )

    when {
      controller.businessMatchingService.getModel(any())
    } thenReturn OptionT.liftF[Future, BusinessMatching](
      Future.successful(
        BusinessMatching(
          activities = Some(BusinessActivities(Set(BillPaymentServices)))
        )
      )
    )

    when {
      controller.businessMatchingService.getSubmittedBusinessActivities(any())(any())
    } thenReturn OptionT.liftF[Future, Set[BusinessActivity]](Future.successful(Set(BillPaymentServices)))

    mockCacheFetch[RemoveBusinessTypeFlowModel](
      Some(RemoveBusinessTypeFlowModel(Some(Set(BillPaymentServices)))),
      Some(RemoveBusinessTypeFlowModel.key)
    )
  }

  "RemoveActivitiesController" when {

    "get is called" must {
      "return OK with remove_activities view" in new Fixture {

        val result = controller.get()(request)
        status(result)                               must be(OK)
        Jsoup.parse(contentAsString(result)).title() must include(
          messages("businessmatching.updateservice.removeactivities.title.multibusinesses")
        )
      }
    }

    "post" must {
      "return a bad request when no data has been posted" in new Fixture {

        val newRequest = FakeRequest(POST, routes.RemoveBusinessTypesController.post().url)
          .withFormUrlEncodedBody(
            "value[1]" -> ""
          )

        val result = controller.post()(newRequest)

        status(result) mustBe BAD_REQUEST
      }

      "return the next page in the flow when valid data has been posted" in new Fixture {

        when {
          controller.businessMatchingService.getModel(any())
        } thenReturn OptionT.liftF[Future, BusinessMatching](
          Future.successful(
            BusinessMatching(
              activities = Some(BusinessActivities(Set(HighValueDealing, AccountancyServices)))
            )
          )
        )

        mockCacheFetch(Some(RemoveBusinessTypeFlowModel()), Some(RemoveBusinessTypeFlowModel.key))

        when(mockRemoveBusinessTypeHelper.dateOfChangeApplicable(any(), any())(any()))
          .thenReturn(OptionT.liftF[Future, Boolean](Future.successful(true)))

        mockCacheSave[RemoveBusinessTypeFlowModel]

        val newRequest = FakeRequest(POST, routes.RemoveBusinessTypesController.post().url)
          .withFormUrlEncodedBody(
            "value[1]" -> "estateAgentBusinessService"
          )

        val result = controller.post()(newRequest)

        status(result) mustBe SEE_OTHER
      }

      "save the list of business activites to the data cache" in new Fixture {
        when {
          controller.businessMatchingService.getModel(any())
        } thenReturn OptionT.liftF[Future, BusinessMatching](
          Future.successful(
            BusinessMatching(
              activities = Some(BusinessActivities(Set(HighValueDealing, AccountancyServices)))
            )
          )
        )

        val today = LocalDate.now

        val flowModel = RemoveBusinessTypeFlowModel(dateOfChange = Some(DateOfChange(today)))

        mockCacheFetch(Some(flowModel), Some(RemoveBusinessTypeFlowModel.key))

        when(mockRemoveBusinessTypeHelper.dateOfChangeApplicable(any(), any())(any()))
          .thenReturn(OptionT.liftF[Future, Boolean](Future.successful(true)))

        mockCacheSave[RemoveBusinessTypeFlowModel]

        val newRequest = FakeRequest(POST, routes.RemoveBusinessTypesController.post().url)
          .withFormUrlEncodedBody(
            "value[1]" -> "highValueDealing"
          )

        status(controller.post()(newRequest)) mustBe SEE_OTHER

        controller.router.verify(
          "internalId",
          WhatBusinessTypesToRemovePageId,
          flowModel.copy(dateOfChange = None, activitiesToRemove = Some(Set(HighValueDealing)))
        )
      }

      "wipe the date of change if its not required" in new Fixture {
        when {
          controller.businessMatchingService.getModel(any())
        } thenReturn OptionT.liftF[Future, BusinessMatching](
          Future.successful(
            BusinessMatching(
              activities = Some(BusinessActivities(Set(HighValueDealing, AccountancyServices)))
            )
          )
        )

        mockCacheFetch(
          Some(RemoveBusinessTypeFlowModel(dateOfChange = Some(DateOfChange(LocalDate.now)))),
          Some(RemoveBusinessTypeFlowModel.key)
        )

        when(mockRemoveBusinessTypeHelper.dateOfChangeApplicable(any(), any())(any()))
          .thenReturn(OptionT.liftF[Future, Boolean](Future.successful(false)))

        mockCacheSave[RemoveBusinessTypeFlowModel]

        val newRequest = FakeRequest(POST, routes.RemoveBusinessTypesController.post().url)
          .withFormUrlEncodedBody(
            "value[1]" -> "highValueDealing"
          )

        status(controller.post()(newRequest)) mustBe SEE_OTHER

        controller.router.verify(
          "internalId",
          WhatBusinessTypesToRemovePageId,
          RemoveBusinessTypeFlowModel(activitiesToRemove = Some(Set(HighValueDealing)))
        )
      }

      "wipe the date of change if the services to remove have been edited and changed" in new Fixture {
        when {
          controller.businessMatchingService.getModel(any())
        } thenReturn OptionT.liftF[Future, BusinessMatching](
          Future.successful(
            BusinessMatching(
              activities = Some(BusinessActivities(Set(HighValueDealing, AccountancyServices)))
            )
          )
        )

        mockCacheFetch(
          Some(
            RemoveBusinessTypeFlowModel(
              activitiesToRemove = Some(Set(MoneyServiceBusiness)),
              dateOfChange = Some(DateOfChange(LocalDate.now))
            )
          ),
          Some(RemoveBusinessTypeFlowModel.key)
        )

        when(mockRemoveBusinessTypeHelper.dateOfChangeApplicable(any(), any())(any()))
          .thenReturn(OptionT.liftF[Future, Boolean](Future.successful(true)))

        mockCacheSave[RemoveBusinessTypeFlowModel]

        val newRequest = FakeRequest(POST, routes.RemoveBusinessTypesController.post().url)
          .withFormUrlEncodedBody(
            "value[1]" -> "highValueDealing"
          )

        status(controller.post()(newRequest)) mustBe SEE_OTHER

        controller.router.verify(
          "internalId",
          WhatBusinessTypesToRemovePageId,
          RemoveBusinessTypeFlowModel(activitiesToRemove = Some(Set(HighValueDealing)))
        )
      }

      "leave the date of change if the services to remove have not been changed" in new Fixture {
        when {
          controller.businessMatchingService.getModel(any())
        } thenReturn OptionT.liftF[Future, BusinessMatching](
          Future.successful(
            BusinessMatching(
              activities = Some(BusinessActivities(Set(HighValueDealing, AccountancyServices)))
            )
          )
        )

        mockCacheFetch(
          Some(
            RemoveBusinessTypeFlowModel(
              activitiesToRemove = Some(Set(HighValueDealing)),
              dateOfChange = Some(DateOfChange(LocalDate.now))
            )
          ),
          Some(RemoveBusinessTypeFlowModel.key)
        )

        when(mockRemoveBusinessTypeHelper.dateOfChangeApplicable(any(), any())(any()))
          .thenReturn(OptionT.liftF[Future, Boolean](Future.successful(true)))

        mockCacheSave[RemoveBusinessTypeFlowModel]

        val newRequest = FakeRequest(POST, routes.RemoveBusinessTypesController.post().url)
          .withFormUrlEncodedBody(
            "value[1]" -> "highValueDealing"
          )

        status(controller.post()(newRequest)) mustBe SEE_OTHER

        controller.router.verify(
          "internalId",
          WhatBusinessTypesToRemovePageId,
          RemoveBusinessTypeFlowModel(
            activitiesToRemove = Some(Set(HighValueDealing)),
            dateOfChange = Some(DateOfChange(LocalDate.now))
          )
        )
      }

      "throw an error message when trying to select all business types the users has" in new Fixture {
        when {
          controller.businessMatchingService.getModel(any())
        } thenReturn OptionT.liftF[Future, BusinessMatching](
          Future.successful(
            BusinessMatching(
              activities = Some(
                BusinessActivities(
                  Set(
                    AccountancyServices,
                    BillPaymentServices,
                    EstateAgentBusinessService,
                    HighValueDealing,
                    MoneyServiceBusiness,
                    TrustAndCompanyServices,
                    TelephonePaymentService
                  )
                )
              )
            )
          )
        )

        mockCacheFetch(Some(RemoveBusinessTypeFlowModel()))

        mockCacheSave[RemoveBusinessTypeFlowModel](
          RemoveBusinessTypeFlowModel(
            Some(
              Set(
                AccountancyServices,
                BillPaymentServices,
                EstateAgentBusinessService,
                HighValueDealing,
                MoneyServiceBusiness,
                TrustAndCompanyServices,
                TelephonePaymentService
              )
            )
          ),
          Some(RemoveBusinessTypeFlowModel.key)
        )

        val newRequest = FakeRequest(POST, routes.RemoveBusinessTypesController.post().url)
          .withFormUrlEncodedBody(
            "value[1]" -> "accountancyServices",
            "value[2]" -> "billPaymentServices",
            "value[3]" -> "estateAgentBusinessService",
            "value[4]" -> "highValueDealing",
            "value[5]" -> "moneyServiceBusiness",
            "value[6]" -> "trustAndCompanyServices",
            "value[7]" -> "telephonePaymentService"
          )

        val result = controller.post()(newRequest)

        status(result) must be(BAD_REQUEST)

        val document: Document = Jsoup.parse(contentAsString(result))
        document.text() must include(messages("error.required.bm.remove.leave.one"))
      }
    }
  }
}
