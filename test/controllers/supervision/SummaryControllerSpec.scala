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

package controllers.supervision

import models.asp.Asp
import models.supervision.Supervision
import org.mockito.ArgumentCaptor
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.{AmlsSpec, AuthorisedFixture, DependencyMocks}

import scala.concurrent.Future

class SummaryControllerSpec extends AmlsSpec with MockitoSugar {

  trait Fixture extends AuthorisedFixture  with DependencyMocks{
    self => val request = addToken(authRequest)

    val controller = new SummaryController(mockCacheConnector, authConnector = self.authConnector)

    val model = Supervision(None)
  }

  "Get" must {

    "load the summary page when section data is available" in new Fixture {



      when(controller.dataCacheConnector.fetch[Supervision](any())
        (any(), any(), any())).thenReturn(Future.successful(Some(model)))

      val result = controller.get()(request)
      status(result) must be(OK)
    }

    "redirect to the main summary page when section data is unavailable" in new Fixture {

      when(controller.dataCacheConnector.fetch[Asp](any())
        (any(), any(), any())).thenReturn(Future.successful(None))

      val result = controller.get()(request)
      status(result) must be(SEE_OTHER)
    }
  }

  "POST" must {
    "update the hasAccepted flag on the model" in new Fixture {
      val cache = mock[CacheMap]

      when {
        controller.dataCacheConnector.fetch[Supervision](any())(any(), any(), any())
      } thenReturn Future.successful(Some(model.copy(hasAccepted = false)))

      when {
        controller.dataCacheConnector.save[Supervision](eqTo(Supervision.key), any())(any(), any(), any())
      } thenReturn Future.successful(cache)

      val result = controller.post()(request)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(controllers.routes.RegistrationProgressController.get.url)

      val captor = ArgumentCaptor.forClass(classOf[Supervision])
      verify(controller.dataCacheConnector).save[Supervision](eqTo(Supervision.key), captor.capture())(any(), any(), any())
      captor.getValue.hasAccepted mustBe true
    }
  }
}
