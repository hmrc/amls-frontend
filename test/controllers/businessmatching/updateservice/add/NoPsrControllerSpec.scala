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

package controllers.businessmatching.updateservice.add

import cats.data.OptionT
import controllers.actions.SuccessfulAuthAction
import controllers.businessmatching.updateservice.AddBusinessTypeHelper
import models.flowmanagement.{AddBusinessTypeFlowModel, NoPSRPageId}
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures
import play.api.i18n.Messages
import play.api.test.Helpers._
import utils.{AmlsSpec, AuthorisedFixture, DependencyMocks, DependencyMocksNewAuth}

import scala.concurrent.Future

class NoPsrControllerSpec extends AmlsSpec with ScalaFutures {

  trait Fixture extends AuthorisedFixture with DependencyMocksNewAuth {
    self =>

    val request = addToken(authRequest)

    val mockUpdateServiceHelper = mock[AddBusinessTypeHelper]

    val controller = new NoPsrController(
      authAction = SuccessfulAuthAction,
      dataCacheConnector = mockCacheConnector,
      helper = mockUpdateServiceHelper,
      router = createRouter[AddBusinessTypeFlowModel]
    )
  }

  "get" when {
    "called" must {
      "return an OK status" when {
        "with the correct content" in new Fixture {

          val result = controller.get()(request)

          status(result) mustBe OK
          contentAsString(result) must include(Messages("businessmatching.updateservice.nopsr.cannotcontinuewiththeapplication.title"))
        }
      }
    }
  }

  "post is called" must {

    "clear the flow model" in new Fixture {
      when {
        mockUpdateServiceHelper.clearFlowModel(any())(any())
      } thenReturn OptionT[Future, AddBusinessTypeFlowModel](Future.successful(Some(AddBusinessTypeFlowModel())))

      val result = controller.post()(request.withFormUrlEncodedBody())

      status(result) mustBe SEE_OTHER
      controller.router.verify("internalId", NoPSRPageId, AddBusinessTypeFlowModel())
    }
  }
}
