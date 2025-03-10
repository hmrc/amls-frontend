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

package controllers.renewal

import controllers.actions.SuccessfulAuthAction
import forms.renewal.AMLSTurnoverFormProvider
import models.businessmatching.BusinessActivity._
import models.businessmatching.{BusinessActivities => Activities, _}
import models.renewal.AMLSTurnover.First
import models.renewal.{AMLSTurnover, Renewal}
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Injecting}
import services.RenewalService
import services.cache.Cache
import utils.{AmlsSpec, DependencyMocks}
import views.html.renewal.AMLSTurnoverView

import scala.concurrent.Future

class AMLSTurnoverControllerSpec extends AmlsSpec with MockitoSugar with ScalaFutures with Injecting {

  trait Fixture extends DependencyMocks {
    self =>
    val request = addToken(authRequest)

    lazy val mockRenewalService = mock[RenewalService]
    lazy val view               = inject[AMLSTurnoverView]
    val controller              = new AMLSTurnoverController(
      authAction = SuccessfulAuthAction,
      ds = commonDependencies,
      renewalService = mockRenewalService,
      cc = mockMcc,
      formProvider = inject[AMLSTurnoverFormProvider],
      view = view
    )

    val businessMatching = BusinessMatching(
      activities = Some(Activities(Set(AccountancyServices)))
    )

    def testRenewal: Option[Renewal] = None

    when(controller.renewalService.getFirstBusinessActivityInLowercase(any())(any(), any()))
      .thenReturn(Future.successful(Some(AccountancyServices.getMessage().toLowerCase)))

    when(mockRenewalService.getBusinessMatching(any())).thenReturn(Future.successful(Some(businessMatching)))

    when(mockRenewalService.getRenewal(any())).thenReturn(Future.successful(testRenewal))
  }

  val emptyCache = Cache.empty

  "AMLSTurnoverController" must {

    "on get" must {

      "display AMLS Turnover page" in new Fixture {

        val result = controller.get()(request)
        status(result)          must be(OK)
        contentAsString(result) must include(messages("renewal.turnover.title"))
      }

      "display the Role Within Business page with pre populated data" in new Fixture {

        override def testRenewal = Some(Renewal(turnover = Some(First)))

        val result = controller.get()(request)
        status(result) must be(OK)

        val document = Jsoup.parse(contentAsString(result))
        document.select("input[value=zeroPlus]").hasAttr("checked") must be(true)
      }

      Activities.all foreach { activity =>
        s"display the business type is $activity" in new Fixture {

          val bMatching = BusinessMatching(
            activities = Some(Activities(Set(activity)))
          )

          when(mockRenewalService.getBusinessMatching(any())).thenReturn(Future.successful(Some(bMatching)))

          val result = controller.get()(request)
          status(result)          must be(OK)
          contentAsString(result) must include(
            messages("renewal.turnover.title", bMatching.alphabeticalBusinessActivitiesLowerCase().head.mkString)
          )
        }
      }
    }

    "on post" when {

      "given valid data" must {

        "go to renewal Summary" when {

          "in edit mode" in new Fixture {

            val newRequest = FakeRequest(POST, routes.AMLSTurnoverController.post().url).withFormUrlEncodedBody(
              "turnover" -> AMLSTurnover.First.toString
            )

            val bMatching = BusinessMatching(
              activities = Some(Activities(Set(BillPaymentServices)))
            )

            when(mockRenewalService.getRenewal(any())).thenReturn(Future.successful(None))

            when(mockRenewalService.updateRenewal(any(), any())).thenReturn(Future.successful(mockCacheMap))

            when(mockRenewalService.getBusinessMatching(any())).thenReturn(Future.successful(Some(bMatching)))

            val result = controller.post(true)(newRequest)
            status(result)           must be(SEE_OTHER)
            redirectLocation(result) must be(Some(controllers.renewal.routes.SummaryController.get.url))
          }

          "it does not have business type of ASP, HVD or MSB" in new Fixture {
            val newRequest = FakeRequest(POST, routes.AMLSTurnoverController.post().url).withFormUrlEncodedBody(
              "turnover" -> AMLSTurnover.First.toString
            )

            val bMatching = BusinessMatching(
              activities = Some(Activities(Set(BillPaymentServices)))
            )

            when(mockRenewalService.getBusinessMatching(any())).thenReturn(Future.successful(Some(bMatching)))

            when(mockRenewalService.getRenewal(any())).thenReturn(Future.successful(None))

            when(mockRenewalService.updateRenewal(any(), any())).thenReturn(Future.successful(mockCacheMap))

            val result = controller.post()(newRequest)
            status(result)           must be(SEE_OTHER)
            redirectLocation(result) must be(Some(controllers.renewal.routes.SummaryController.get.url))
          }
        }

        "go to renewal CustomerOutsideIsUKController" when {
          "it has business type of HVD and not (ASP or MSB)" in new Fixture {
            val newRequest = FakeRequest(POST, routes.AMLSTurnoverController.post().url).withFormUrlEncodedBody(
              "turnover" -> AMLSTurnover.First.toString
            )

            val bMatching = BusinessMatching(
              activities = Some(Activities(Set(HighValueDealing)))
            )

            when(mockRenewalService.getBusinessMatching(any())).thenReturn(Future.successful(Some(bMatching)))

            when(mockRenewalService.getRenewal(any())).thenReturn(Future.successful(None))

            when(mockRenewalService.updateRenewal(any(), any())).thenReturn(Future.successful(mockCacheMap))

            val result = controller.post()(newRequest)
            status(result)           must be(SEE_OTHER)
            redirectLocation(result) must be(Some(controllers.renewal.routes.CustomersOutsideIsUKController.get().url))
          }
        }

        "go to the renewal TotalThroughput page" when {

          "it has a business type of MSB but not ASP" in new Fixture {
            val newRequest = FakeRequest(POST, routes.AMLSTurnoverController.post().url).withFormUrlEncodedBody(
              "turnover" -> AMLSTurnover.First.toString
            )

            val bMatching = BusinessMatching(
              activities = Some(Activities(Set(MoneyServiceBusiness, HighValueDealing)))
            )

            when(mockRenewalService.getBusinessMatching(any())).thenReturn(Future.successful(Some(bMatching)))

            when(mockRenewalService.getRenewal(any())).thenReturn(Future.successful(None))

            when(mockRenewalService.updateRenewal(any(), any())).thenReturn(Future.successful(mockCacheMap))

            val result = controller.post()(newRequest)
            status(result)           must be(SEE_OTHER)
            redirectLocation(result) must be(Some(controllers.renewal.routes.TotalThroughputController.get().url))
          }

        }

      }

      "given invalid data" must {

        "show BAD_REQUEST" in new Fixture {

          when(mockRenewalService.getBusinessMatching(any())).thenReturn(Future.successful(Some(businessMatching)))

          val result = controller.post(true)(request)

          status(result) mustBe BAD_REQUEST
        }

        "return error message for multiple services" when {
          "there's more than one business activity" in new Fixture {
            override val businessMatching = BusinessMatching(
              activities = Some(Activities(Set(AccountancyServices, MoneyServiceBusiness)))
            )

            when(controller.renewalService.getFirstBusinessActivityInLowercase(any())(any(), any()))
              .thenReturn(Future.successful(None))

            when(controller.renewalService.getBusinessMatching(any()))
              .thenReturn(Future.successful(Some(businessMatching)))

            val newRequest = FakeRequest(POST, routes.AMLSTurnoverController.post().url).withFormUrlEncodedBody(
              "turnover" -> ""
            )

            val result = controller.post(true)(newRequest)

            status(result) mustBe BAD_REQUEST

            contentAsString(result) must include(
              messages("error.required.renewal.ba.turnover.from.mlr")
            )
          }
        }

        "return error message for single service" when {
          "there's one business activity" in new Fixture {

            val expected = AccountancyServices.getMessage()

            when(controller.renewalService.getFirstBusinessActivityInLowercase(any())(any(), any()))
              .thenReturn(Future.successful(Some(expected)))

            when(controller.renewalService.getBusinessMatching(any()))
              .thenReturn(Future.successful(Some(businessMatching)))

            val newRequest = FakeRequest(POST, routes.AMLSTurnoverController.post().url).withFormUrlEncodedBody(
              "turnover" -> ""
            )

            val result = controller.post(true)(newRequest)

            status(result) mustBe BAD_REQUEST

            contentAsString(result) must include(
              messages("error.required.renewal.ba.turnover.from.mlr.single.service", expected)
            )
          }
        }

        "return default error message" when {
          "no business activities are returned" in new Fixture {
            override val businessMatching = BusinessMatching(
              activities = Some(Activities(Set()))
            )

            when(controller.renewalService.getFirstBusinessActivityInLowercase(any())(any(), any()))
              .thenReturn(Future.successful(None))

            when(controller.renewalService.getBusinessMatching(any()))
              .thenReturn(Future.successful(Some(businessMatching)))

            val newRequest = FakeRequest(POST, routes.AMLSTurnoverController.post().url).withFormUrlEncodedBody(
              "turnover" -> ""
            )

            val result = controller.post(true)(newRequest)

            status(result) mustBe BAD_REQUEST

            contentAsString(result) must include(
              messages("error.required.renewal.ba.turnover.from.mlr")
            )
          }
        }
      }
    }
  }
}
