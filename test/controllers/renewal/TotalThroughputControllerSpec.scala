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

package controllers.renewal

import connectors.DataCacheConnector
import models.businessmatching._
import models.renewal.{TotalThroughput, Renewal}
import org.mockito.ArgumentCaptor
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import play.api.i18n.Messages
import play.api.mvc.Result
import play.api.test.Helpers._
import services.RenewalService
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.{AuthorisedFixture, GenericTestHelper}

import scala.concurrent.Future

class TotalThroughputControllerSpec extends GenericTestHelper with MockitoSugar {

  trait Fixture extends AuthorisedFixture {
    self =>
    implicit val request = addToken(authRequest)

    val renewalService = mock[RenewalService]
    val dataCacheConnector = mock[DataCacheConnector]
    val renewal = Renewal()

    when {
      renewalService.getRenewal(any(), any(), any())
    } thenReturn Future.successful(Some(renewal))

    lazy val controller = new TotalThroughputController(
      self.authConnector,
      renewalService,
      dataCacheConnector
    )
  }

  trait FormSubmissionFixture extends Fixture { self =>

    val formData = "throughput" -> "01"
    val formRequest = request.withFormUrlEncodedBody(formData)
    val cache = mock[CacheMap]

    when {
      renewalService.updateRenewal(any())(any(), any(), any())
    } thenReturn Future.successful(cache)

    when {
      dataCacheConnector.fetchAll(any(), any())
    } thenReturn Future.successful(Some(cache))

    setupActivities(Set(HighValueDealing, MoneyServiceBusiness))

    def post(edit: Boolean = false)(block: Result => Unit) =
      block(await(controller.post(edit)(formRequest)))

    def setupActivities(activities: Set[BusinessActivity]) = when {
        cache.getEntry[BusinessMatching](BusinessMatching.key)
      } thenReturn Some(BusinessMatching(activities = Some(BusinessActivities(activities))))
  }

  "The MSB throughput controller" must {
    "return the view" in new Fixture {
      val result = controller.get()(request)

      status(result) mustBe OK

      contentAsString(result) must include(Messages("renewal.msb.throughput.header"))

      verify(renewalService).getRenewal(any(), any(), any())
    }

    "return a bad request result when an invalid form is posted" in new Fixture {
      val result = controller.post()(request)

      status(result) mustBe BAD_REQUEST
    }
  }

  "A valid form post to the MSB throughput controller" must {
    "redirect to the next page in the flow if edit = false" in new FormSubmissionFixture {
      post() { result =>
        result.header.status mustBe SEE_OTHER
        result.header.headers.get("Location") mustBe Some(controllers.renewal.routes.TransactionsInLast12MonthsController.get().url)
      }
    }

    "redirect to the summary page if edit = true" in new FormSubmissionFixture {
      post(edit = true) { result =>
        result.header.status mustBe SEE_OTHER
        result.header.headers.get("Location") mustBe Some(controllers.renewal.routes.SummaryController.get().url)
      }
    }

    "save the throughput model into the renewals model when posted" in new FormSubmissionFixture {
      post() { _ =>
        val captor = ArgumentCaptor.forClass(classOf[Renewal])

        verify(renewalService).updateRenewal(captor.capture())(any(), any(), any())
        captor.getValue.totalThroughput mustBe Some(TotalThroughput("01"))
      }
    }
  }
}
