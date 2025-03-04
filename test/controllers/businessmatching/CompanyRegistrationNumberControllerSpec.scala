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

package controllers.businessmatching

import cats.data.OptionT
import cats.implicits._
import controllers.actions.SuccessfulAuthAction
import forms.businessmatching.CompanyRegistrationNumberFormProvider
import models.businessmatching.{BusinessMatching, CompanyRegistrationNumber}
import models.status.NotCompleted
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import play.api.i18n.Messages
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.businessmatching.BusinessMatchingService
import services.cache.Cache
import utils.{AmlsSpec, CacheMocks, StatusMocks}
import views.html.businessmatching.CompanyRegistrationNumberView

import scala.concurrent.{ExecutionContext, Future}

class CompanyRegistrationNumberControllerSpec
    extends AmlsSpec
    with MockitoSugar
    with ScalaFutures
    with StatusMocks
    with CacheMocks {

  trait Fixture {
    self =>
    val request                       = addToken(authRequest)
    implicit val ec: ExecutionContext = app.injector.instanceOf[ExecutionContext]

    lazy val view  = app.injector.instanceOf[CompanyRegistrationNumberView]
    val controller = new CompanyRegistrationNumberController(
      authAction = SuccessfulAuthAction,
      ds = commonDependencies,
      mockCacheConnector,
      statusService = mockStatusService,
      mock[BusinessMatchingService],
      cc = mockMcc,
      app.injector.instanceOf[CompanyRegistrationNumberFormProvider],
      view
    )

    val businessMatching = BusinessMatching(companyRegistrationNumber = Some(CompanyRegistrationNumber("12345678")))

    when {
      mockStatusService.isPreSubmission(any())
    } thenReturn true

    mockApplicationStatus(NotCompleted)

    when {
      controller.businessMatchingService.getModel(any())
    } thenReturn OptionT.liftF[Future, BusinessMatching](Future.successful(businessMatching))

    val emptyCache = Cache.empty
  }

  "CompanyRegistrationNumberController" must {

    "on get() display company registration number page" in new Fixture {
      when {
        controller.businessMatchingService.getModel(any())
      } thenReturn OptionT.liftF[Future, BusinessMatching](Future.successful(None))

      val result = controller.get()(request)
      status(result) must be(OK)
      val document  = Jsoup.parse(contentAsString(result))
      val pageTitle = Messages("businessmatching.registrationnumber.title") + " - " +
        Messages("summary.businessmatching") + " - " +
        Messages("title.amls") + " - " + Messages("title.gov")
      document.title() mustBe pageTitle
    }

    "on get() display existing data if it exists" in new Fixture {

      val result = controller.get()(request)
      status(result) must be(OK)

      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("value").`val` must be("12345678")
    }

    "on post() give a bad request if invalid data sent" in new Fixture {

      val invalidRequest = FakeRequest(POST, routes.CompanyRegistrationNumberController.post(true).url)
        .withFormUrlEncodedBody(("value", "1234567#"))

      val result    = controller.post()(invalidRequest)
      val document  = Jsoup.parse(contentAsString(result))
      val pageTitle = Messages("businessmatching.registrationnumber.title") + " - " +
        Messages("summary.businessmatching") + " - " +
        Messages("title.amls") + " - " + Messages("title.gov")

      document.title() mustBe s"Error: $pageTitle"
    }

    "on post() redirect correctly if valid data sent and edit is true" in new Fixture {

      val validRequest = FakeRequest(POST, routes.CompanyRegistrationNumberController.post(true).url)
        .withFormUrlEncodedBody(("value", "12345678"))

      when(controller.dataCacheConnector.fetch[BusinessMatching](any(), any())(any()))
        .thenReturn(Future.successful(Some(businessMatching)))

      when(controller.dataCacheConnector.save[BusinessMatching](any(), any(), any())(any()))
        .thenReturn(Future.successful(emptyCache))

      val result = controller.post(true)(validRequest)
      status(result)           must be(SEE_OTHER)
      redirectLocation(result) must be(Some(routes.SummaryController.get().url))
    }

    "on post() redirect correctly if valid data sent and edit is false" in new Fixture {

      val validRequest = FakeRequest(POST, routes.CompanyRegistrationNumberController.post(false).url)
        .withFormUrlEncodedBody(("value", "12345678"))

      when(controller.dataCacheConnector.fetch[BusinessMatching](any(), any())(any()))
        .thenReturn(Future.successful(Some(businessMatching)))

      when(controller.dataCacheConnector.save[BusinessMatching](any(), any(), any())(any()))
        .thenReturn(Future.successful(emptyCache))

      val result = controller.post(false)(validRequest)
      status(result)           must be(SEE_OTHER)
      redirectLocation(result) must be(Some(routes.RegisterServicesController.get().url))
    }

  }

}
