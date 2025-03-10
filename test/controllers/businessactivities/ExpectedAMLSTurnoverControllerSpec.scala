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

import controllers.actions.SuccessfulAuthAction
import forms.businessactivities.ExpectedAMLSTurnoverFormProvider
import models.businessactivities.ExpectedAMLSTurnover.First
import models.businessactivities._
import models.businessmatching.BusinessActivity._
import models.businessmatching.{BusinessActivities => Activities, BusinessMatching}
import models.status.NotCompleted
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.ArgumentMatchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc.{AnyContentAsEmpty, AnyContentAsFormUrlEncoded, Request, Result}
import play.api.test
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Injecting}
import services.StatusService
import services.businessactivities.ExpectedAMLSTurnoverService
import services.cache.Cache
import utils.AmlsSpec
import views.html.businessactivities.ExpectedAMLSTurnoverView

import scala.concurrent.{ExecutionContext, Future}

class ExpectedAMLSTurnoverControllerSpec
    extends AmlsSpec
    with MockitoSugar
    with ScalaFutures
    with Injecting
    with BeforeAndAfterEach {

  val mockService: ExpectedAMLSTurnoverService = mock[ExpectedAMLSTurnoverService]

  trait Fixture {
    self =>
    val request: Request[AnyContentAsEmpty.type] = addToken(authRequest)
    implicit val ec: ExecutionContext            = inject[ExecutionContext]

    lazy val view: ExpectedAMLSTurnoverView = inject[ExpectedAMLSTurnoverView]
    val controller                          = new ExpectedAMLSTurnoverController(
      SuccessfulAuthAction,
      ds = commonDependencies,
      statusService = mock[StatusService],
      cc = mockMcc,
      service = mockService,
      formProvider = inject[ExpectedAMLSTurnoverFormProvider],
      view = view
    )

    val mockCache: Cache = mock[Cache]

    def model: Option[BusinessActivities] = None
  }

  val emptyCache: Cache = Cache.empty

  override def beforeEach(): Unit = reset(mockService)

  "ExpectedAMLSTurnoverController" when {

    "get is called" must {
      "respond with OK" when {
        "there is no existing data, and show the services on the page" in new Fixture {

          val businessMatching: BusinessMatching = BusinessMatching(
            activities = Some(
              Activities(
                Set(
                  AccountancyServices,
                  ArtMarketParticipant,
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

          when(mockService.getBusinessMatchingExpectedTurnover(any()))
            .thenReturn(Future.successful(Some((businessMatching, None))))

          val result: Future[Result] = controller.get()(request)
          status(result) must be(OK)

          val html: String       = contentAsString(result)
          val document: Document = Jsoup.parse(html)

          ExpectedAMLSTurnover.all.foreach { value =>
            document.select(s"input[value=${value.toString}]").hasAttr("checked") must be(false)
            html                                                                  must include(messages(s"businessactivities.registerservices.servicename.lbl.${value.value}"))
          }

          html must include(messages("businessactivities.registerservices.servicename.lbl.08"))
        }

        "there is existing data" in new Fixture {

          override def model: Option[BusinessActivities] = Some(BusinessActivities(expectedAMLSTurnover = Some(First)))

          val businessMatching: BusinessMatching = BusinessMatching(
            activities = Some(
              Activities(
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

          when(mockService.getBusinessMatchingExpectedTurnover(any()))
            .thenReturn(Future.successful(Some((businessMatching, Some(First)))))

          val result: Future[Result] = controller.get()(request)
          status(result) must be(OK)

          val document: Document = Jsoup.parse(contentAsString(result))
          document.select("input[value=zeroPlus]").hasAttr("checked") must be(true)
        }

        "there is no cache data" in new Fixture {

          override def model: Option[BusinessActivities] = Some(BusinessActivities(expectedAMLSTurnover = Some(First)))

          when(controller.statusService.getStatus(any(), any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(NotCompleted))

          when(mockService.getBusinessMatchingExpectedTurnover(any()))
            .thenReturn(Future.successful(None))

          val result: Future[Result] = controller.get()(request)
          status(result) must be(OK)
        }
      }
    }

    "post is called" must {
      "on post with valid data" in new Fixture {

        val newRequest: FakeRequest[AnyContentAsFormUrlEncoded] =
          FakeRequest(POST, routes.ExpectedAMLSTurnoverController.post().url).withFormUrlEncodedBody(
            "expectedAMLSTurnover" -> "zeroPlus"
          )

        when(mockService.getBusinessMatching(any()))
          .thenReturn(Future.successful(Some(BusinessMatching())))

        when(mockService.updateBusinessActivities(any(), eqTo(First)))
          .thenReturn(Future.successful(Some(mockCache)))

        val result: Future[Result] = controller.post()(newRequest)
        status(result)           must be(SEE_OTHER)
        redirectLocation(result) must be(
          Some(controllers.businessactivities.routes.BusinessFranchiseController.get().url)
        )

        verify(mockService).updateBusinessActivities(any(), eqTo(First))
      }

      "on post with valid data in edit mode" in new Fixture {

        val newRequest: FakeRequest[AnyContentAsFormUrlEncoded] = test
          .FakeRequest(POST, routes.ExpectedAMLSTurnoverController.post().url)
          .withFormUrlEncodedBody(
            "expectedAMLSTurnover" -> "zeroPlus"
          )

        when(mockService.getBusinessMatching(any()))
          .thenReturn(Future.successful(Some(BusinessMatching())))

        when(mockService.updateBusinessActivities(any(), eqTo(First)))
          .thenReturn(Future.successful(Some(mockCache)))

        val result: Future[Result] = controller.post(true)(newRequest)
        status(result)           must be(SEE_OTHER)
        redirectLocation(result) must be(Some(controllers.businessactivities.routes.SummaryController.get.url))

        verify(mockService).updateBusinessActivities(any(), eqTo(First))
      }

      "on post with invalid data" in new Fixture {

        when(mockService.getBusinessMatching(any()))
          .thenReturn(Future.successful(Some(BusinessMatching())))

        val result: Future[Result] = controller.post(true)(request)

        status(result) mustBe BAD_REQUEST

        verify(mockService, times(0)).updateBusinessActivities(any(), any())
      }
    }
  }
}
