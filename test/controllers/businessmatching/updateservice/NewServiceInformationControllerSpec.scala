/*
 * Copyright 2017 HM Revenue & Customs
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
import cats.implicits._
import models.businessmatching._
import org.mockito.Matchers.{any, eq => eqTo}
import org.mockito.Mockito.{when, verify}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.scalatest.time.{Millis, Seconds, Span}
import play.api.test.Helpers._
import services.businessmatching.{BusinessMatchingService, ServiceFlow, NextService}
import utils.{AuthorisedFixture, DependencyMocks, FutureAssertions, GenericTestHelper}
import models.businessmatching.updateservice.UpdateService
import uk.gov.hmrc.http.cache.client.CacheMap
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class NewServiceInformationControllerSpec extends GenericTestHelper with MockitoSugar with FutureAssertions with ScalaFutures {

  trait Fixture extends AuthorisedFixture with DependencyMocks { self =>
    val request = addToken(authRequest)

    val bmService = mock[BusinessMatchingService]

    val controller = new NewServiceInformationController(self.authConnector, mockCacheConnector, bmService, mockServiceFlow, messagesApi)
  }

  "GET" when {
    "called" must {
      "return OK with the service name" in new Fixture {
        when {
          bmService.getAdditionalBusinessActivities(any(), any(), any())
        } thenReturn OptionT.some[Future, Set[BusinessActivity]](Set(MoneyServiceBusiness))

        when {
          mockServiceFlow.next(any(), any(), any())
        } thenReturn OptionT.some[Future, NextService](NextService("/service", AccountancyServices))

        val result = controller.get()(request)

        status(result) mustBe OK

        contentAsString(result) must include(AccountancyServices.getMessage)
        contentAsString(result) must include("/service")
      }

      "redirect to the 'update more information' page" when {
        "there are no services left to update" in new Fixture {
          when {
            bmService.getAdditionalBusinessActivities(any(), any(), any())
          } thenReturn OptionT.some[Future, Set[BusinessActivity]](Set(AccountancyServices))

          when {
            mockServiceFlow.next(any(), any(), any())
          } thenReturn OptionT.none[Future, NextService]

          val result = controller.get()(request)

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(controllers.businessmatching.updateservice.routes.UpdateAnyInformationController.get().url)
        }
      }
    }
  }

  "POST" when {
    "called" must {
      "update UpdateService with isInNewFlow = true" in new Fixture {
        val url = "/service"

        when {
          mockServiceFlow.setInServiceFlowFlag(eqTo(true))(any(), any(), any())
        } thenReturn Future.successful(mock[CacheMap])

        val result = controller.post()(request.withFormUrlEncodedBody("redirectUrl" -> url))

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(url)
      }
    }
  }
}
