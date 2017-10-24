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

package controllers.hvd

import connectors.DataCacheConnector
import models.hvd.Hvd
import models.status.SubmissionReady
import org.jsoup.Jsoup
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import play.api.i18n.Messages
import services.StatusService
import services.businessmatching.ServiceFlow
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.{AuthorisedFixture, DependencyMocks, GenericTestHelper}
import play.api.test.Helpers._

import scala.concurrent.Future

class ExpectToReceiveCashPaymentsControllerSpec extends GenericTestHelper with MockitoSugar {

  trait Fixture extends AuthorisedFixture with DependencyMocks { self =>
    val request = addToken(authRequest)

    val controller = new ExpectToReceiveCashPaymentsController(
      self.authConnector,
      mockCacheConnector,
      mockStatusService,
      mock[ServiceFlow]
    )

    mockCacheFetch[Hvd](None, Some(Hvd.key))
    mockCacheSave[Hvd]
    mockApplicationStatus(SubmissionReady)

    when(controller.serviceFlow.inNewServiceFlow(any())(any(), any(), any()))
      .thenReturn(Future.successful(false))
  }


  "ExpectToReceiveCashPaymentsController" when {

    "get is called" must {
      "display the expect_to_receive view" in new Fixture {

        val result = controller.get()(request)

        status(result) must be(OK)

        val content = contentAsString(result)

        Jsoup.parse(content).title() must include(Messages("hvd.expect.to.receive.title"))
      }
    }

  }

}
