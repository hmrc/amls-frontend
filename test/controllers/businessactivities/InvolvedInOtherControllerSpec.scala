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

package controllers.businessactivities

import config.AmlsErrorHandler
import connectors.DataCacheConnector
import controllers.actions.SuccessfulAuthAction
import forms.businessactivities.InvolvedInOtherFormProvider
import models.businessactivities.{BusinessActivities, InvolvedInOtherYes}
import models.businessmatching.BusinessActivity._
import models.businessmatching.{BusinessActivities => BMActivities, BusinessMatching}
import models.status.NotCompleted
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import play.api.mvc.{AnyContentAsEmpty, AnyContentAsFormUrlEncoded, Request, Result}
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Injecting}
import services.StatusService
import services.businessmatching.RecoverActivitiesService
import services.cache.Cache
import utils.AmlsSpec
import views.html.businessactivities.InvolvedInOtherNameView

import scala.concurrent.{ExecutionContext, Future}

class InvolvedInOtherControllerSpec extends AmlsSpec with ScalaFutures with Injecting {

  trait Fixture {
    self =>
    val request: Request[AnyContentAsEmpty.type] = addToken(authRequest)
    implicit val ec: ExecutionContext            = app.injector.instanceOf[ExecutionContext]
    lazy val view: InvolvedInOtherNameView       = inject[InvolvedInOtherNameView]
    val controller                               = new InvolvedInOtherController(
      dataCacheConnector = mock[DataCacheConnector],
      recoverActivitiesService = mock[RecoverActivitiesService],
      authAction = SuccessfulAuthAction,
      ds = commonDependencies,
      statusService = mock[StatusService],
      cc = mockMcc,
      formProvider = inject[InvolvedInOtherFormProvider],
      view = view,
      error = inject[AmlsErrorHandler]
    )
  }

  val emptyCache: Cache = Cache.empty

  val mockCacheMap: Cache = mock[Cache]

  "InvolvedInOtherController" when {

    "get is called" must {
      "display the is your involved in other page with an empty form" in new Fixture {

        val businessMatching: BusinessMatching = BusinessMatching(
          activities = Some(
            BMActivities(
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

        when(controller.statusService.getStatus(any(), any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(NotCompleted))

        when(mockCacheMap.getEntry[BusinessActivities](BusinessActivities.key))
          .thenReturn(None)

        when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
          .thenReturn(Some(businessMatching))

        when(controller.dataCacheConnector.fetchAll(any()))
          .thenReturn(Future.successful(Some(mockCacheMap)))

        val result: Future[Result] = controller.get()(request)
        status(result) must be(OK)

        val html: String = contentAsString(result)

        html must include("an " + messages("businessactivities.registerservices.servicename.lbl.01"))
        html must include("a " + messages("businessactivities.registerservices.servicename.lbl.03"))
        html must include("an " + messages("businessactivities.registerservices.servicename.lbl.04"))
        html must include("a " + messages("businessactivities.registerservices.servicename.lbl.05"))
        html must include("a " + messages("businessactivities.registerservices.servicename.lbl.06"))
        html must include("a " + messages("businessactivities.registerservices.servicename.lbl.07"))
        html must include("a " + messages("businessactivities.registerservices.servicename.lbl.08"))

        val page: Document = Jsoup.parse(html)

        page.select("input[type=radio][name=involvedInOther][value=true]").hasAttr("checked")  must be(false)
        page.select("input[type=radio][name=involvedInOther][value=false]").hasAttr("checked") must be(false)
        page.select("textarea[name=details]").`val`                                            must be("")
      }

      "display the is your involved in other page when there is no cache data" in new Fixture {

        when(controller.statusService.getStatus(any(), any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(NotCompleted))

        when(controller.dataCacheConnector.fetchAll(any()))
          .thenReturn(Future.successful(None))

        val result: Future[Result] = controller.get()(request)
        status(result) must be(OK)

      }

      "display the involved in other page with pre populated data" in new Fixture {

        when(mockCacheMap.getEntry[BusinessActivities](BusinessActivities.key))
          .thenReturn(Some(BusinessActivities(involvedInOther = Some(InvolvedInOtherYes("test")))))

        when(controller.statusService.getStatus(any(), any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(NotCompleted))

        when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
          .thenReturn(Some(BusinessMatching()))

        when(controller.dataCacheConnector.fetchAll(any()))
          .thenReturn(Future.successful(Some(mockCacheMap)))

        val result: Future[Result] = controller.get()(request)
        status(result) must be(OK)

        val page: Document = Jsoup.parse(contentAsString(result))

        page.select("input[type=radio][name=involvedInOther][value=true]").hasAttr("checked")  must be(true)
        page.select("input[type=radio][name=involvedInOther][value=false]").hasAttr("checked") must be(false)
        page.select("textarea[name=details]").`val`                                            must be("test")
      }

      "redirect to itself after performing a successful recovery of missing business types" in new Fixture {
        val businessMatching: BusinessMatching = BusinessMatching(activities = Some(BMActivities(Set())))

        when(controller.dataCacheConnector.fetchAll(any()))
          .thenReturn(Future.successful(Some(mockCacheMap)))

        when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
          .thenReturn(Some(businessMatching))

        when(mockCacheMap.getEntry[BusinessActivities](BusinessActivities.key))
          .thenReturn(None)

        when(controller.recoverActivitiesService.recover(any())(any(), any(), any()))
          .thenReturn(Future.successful(true))

        val result: Future[Result] = controller.get()(request)
        status(result) must be(SEE_OTHER)
        redirectLocation(result) mustBe Some(routes.InvolvedInOtherController.get().url)
      }

      "return an internal server error after failing to recover missing business types" in new Fixture {
        val businessMatching: BusinessMatching = BusinessMatching(activities = Some(BMActivities(Set())))

        when(controller.dataCacheConnector.fetchAll(any()))
          .thenReturn(Future.successful(Some(mockCacheMap)))

        when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
          .thenReturn(Some(businessMatching))

        when(mockCacheMap.getEntry[BusinessActivities](BusinessActivities.key))
          .thenReturn(None)

        when(controller.recoverActivitiesService.recover(any())(any(), any(), any()))
          .thenReturn(Future.successful(false))

        val result: Future[Result] = controller.get()(request)
        status(result) must be(INTERNAL_SERVER_ERROR)
      }
    }

    "post is called" must {
      "respond with SEE_OTHER" when {
        "edit is false" when {
          "involvedInOther is true and there is no existing BusinessActivities data" in new Fixture {

            val newRequest: FakeRequest[AnyContentAsFormUrlEncoded] =
              FakeRequest(POST, routes.InvolvedInOtherController.post().url).withFormUrlEncodedBody(
                "involvedInOther" -> "true",
                "details"         -> "test"
              )

            when(controller.dataCacheConnector.fetch[BusinessActivities](any(), any())(any()))
              .thenReturn(Future.successful(None))

            when(controller.dataCacheConnector.save[BusinessActivities](any(), any(), any())(any()))
              .thenReturn(Future.successful(emptyCache))

            val result: Future[Result] = controller.post()(newRequest)
            status(result)           must be(SEE_OTHER)
            redirectLocation(result) must be(Some(routes.ExpectedBusinessTurnoverController.get().url))
          }
          "involvedInOther is true and there is existing BusinessActivities data" in new Fixture {

            val newRequest: FakeRequest[AnyContentAsFormUrlEncoded] =
              FakeRequest(POST, routes.InvolvedInOtherController.post().url).withFormUrlEncodedBody(
                "involvedInOther" -> "true",
                "details"         -> "test"
              )

            when(controller.dataCacheConnector.fetch[BusinessActivities](any(), any())(any()))
              .thenReturn(Future.successful(Some(BusinessActivities())))

            when(controller.dataCacheConnector.save[BusinessActivities](any(), any(), any())(any()))
              .thenReturn(Future.successful(emptyCache))

            val result: Future[Result] = controller.post()(newRequest)
            status(result)           must be(SEE_OTHER)
            redirectLocation(result) must be(Some(routes.ExpectedBusinessTurnoverController.get().url))
          }

          "involvedInOther is false and there is no existing BusinessActivities data" in new Fixture {

            val newRequest: FakeRequest[AnyContentAsFormUrlEncoded] =
              FakeRequest(POST, routes.InvolvedInOtherController.post().url).withFormUrlEncodedBody(
                "involvedInOther" -> "false"
              )

            when(controller.dataCacheConnector.fetch[BusinessActivities](any(), any())(any()))
              .thenReturn(Future.successful(None))

            when(controller.dataCacheConnector.save[BusinessActivities](any(), any(), any())(any()))
              .thenReturn(Future.successful(emptyCache))

            val result: Future[Result] = controller.post()(newRequest)
            status(result)           must be(SEE_OTHER)
            redirectLocation(result) must be(Some(routes.ExpectedAMLSTurnoverController.get().url))
          }

          "involvedInOther is false and there is existing BusinessActivities data" in new Fixture {

            val newRequest: FakeRequest[AnyContentAsFormUrlEncoded] =
              FakeRequest(POST, routes.InvolvedInOtherController.post().url).withFormUrlEncodedBody(
                "involvedInOther" -> "false"
              )

            when(controller.dataCacheConnector.fetch[BusinessActivities](any(), any())(any()))
              .thenReturn(Future.successful(Some(BusinessActivities())))

            when(controller.dataCacheConnector.save[BusinessActivities](any(), any(), any())(any()))
              .thenReturn(Future.successful(emptyCache))

            val result: Future[Result] = controller.post()(newRequest)
            status(result)           must be(SEE_OTHER)
            redirectLocation(result) must be(Some(routes.ExpectedAMLSTurnoverController.get().url))
          }
        }
        "edit is true" when {
          "involvedInOther is true" in new Fixture {

            val newRequest: FakeRequest[AnyContentAsFormUrlEncoded] =
              FakeRequest(POST, routes.InvolvedInOtherController.post(true).url).withFormUrlEncodedBody(
                "involvedInOther" -> "true",
                "details"         -> "test"
              )

            when(controller.dataCacheConnector.fetch[BusinessActivities](any(), any())(any()))
              .thenReturn(Future.successful(None))

            when(controller.dataCacheConnector.save[BusinessActivities](any(), any(), any())(any()))
              .thenReturn(Future.successful(emptyCache))

            val result: Future[Result] = controller.post(true)(newRequest)
            status(result)           must be(SEE_OTHER)
            redirectLocation(result) must be(Some(routes.ExpectedBusinessTurnoverController.get(true).url))
          }

          "involvedInOther is false" in new Fixture {

            val newRequest: FakeRequest[AnyContentAsFormUrlEncoded] =
              FakeRequest(POST, routes.InvolvedInOtherController.post(true).url).withFormUrlEncodedBody(
                "involvedInOther" -> "false"
              )

            when(controller.dataCacheConnector.fetch[BusinessActivities](any(), any())(any()))
              .thenReturn(Future.successful(None))

            when(controller.dataCacheConnector.save[BusinessActivities](any(), any(), any())(any()))
              .thenReturn(Future.successful(emptyCache))

            val result: Future[Result] = controller.post(true)(newRequest)
            status(result)           must be(SEE_OTHER)
            redirectLocation(result) must be(Some(routes.SummaryController.get.url))
          }
        }
      }

      "respond with BAD_REQUEST" when {
        "given invalid data" in new Fixture {

          when(controller.dataCacheConnector.fetch[BusinessMatching](any(), any())(any()))
            .thenReturn(Future.successful(None))

          val newRequest: FakeRequest[AnyContentAsFormUrlEncoded] =
            FakeRequest(POST, routes.InvolvedInOtherController.post().url).withFormUrlEncodedBody(
              "involvedInOther" -> "test"
            )

          val result: Future[Result] = controller.post()(newRequest)
          status(result) must be(BAD_REQUEST)
        }

        "on post with required field not filled with business activities" in new Fixture {

          val businessMatching: BusinessMatching = BusinessMatching(
            activities = Some(BMActivities(Set(AccountancyServices)))
          )

          when(controller.dataCacheConnector.fetch[BusinessMatching](any(), any())(any()))
            .thenReturn(Future.successful(Some(businessMatching)))

          val newRequest: FakeRequest[AnyContentAsFormUrlEncoded] =
            FakeRequest(POST, routes.InvolvedInOtherController.post().url).withFormUrlEncodedBody(
              "involvedInOther" -> "true",
              "details"         -> ""
            )

          val result: Future[Result] = controller.post()(newRequest)
          status(result) must be(BAD_REQUEST)
        }
      }
    }
  }
}
