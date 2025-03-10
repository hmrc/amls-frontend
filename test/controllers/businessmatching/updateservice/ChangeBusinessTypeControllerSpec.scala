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

package controllers.businessmatching.updateservice

import cats.data.OptionT
import controllers.actions.SuccessfulAuthAction
import forms.businessmatching.updateservice.ChangeBusinessTypesFormProvider
import models.businessmatching._
import models.businessmatching.BusinessActivity.{HighValueDealing, MoneyServiceBusiness, TelephonePaymentService, TrustAndCompanyServices}
import models.businessmatching.updateservice.Remove
import models.businessmatching.updateservice.{Add, ChangeBusinessType}
import models.flowmanagement.{ChangeBusinessTypesPageId, RemoveBusinessTypeFlowModel}
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import play.api.i18n.Messages
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Injecting}
import services.businessmatching.BusinessMatchingService
import utils.{AmlsSpec, DependencyMocks}
import views.html.businessmatching.updateservice.ChangeServicesView

import scala.concurrent.Future

class ChangeBusinessTypeControllerSpec extends AmlsSpec with MockitoSugar with Injecting {

  sealed trait Fixture extends DependencyMocks {
    self =>

    val request   = addToken(authRequest)
    val bmService = mock[BusinessMatchingService]

    lazy val view  = inject[ChangeServicesView]
    val controller = new ChangeBusinessTypesController(
      SuccessfulAuthAction,
      ds = commonDependencies,
      mockCacheConnector,
      bmService,
      createRouter[ChangeBusinessType],
      mock[RemoveBusinessTypeHelper],
      mock[AddBusinessTypeHelper],
      cc = mockMcc,
      formProvider = inject[ChangeBusinessTypesFormProvider],
      view = view
    )

    val businessActivitiesModel = BusinessActivities(
      Set(MoneyServiceBusiness, TrustAndCompanyServices, TelephonePaymentService)
    )
    val businessMatching        = BusinessMatching(activities = Some(businessActivitiesModel))
    val emptyBusinessMatching   = BusinessMatching()

    when {
      bmService.getRemainingBusinessActivities(any())(any())
    } thenReturn OptionT.liftF[Future, Set[BusinessActivity]](Future.successful(Set(HighValueDealing)))

    when {
      controller.helper.removeFlowData(any())(any())
    } thenReturn OptionT.liftF[Future, RemoveBusinessTypeFlowModel](Future.successful(RemoveBusinessTypeFlowModel()))
  }

  "ChangeServicesController" when {

    "get is called" must {
      "return OK with change_services view" in new Fixture {

        when {
          controller.addHelper.prefixedActivities(any())(any())
        } thenReturn Set.empty[String]

        val result = controller.get()(request)

        status(result)                               must be(OK)
        Jsoup.parse(contentAsString(result)).title() must include(
          Messages("businessmatching.updateservice.changeservices.title")
        )
      }

      "return OK with change_services view - no activities" in new Fixture {

        when {
          controller.addHelper.prefixedActivities(any())(any())
        } thenReturn Set.empty[String]

        when {
          bmService.getRemainingBusinessActivities(any())(any())
        } thenReturn OptionT.liftF[Future, Set[BusinessActivity]](Future.successful(Set.empty))

        val result = controller.get()(request)

        status(result) must be(OK)

        val doc = Jsoup.parse(contentAsString(result))

        doc.title()                                      must include(Messages("businessmatching.updateservice.changeservices.title"))
        Option(doc.getElementById("changeServices-add")) must not be defined
      }
    }

    "post is called" must {
      "verify the router is called correctly" when {
        "request is add" in new Fixture {

          val result = controller.post()(
            FakeRequest(POST, routes.ChangeBusinessTypesController.post().url)
              .withFormUrlEncodedBody("changeServices" -> "add")
          )

          status(result) must be(SEE_OTHER)
          controller.router.verify("internalId", ChangeBusinessTypesPageId, Add)
        }
      }

      "verify the router is called correctly" when {
        "request is remove" in new Fixture {

          val result = controller.post()(
            FakeRequest(POST, routes.ChangeBusinessTypesController.post().url)
              .withFormUrlEncodedBody("changeServices" -> "remove")
          )

          status(result) must be(SEE_OTHER)
          controller.router.verify("internalId", ChangeBusinessTypesPageId, Remove)
        }
      }

      "return BAD_REQUEST" when {
        "request is invalid" in new Fixture {
          when {
            controller.addHelper.prefixedActivities(any())(any())
          } thenReturn Set.empty[String]

          val result = controller.post()(request)
          status(result) must be(BAD_REQUEST)
        }
      }
    }
  }
}
