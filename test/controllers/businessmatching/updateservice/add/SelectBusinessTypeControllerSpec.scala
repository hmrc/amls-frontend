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

package controllers.businessmatching.updateservice.add

import cats.data.OptionT
import cats.implicits._
import controllers.actions.SuccessfulAuthAction
import controllers.businessmatching.updateservice.AddBusinessTypeHelper
import forms.businessmatching.updateservice.add.SelectActivitiesFormProvider
import generators.ResponsiblePersonGenerator
import models.businessmatching._
import models.businessmatching.BusinessActivity.{AccountancyServices, BillPaymentServices, HighValueDealing}
import models.flowmanagement.AddBusinessTypeFlowModel
import models.responsiblepeople.ResponsiblePerson
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Injecting}
import services.businessmatching.BusinessMatchingService
import utils.{AmlsSpec, DependencyMocks}
import views.html.businessmatching.updateservice.add.SelectActivitiesView

import scala.concurrent.Future

class SelectBusinessTypeControllerSpec extends AmlsSpec with Injecting {

  sealed trait Fixture extends DependencyMocks with ResponsiblePersonGenerator {
    self =>

    val request                     = addToken(authRequest)
    val mockBusinessMatchingService = mock[BusinessMatchingService]
    val mockUpdateServiceHelper     = mock[AddBusinessTypeHelper]
    lazy val view                   = inject[SelectActivitiesView]
    val controller                  = new SelectBusinessTypeController(
      authAction = SuccessfulAuthAction,
      ds = commonDependencies,
      dataCacheConnector = mockCacheConnector,
      businessMatchingService = mockBusinessMatchingService,
      router = createRouter[AddBusinessTypeFlowModel],
      addHelper = mock[AddBusinessTypeHelper],
      cc = mockMcc,
      formProvider = inject[SelectActivitiesFormProvider],
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

    mockCacheFetch[AddBusinessTypeFlowModel](
      Some(AddBusinessTypeFlowModel(Some(BillPaymentServices), Some(true))),
      Some(AddBusinessTypeFlowModel.key)
    )

    mockCacheFetch[Seq[ResponsiblePerson]](Some(Seq(responsiblePersonGen.sample.get)), Some(ResponsiblePerson.key))
    mockCacheUpdate(Some(AddBusinessTypeFlowModel.key), AddBusinessTypeFlowModel())

    when {
      controller.addHelper.prefixedActivities(any())(any())
    } thenReturn Set.empty[String]
  }

  "SelectActivitiesController" when {

    "get is called" must {
      "return OK with select_activities view" in new Fixture {

        val result = controller.get()(request)

        status(result)                               must be(OK)
        Jsoup.parse(contentAsString(result)).title() must include(
          messages("businessmatching.updateservice.selectactivities.title")
        )
      }

      "return OK with select_activities view and filled form when activities are retrieved from cache" in new Fixture {

        mockCacheUpdate(Some(AddBusinessTypeFlowModel.key), AddBusinessTypeFlowModel().activity(AccountancyServices))

        val result = controller.get()(request)

        status(result)                               must be(OK)
        Jsoup.parse(contentAsString(result)).title() must include(
          messages("businessmatching.updateservice.selectactivities.title")
        )
      }

      "return 500 when cache is empty" in new Fixture {

        when(mockCacheConnector.update[AddBusinessTypeFlowModel](any(), any())(any())(any()))
          .thenReturn(Future.successful(None))

        val result = controller.get()(request)

        status(result) must be(INTERNAL_SERVER_ERROR)
      }
    }

    "post" must {
      "return a bad request when no data has been posted" in new Fixture {

        val result = controller.post()(
          FakeRequest(POST, routes.SelectBusinessTypeController.post().url)
            .withFormUrlEncodedBody("" -> "")
        )

        status(result) mustBe BAD_REQUEST
      }

      "return a 500" when {
        "invalid form is posted and business matching returns None" in new Fixture {

          when(controller.businessMatchingService.getModel(any()))
            .thenReturn(OptionT.none[Future, BusinessMatching])

          val result = controller.post()(
            FakeRequest(POST, routes.SelectBusinessTypeController.post().url)
              .withFormUrlEncodedBody("" -> "")
          )

          status(result) mustBe INTERNAL_SERVER_ERROR
        }

        "update returns None" in new Fixture {

          when(mockCacheConnector.update[AddBusinessTypeFlowModel](any(), any())(any())(any()))
            .thenReturn(Future.successful(None))

          val result = controller.post()(
            FakeRequest(POST, routes.SelectBusinessTypeController.post().url)
              .withFormUrlEncodedBody(
                "businessActivities" -> HighValueDealing.toString
              )
          )

          status(result) mustBe INTERNAL_SERVER_ERROR
        }
      }

      "return the next page in the flow when valid data has been posted" in new Fixture {
        mockCacheUpdate(Some(AddBusinessTypeFlowModel.key), AddBusinessTypeFlowModel())
        mockCacheSave[AddBusinessTypeFlowModel](
          AddBusinessTypeFlowModel(Some(HighValueDealing)),
          Some(AddBusinessTypeFlowModel.key)
        )

        val result = controller.post()(
          FakeRequest(POST, routes.SelectBusinessTypeController.post().url)
            .withFormUrlEncodedBody(
              "businessActivities" -> HighValueDealing.toString
            )
        )

        status(result) mustBe SEE_OTHER
      }
    }
  }
}
