/*
 * Copyright 2019 HM Revenue & Customs
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

import models.businessmatching.updateservice.ServiceChangeRegister
import models.businessmatching.{BusinessMatching, HighValueDealing, MoneyServiceBusiness}
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import play.api.i18n.Messages
import play.api.test.Helpers._
import utils.{AmlsSpec, AuthorisedFixture, DependencyMocks}

import scala.concurrent.Future

class WhatYouNeedControllerSpec extends AmlsSpec with MockitoSugar with ScalaFutures {

  trait Fixture extends AuthorisedFixture with DependencyMocks {
    self =>
    val request = addToken(authRequest)

    val controller = new WhatYouNeedController(
      self.authConnector,
      mockStatusService,
      mockCacheConnector)

    mockCacheFetch[ServiceChangeRegister](None)
    mockCacheFetchAll
    mockCacheGetEntry[BusinessMatching](Some(BusinessMatching()), BusinessMatching.key)
  }

  "WhatYouNeedController" must {

    "get" must {

      "load the page" in new Fixture {
        val result = controller.get(request)
        status(result) must be(OK)

        val pageTitle = Messages("title.wyn") + " - " +
          Messages("summary.msb") + " - " +
          Messages("title.amls") + " - " + Messages("title.gov")

        contentAsString(result) must include(pageTitle)
      }
    }

    "post" must {

      "redirect to the expected throughput controller if in pre-submission status" in new Fixture {
        when {
          mockStatusService.isPreSubmission(any(), any(), any())
        } thenReturn Future.successful(true)

        val result = controller.post(request)
        redirectLocation(result) mustBe Some(controllers.msb.routes.ExpectedThroughputController.get().url)
      }

      "redirect to the expected throughput page if MSB has just been added to the application" in new Fixture {
        when {
          mockStatusService.isPreSubmission(any(), any(), any())
        } thenReturn Future.successful(false)

        mockCacheFetch(
          Some(ServiceChangeRegister(Some(Set(MoneyServiceBusiness, HighValueDealing)))),
          Some(ServiceChangeRegister.key))

        val result = controller.post(request)
        redirectLocation(result) mustBe Some(controllers.msb.routes.ExpectedThroughputController.get().url)
      }

      "redirect to the branches or agents controller if not in pre-submission status" in new Fixture {

        when {
          mockStatusService.isPreSubmission(any(), any(), any())
        } thenReturn Future.successful(false)


        val result = controller.post(request)
        redirectLocation(result) mustBe Some(controllers.msb.routes.BranchesOrAgentsController.get().url)
      }
    }
  }
}
