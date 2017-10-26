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
import models.hvd.{ExciseGoods, Hvd}
import models.status.{ReadyForRenewal, SubmissionDecisionApproved, SubmissionDecisionRejected}
import org.jsoup.Jsoup
import org.mockito.Matchers._
import org.mockito.Mockito._
import play.api.i18n.Messages
import play.api.test.FakeApplication
import play.api.test.Helpers._
import services.StatusService
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.{AuthorisedFixture, DependencyMocks, GenericTestHelper}
import models.businessmatching.HighValueDealing

import scala.concurrent.Future

class ExciseGoodsControllerSpec extends GenericTestHelper {

  override lazy val app = FakeApplication(additionalConfiguration = Map("microservice.services.feature-toggle.release7" -> true) )

  trait Fixture extends AuthorisedFixture with DependencyMocks {
    self => val request = addToken(authRequest)

    val controller = new ExciseGoodsController(
      mockCacheConnector,
      mockStatusService,
      self.authConnector,
      mockServiceFlow
    )

    mockCacheFetch[Hvd](None)
    mockCacheSave[Hvd]
    setupInServiceFlow(false)
  }

  val emptyCache = CacheMap("", Map.empty)

  "ExciseGoodsController" must {

    "successfully load UI for the first time" in new Fixture {
      val result = controller.get()(request)
      status(result) must be(OK)

      val htmlValue = Jsoup.parse(contentAsString(result))
      htmlValue.title mustBe Messages("hvd.excise.goods.title") + " - " + Messages("summary.hvd") + " - " + Messages("title.amls") + " - " + Messages("title.gov")
    }

    "successfully load UI from save4later" in new Fixture {

      mockCacheFetch(Some(Hvd(exciseGoods = Some(ExciseGoods(true)))))

      val result = controller.get()(request)
      status(result) must be(OK)

      val htmlValue = Jsoup.parse(contentAsString(result))
      htmlValue.title mustBe Messages("hvd.excise.goods.title") + " - " + Messages("summary.hvd") + " - " + Messages("title.amls") + " - " + Messages("title.gov")
      htmlValue.getElementById("exciseGoods-true").`val`() mustBe "true"
      htmlValue.getElementById("exciseGoods-false").`val`() mustBe "false"
    }

    "successfully redirect to next page when submitted with valid data" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody("exciseGoods" -> "true")

      mockApplicationStatus(SubmissionDecisionRejected)

      val result = controller.post()(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(controllers.hvd.routes.HowWillYouSellGoodsController.get().url))
    }

    "successfully redirect to nex page when submitted with valida data in edit mode" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody("exciseGoods" -> "false")

      mockApplicationStatus(SubmissionDecisionRejected)

      val result = controller.post(true)(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(controllers.hvd.routes.SummaryController.get().url))
    }

    "fail with validation error when mandatory field is missing" in new Fixture {
      val newRequest = request.withFormUrlEncodedBody()

      val result = controller.post()(newRequest)
      status(result) must be(BAD_REQUEST)
      contentAsString(result) must include(Messages("error.required.hvd.excise.goods"))
    }

    "redirect to dateOfChange when the model has been changed and application is approved" in new Fixture{

      val hvd = Hvd(exciseGoods = Some(ExciseGoods(true)))
      val newRequest = request.withFormUrlEncodedBody("exciseGoods" -> "false")

      mockApplicationStatus(SubmissionDecisionApproved)
      mockCacheFetch(Some(hvd))

      val result = controller.post(true)(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(controllers.hvd.routes.HvdDateOfChangeController.get().url))
    }

    "redirect to dateOfChange when the model has been changed and application is ready for renewal" in new Fixture{

      val hvd = Hvd(exciseGoods = Some(ExciseGoods(true)))
      val newRequest = request.withFormUrlEncodedBody("exciseGoods" -> "false")

      mockApplicationStatus(ReadyForRenewal(None))
      mockCacheFetch(Some(hvd))

      val result = controller.post(true)(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(controllers.hvd.routes.HvdDateOfChangeController.get().url))
    }
  }

  "Calling POST" when {
    "the submission is approved" when {
      "the sector has just been added" must {
        "progress to the next page" in new Fixture {
          val newRequest = request.withFormUrlEncodedBody("exciseGoods" -> "true")

          mockApplicationStatus(SubmissionDecisionApproved)
          setupInServiceFlow(true, Some(HighValueDealing))

          val result = controller.post()(newRequest)

          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(controllers.hvd.routes.HowWillYouSellGoodsController.get().url))
        }
      }
    }
  }
}
