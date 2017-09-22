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

package controllers.asp

import connectors.DataCacheConnector
import models.asp.Asp
import org.mockito.Matchers.{eq => eqTo, any}
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import utils.{AuthorisedFixture, DependencyMocks, GenericTestHelper}
import play.api.test.Helpers._

import scala.concurrent.Future

class SummaryControllerSpec extends GenericTestHelper with MockitoSugar {

  trait Fixture extends AuthorisedFixture with DependencyMocks {
    self => val request = addToken(authRequest)

    val controller = new SummaryController {
      override val dataCache = mockCacheConnector
      override val authConnector = self.authConnector
    }

    mockCacheSave[Asp]
  }

  "Get" must {

    "load the summary page when section data is available" in new Fixture {

      val model = Asp(None, None)

      mockCacheFetch[Asp](Some(model))

      val result = controller.get()(request)
      status(result) must be(OK)
    }

    "redirect to the main summary page when section data is unavailable" in new Fixture {

      mockCacheFetch[Asp](None)

      val result = controller.get()(request)
      status(result) must be(SEE_OTHER)
    }
  }

  "Post" must {
    "load the Asp model and set hasAccepted to true" in new Fixture {
      val postRequest = request.withFormUrlEncodedBody()

      val model = Asp(None, None)
      mockCacheFetch(Some(model))

      val result = controller.post()(postRequest)
      status(result) mustBe SEE_OTHER

      verify(mockCacheConnector).save[Asp](eqTo(Asp.key), eqTo(model.copy(hasAccepted = true)))(any(), any(), any())
    }
  }
}
