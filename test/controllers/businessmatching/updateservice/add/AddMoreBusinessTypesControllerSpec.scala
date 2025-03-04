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

import controllers.actions.SuccessfulAuthAction
import controllers.businessmatching.updateservice.AddBusinessTypeHelper
import forms.businessmatching.updateservice.add.AddMoreActivitiesFormProvider
import generators.businessmatching.BusinessMatchingGenerator
import models.businessmatching._
import models.businessmatching.BusinessActivity.{BillPaymentServices, HighValueDealing, TelephonePaymentService}
import models.flowmanagement.{AddBusinessTypeFlowModel, AddMoreBusinessTypesPageId}
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Injecting}
import services.businessmatching.BusinessMatchingService
import utils.{AmlsSpec, DependencyMocks}
import views.html.businessmatching.updateservice.add.AddMoreActivitiesView

import scala.concurrent.Future

class AddMoreBusinessTypesControllerSpec extends AmlsSpec with BusinessMatchingGenerator with Injecting {

  sealed trait Fixture extends DependencyMocks {
    self =>

    val request                     = addToken(authRequest)
    val mockBusinessMatchingService = mock[BusinessMatchingService]
    val mockUpdateServiceHelper     = mock[AddBusinessTypeHelper]
    lazy val view                   = inject[AddMoreActivitiesView]

    val controller = new AddMoreBusinessTypesController(
      authAction = SuccessfulAuthAction,
      ds = commonDependencies,
      dataCacheConnector = mockCacheConnector,
      router = createRouter[AddBusinessTypeFlowModel],
      cc = mockMcc,
      formProvider = inject[AddMoreActivitiesFormProvider],
      view = view
    )

    val BusinessActivitiesModel = BusinessActivities(Set(BillPaymentServices, TelephonePaymentService))
    val bm                      = Some(BusinessMatching(activities = Some(BusinessActivitiesModel)))
    val bmEmpty                 = Some(BusinessMatching())

    mockCacheGetEntry[BusinessMatching](Some(bm), BusinessMatching.key)

    when {
      mockBusinessMatchingService.preApplicationComplete(any())(any())
    } thenReturn Future.successful(false)

  }

  "AddMoreActivitiesController" when {

    "get is called" must {
      "return OK with add_more_activities view" in new Fixture {
        val result = controller.get()(request)

        status(result)                               must be(OK)
        Jsoup.parse(contentAsString(result)).title() must include(
          messages("businessmatching.updateservice.addmoreactivities.title")
        )
      }
    }

    "post is called" must {
      "with a valid request" must {
        "progress to the 'select Activities' page" when {
          "request equals Yes" in new Fixture {

            mockCacheUpdate[AddBusinessTypeFlowModel](Some(AddBusinessTypeFlowModel.key), AddBusinessTypeFlowModel())

            val result = controller.post()(
              FakeRequest(POST, routes.AddMoreBusinessTypesController.post().url)
                .withFormUrlEncodedBody(
                  "addmoreactivities" -> "true"
                )
            )

            status(result) mustBe SEE_OTHER
            controller.router.verify(
              "internalId",
              AddMoreBusinessTypesPageId,
              AddBusinessTypeFlowModel(addMoreActivities = Some(true))
            )
          }
        }

        "when request equals 'No'" must {
          "progress to the 'registration progress' page " when {
            "no activity that generates a section has been chosen" in new Fixture {
              val flowModel = AddBusinessTypeFlowModel(Some(BillPaymentServices))
              mockCacheUpdate[AddBusinessTypeFlowModel](Some(AddBusinessTypeFlowModel.key), flowModel)

              val result = controller.post()(
                FakeRequest(POST, routes.AddMoreBusinessTypesController.post().url)
                  .withFormUrlEncodedBody(
                    "addmoreactivities" -> "false"
                  )
              )

              status(result) mustBe SEE_OTHER
              controller.router
                .verify("internalId", AddMoreBusinessTypesPageId, flowModel.copy(addMoreActivities = Some(false)))
            }
          }

          "progress to the next page " when {
            "an activity that generates a section has been chosen" in new Fixture {
              val flowModel = AddBusinessTypeFlowModel(Some(HighValueDealing))
              mockCacheUpdate[AddBusinessTypeFlowModel](Some(AddBusinessTypeFlowModel.key), flowModel)

              val result = controller.post()(
                FakeRequest(POST, routes.AddMoreBusinessTypesController.post().url)
                  .withFormUrlEncodedBody(
                    "addmoreactivities" -> "false"
                  )
              )

              status(result) mustBe SEE_OTHER
              controller.router
                .verify("internalId", AddMoreBusinessTypesPageId, flowModel.copy(addMoreActivities = Some(false)))
            }
          }
        }
      }

      "on invalid request" must {
        "return badRequest" in new Fixture {
          val result = controller.post()(request)

          status(result) mustBe BAD_REQUEST
        }
      }
    }
  }
}
