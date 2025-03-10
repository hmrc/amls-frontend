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

package controllers.msb

import controllers.actions.SuccessfulAuthAction
import models.businessmatching.updateservice.ServiceChangeRegister
import models.businessmatching.BusinessMatching
import models.businessmatching.BusinessActivity.{HighValueDealing, MoneyServiceBusiness}
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.Helpers._
import play.api.test.Injecting
import utils.{AmlsSpec, DependencyMocks}
import views.html.msb.WhatYouNeedView

import scala.concurrent.{ExecutionContext, Future}

class WhatYouNeedControllerSpec extends AmlsSpec with MockitoSugar with ScalaFutures with Injecting {

  trait Fixture extends DependencyMocks {
    self =>
    val request                       = addToken(authRequest)
    lazy val view                     = inject[WhatYouNeedView]
    implicit val ec: ExecutionContext = inject[ExecutionContext]
    val controller                    = new WhatYouNeedController(
      SuccessfulAuthAction,
      ds = commonDependencies,
      mockStatusService,
      mockCacheConnector,
      cc = mockMcc,
      view = view
    )

    mockCacheFetch[ServiceChangeRegister](None)
    mockCacheFetchAll
    mockCacheGetEntry[BusinessMatching](Some(BusinessMatching()), BusinessMatching.key)
  }

  "WhatYouNeedController" must {

    "get" must {

      "load the page" in new Fixture {
        val result = controller.get()(request)
        status(result) must be(OK)

        val pageTitle = messages("title.wyn") + " - " +
          messages("summary.msb") + " - " +
          messages("title.amls") + " - " + messages("title.gov")

        contentAsString(result) must include(pageTitle)
      }
    }

    "post" must {

      "redirect to the expected throughput controller if in pre-submission status" in new Fixture {
        when {
          mockStatusService.isPreSubmission(any(), any(), any())(any(), any(), any())
        } thenReturn Future.successful(true)

        val result = controller.post(request)
        redirectLocation(result) mustBe Some(controllers.msb.routes.ExpectedThroughputController.get().url)
      }

      "redirect to the expected throughput page if MSB has just been added to the application" in new Fixture {
        when {
          mockStatusService.isPreSubmission(any(), any(), any())(any(), any(), any())
        } thenReturn Future.successful(false)

        mockCacheFetch(
          Some(ServiceChangeRegister(Some(Set(MoneyServiceBusiness, HighValueDealing)))),
          Some(ServiceChangeRegister.key)
        )

        val result = controller.post(request)
        redirectLocation(result) mustBe Some(controllers.msb.routes.ExpectedThroughputController.get().url)
      }

      "redirect to the expected throughput page if not in pre-submission status" in new Fixture {

        when {
          mockStatusService.isPreSubmission(any(), any(), any())(any(), any(), any())
        } thenReturn Future.successful(false)

        val result = controller.post(request)
        redirectLocation(result) mustBe Some(controllers.msb.routes.ExpectedThroughputController.get().url)
      }
    }
  }
}
