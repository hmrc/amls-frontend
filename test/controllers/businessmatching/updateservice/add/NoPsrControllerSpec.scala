/*
 * Copyright 2018 HM Revenue & Customs
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
import controllers.businessmatching.updateservice.UpdateServiceHelper
import models.businessmatching._
import models.flowmanagement.{AddServiceFlowModel, NoPSRPageId}
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures
import org.mockito.Matchers.any
import play.api.i18n.Messages
import play.api.test.Helpers._
import utils.{AuthorisedFixture, DependencyMocks, GenericTestHelper}

import scala.concurrent.Future

class NoPsrControllerSpec extends GenericTestHelper with ScalaFutures {

  trait Fixture extends AuthorisedFixture with DependencyMocks {
    self =>

    val request = addToken(authRequest)

    val mockUpdateServiceHelper = mock[UpdateServiceHelper]

    val controller = new NoPsrController(
      authConnector = self.authConnector,
      dataCacheConnector = mockCacheConnector,
      helper = mockUpdateServiceHelper,
      router = createRouter[AddServiceFlowModel]
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
        mockUpdateServiceHelper.clearFlowModel()(any(), any())
      } thenReturn OptionT[Future, AddServiceFlowModel](Future.successful(Some(AddServiceFlowModel())))

      val result = controller.post()(request.withFormUrlEncodedBody())

      status(result) mustBe SEE_OTHER
      controller.router.verify(NoPSRPageId, AddServiceFlowModel())
    }
  }
}
