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

import config.AmlsErrorHandler
import connectors.DataCacheConnector
import controllers.actions.SuccessfulAuthAction
import forms.renewal.InvolvedInOtherFormProvider
import models.businessmatching.BusinessActivity._
import models.businessmatching.{BusinessActivities => BMActivities, _}
import models.renewal.Renewal
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc.{AnyContentAsEmpty, AnyContentAsFormUrlEncoded, Request, Result}
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Injecting}
import services.businessmatching.RecoverActivitiesService
import services.cache.Cache
import services.{RenewalService, StatusService}
import utils.AmlsSpec
import views.html.renewal.InvolvedInOtherView

import scala.concurrent.Future

class InvolvedInOtherControllerSpec extends AmlsSpec with MockitoSugar with ScalaFutures with Injecting {

  trait Fixture {
    self =>
    val request: Request[AnyContentAsEmpty.type] = addToken(authRequest)

    val mockCacheMap: Cache = mock[Cache]

    val emptyCache: Cache = Cache.empty

    lazy val mockDataCacheConnector: DataCacheConnector = mock[DataCacheConnector]
    lazy val mockStatusService: StatusService           = mock[StatusService]
    lazy val mockRenewalService: RenewalService         = mock[RenewalService]
    lazy val view: InvolvedInOtherView                  = inject[InvolvedInOtherView]
    val controller                                      = new InvolvedInOtherController(
      dataCacheConnector = mockDataCacheConnector,
      recoverActivitiesService = mock[RecoverActivitiesService],
      authAction = SuccessfulAuthAction,
      ds = commonDependencies,
      renewalService = mockRenewalService,
      cc = mockMcc,
      formProvider = inject[InvolvedInOtherFormProvider],
      view = view,
      error = inject[AmlsErrorHandler]
    )
  }

  "InvolvedInOtherController" must {
    "add a/an to sorted business type list" in new Fixture {
      val businessMatching: BusinessMatching = BusinessMatching(
        activities = Some(
          BMActivities(
            Set(
              TelephonePaymentService,
              HighValueDealing,
              MoneyServiceBusiness,
              TrustAndCompanyServices,
              AccountancyServices,
              BillPaymentServices,
              EstateAgentBusinessService
            )
          )
        )
      )

      when(mockDataCacheConnector.fetchAll(any()))
        .thenReturn(Future.successful(Some(mockCacheMap)))

      when(mockCacheMap.getEntry[Renewal](Renewal.key)).thenReturn(None)
      when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key)).thenReturn(Some(businessMatching))

      val result: Future[Result] = controller.get()(request)

      val expectedSpecialCase: String = messages("businessmatching.registerservices.servicename.lbl.08")

      status(result)          must be(OK)
      contentAsString(result) must include(
        s"an ${messages("businessmatching.registerservices.servicename.lbl.01").toLowerCase}"
      )
      contentAsString(result) must include(
        s"a ${messages("businessmatching.registerservices.servicename.lbl.03").toLowerCase}"
      )
      contentAsString(result) must include(
        s"an ${messages("businessmatching.registerservices.servicename.lbl.04").toLowerCase}"
      )
      contentAsString(result) must include(
        s"a ${messages("businessmatching.registerservices.servicename.lbl.05").toLowerCase}"
      )
      contentAsString(result) must include(
        s"a ${messages("businessmatching.registerservices.servicename.lbl.06").toLowerCase}"
      )
      contentAsString(result) must include(s"a ${expectedSpecialCase(0).toLower}")
      contentAsString(result) must include(
        s"a ${messages("businessmatching.registerservices.servicename.lbl.07").toLowerCase}"
      )
    }
  }

  "InvolvedInOtherController" must {

    "when get is called" must {
      "display the other business activities page" in new Fixture {

        when(mockCacheMap.getEntry[Renewal](Renewal.key)).thenReturn(None)
        when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key)).thenReturn(None)
        when(mockDataCacheConnector.fetchAll(any())).thenReturn(Future.successful(Some(mockCacheMap)))

        val result: Future[Result] = controller.get()(request)
        status(result)          must be(OK)
        contentAsString(result) must include(messages("renewal.involvedinother.title"))
      }

      "redirect to itself after performing a successful recovery of missing business types" in new Fixture {
        val businessMatching: BusinessMatching = BusinessMatching(activities = Some(BMActivities(Set())))
        when(mockCacheMap.getEntry[Renewal](Renewal.key)).thenReturn(None)
        when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key)).thenReturn(Some(businessMatching))
        when(mockDataCacheConnector.fetchAll(any())).thenReturn(Future.successful(Some(mockCacheMap)))
        when(controller.recoverActivitiesService.recover(any())(any(), any(), any()))
          .thenReturn(Future.successful(true))

        val result: Future[Result] = controller.get()(request)
        status(result) must be(SEE_OTHER)
        redirectLocation(result) mustBe Some(routes.InvolvedInOtherController.get().url)
      }

      "return an internal server error after failing to recover missing business types" in new Fixture {
        val businessMatching: BusinessMatching = BusinessMatching(activities = Some(BMActivities(Set())))
        when(mockCacheMap.getEntry[Renewal](Renewal.key)).thenReturn(None)
        when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key)).thenReturn(Some(businessMatching))
        when(mockDataCacheConnector.fetchAll(any())).thenReturn(Future.successful(Some(mockCacheMap)))
        when(controller.recoverActivitiesService.recover(any())(any(), any(), any()))
          .thenReturn(Future.successful(false))

        val result: Future[Result] = controller.get()(request)
        status(result) must be(INTERNAL_SERVER_ERROR)
      }
    }

    "when post is called" must {
      "when there is pre-existing Renewal Data" must {
        "redirect to InvolvedInOtherDetailsController with option yes" in new Fixture {

          val newRequest: FakeRequest[AnyContentAsFormUrlEncoded] =
            FakeRequest(POST, routes.InvolvedInOtherController.post().url)
              .withFormUrlEncodedBody("involvedInOther" -> "true")

          when(mockRenewalService.createOrUpdateRenewal(any(), any(), any())).thenReturn(Future.successful(emptyCache))
          when(mockRenewalService.getRenewal(any())).thenReturn(Future.successful(Some(Renewal())))

          val result: Future[Result] = controller.post()(newRequest)
          status(result)           must be(SEE_OTHER)
          redirectLocation(result) must be(Some(routes.InvolvedInOtherDetailsController.get().url))
        }

        "redirect to AMLSTurnoverController with valid data with option no" in new Fixture {

          val newRequest: FakeRequest[AnyContentAsFormUrlEncoded] =
            FakeRequest(POST, routes.InvolvedInOtherController.post().url)
              .withFormUrlEncodedBody("involvedInOther" -> "false")

          when(mockRenewalService.createOrUpdateRenewal(any(), any(), any())).thenReturn(Future.successful(emptyCache))
          when(mockRenewalService.getRenewal(any())).thenReturn(Future.successful(Some(Renewal())))

          val result: Future[Result] = controller.post()(newRequest)
          status(result)           must be(SEE_OTHER)
          redirectLocation(result) must be(Some(routes.AMLSTurnoverController.get().url))
        }
      }

      "when there is no Renewal data at all yet" must {
        "redirect to InvolvedInOtherDetailsController with option yes" in new Fixture {

          val newRequest: FakeRequest[AnyContentAsFormUrlEncoded] =
            FakeRequest(POST, routes.InvolvedInOtherController.post().url)
              .withFormUrlEncodedBody("involvedInOther" -> "true")

          when(mockRenewalService.createOrUpdateRenewal(any(), any(), any())).thenReturn(Future.successful(emptyCache))
          when(mockRenewalService.getRenewal(any())).thenReturn(Future.successful(Some(Renewal())))

          val result: Future[Result] = controller.post()(newRequest)
          status(result)           must be(SEE_OTHER)
          redirectLocation(result) must be(Some(routes.InvolvedInOtherDetailsController.get().url))
        }

        "redirect to AMLSTurnoverController with valid data with option no" in new Fixture {

          val newRequest: FakeRequest[AnyContentAsFormUrlEncoded] =
            FakeRequest(POST, routes.InvolvedInOtherController.post().url)
              .withFormUrlEncodedBody("involvedInOther" -> "false")

          when(mockRenewalService.createOrUpdateRenewal(any(), any(), any())).thenReturn(Future.successful(emptyCache))
          when(mockRenewalService.getRenewal(any())).thenReturn(Future.successful(Some(Renewal())))

          val result: Future[Result] = controller.post()(newRequest)
          status(result)           must be(SEE_OTHER)
          redirectLocation(result) must be(Some(routes.AMLSTurnoverController.get().url))
        }

        "redirect to SummaryController with valid data with option no in edit mode" in new Fixture {

          val newRequest: FakeRequest[AnyContentAsFormUrlEncoded] =
            FakeRequest(POST, routes.InvolvedInOtherController.post().url)
              .withFormUrlEncodedBody("involvedInOther" -> "false")

          when(mockRenewalService.createOrUpdateRenewal(any(), any(), any())).thenReturn(Future.successful(emptyCache))
          when(mockRenewalService.getRenewal(any())).thenReturn(Future.successful(Some(Renewal())))

          val result: Future[Result] = controller.post(true)(newRequest)
          status(result)           must be(SEE_OTHER)
          redirectLocation(result) must be(Some(routes.SummaryController.get.url))
        }

        "redirect to InvolvedInOtherDetailsController with valid data in edit mode" in new Fixture {

          val newRequest: FakeRequest[AnyContentAsFormUrlEncoded] =
            FakeRequest(POST, routes.InvolvedInOtherController.post().url)
              .withFormUrlEncodedBody("involvedInOther" -> "true")

          when(mockRenewalService.createOrUpdateRenewal(any(), any(), any())).thenReturn(Future.successful(emptyCache))
          when(mockRenewalService.getRenewal(any())).thenReturn(Future.successful(Some(Renewal())))

          val result: Future[Result] = controller.post(true)(newRequest)
          status(result)           must be(SEE_OTHER)
          redirectLocation(result) must be(Some(routes.InvolvedInOtherDetailsController.get(true).url))
        }
      }

      "respond with BAD_REQUEST" when {

        "with invalid data with business activities" in new Fixture {

          val businessMatching: BusinessMatching =
            BusinessMatching(activities = Some(BMActivities(Set(AccountancyServices))))

          when(mockRenewalService.getBusinessMatching(any())).thenReturn(Future.successful(Some(businessMatching)))

          val newRequest: FakeRequest[AnyContentAsFormUrlEncoded] =
            FakeRequest(POST, routes.InvolvedInOtherController.post().url).withFormUrlEncodedBody(
              "involvedInOther" -> "test"
            )

          val result: Future[Result] = controller.post()(newRequest)
          status(result)          must be(BAD_REQUEST)
          contentAsString(result) must include(
            messages("businessmatching.registerservices.servicename.lbl.01").toLowerCase
          )

          val document: Document = Jsoup.parse(contentAsString(result))
          document.select("a[href=#involvedInOther-true]").html() must include(
            messages("error.required.renewal.ba.involved.in.other")
          )
        }

        "with invalid data without business activities" in new Fixture {

          when(mockRenewalService.getBusinessMatching(any())).thenReturn(Future.successful(None))

          val newRequest: FakeRequest[AnyContentAsFormUrlEncoded] =
            FakeRequest(POST, routes.InvolvedInOtherController.post().url)
              .withFormUrlEncodedBody("involvedInOther" -> "test")

          val result: Future[Result] = controller.post()(newRequest)
          status(result) must be(BAD_REQUEST)

          val document: Document = Jsoup.parse(contentAsString(result))
          document.select("a[href=#involvedInOther-true]").html() must include(
            messages("error.required.renewal.ba.involved.in.other")
          )
        }
      }
    }
  }
}
