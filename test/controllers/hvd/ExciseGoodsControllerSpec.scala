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

package controllers.hvd

import controllers.actions.SuccessfulAuthAction
import forms.hvd.ExciseGoodsFormProvider
import models.businessmatching.BusinessActivity.HighValueDealing
import models.hvd.{ExciseGoods, Hvd}
import models.status.{ReadyForRenewal, SubmissionDecisionApproved, SubmissionDecisionRejected}
import org.jsoup.Jsoup
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Injecting}
import services.cache.Cache
import utils.{AmlsSpec, DateOfChangeHelper, DependencyMocks}
import views.html.hvd.ExciseGoodsView

class ExciseGoodsControllerSpec extends AmlsSpec with Injecting {

  trait Fixture extends DependencyMocks {
    self =>
    val request = addToken(authRequest)

    lazy val view  = inject[ExciseGoodsView]
    val controller = new ExciseGoodsController(
      mockCacheConnector,
      mockStatusService,
      SuccessfulAuthAction,
      ds = commonDependencies,
      mockServiceFlow,
      cc = mockMcc,
      formProvider = inject[ExciseGoodsFormProvider],
      view = view
    )

    mockCacheFetch[Hvd](None)
    mockCacheSave[Hvd]
    mockIsNewActivityNewAuth(false)
  }

  val emptyCache = Cache.empty

  "ExciseGoodsController" must {

    "successfully load UI for the first time" in new Fixture {

      val result = controller.get()(request)
      status(result) must be(OK)

      val htmlValue = Jsoup.parse(contentAsString(result))
      htmlValue.title mustBe messages("hvd.excise.goods.title") + " - " + messages("summary.hvd") + " - " + messages(
        "title.amls"
      ) + " - " + messages("title.gov")
    }

    "successfully load UI from mongoCache" in new Fixture {

      mockCacheFetch(Some(Hvd(exciseGoods = Some(ExciseGoods(true)))))

      val result = controller.get()(request)
      status(result) must be(OK)

      val htmlValue = Jsoup.parse(contentAsString(result))
      htmlValue.title mustBe messages("hvd.excise.goods.title") + " - " + messages("summary.hvd") + " - " + messages(
        "title.amls"
      ) + " - " + messages("title.gov")
      htmlValue.getElementById("exciseGoods").`val`() mustBe "true"
      htmlValue.getElementById("exciseGoods-2").`val`() mustBe "false"
    }

    "successfully redirect to next page when submitted with valid data" in new Fixture {

      val newRequest = FakeRequest(POST, routes.ExciseGoodsController.post().url)
        .withFormUrlEncodedBody("exciseGoods" -> "true")

      mockApplicationStatus(SubmissionDecisionRejected)

      val result = controller.post()(newRequest)
      status(result)           must be(SEE_OTHER)
      redirectLocation(result) must be(Some(controllers.hvd.routes.HowWillYouSellGoodsController.get().url))
    }

    "successfully redirect to next page when submitted with valid data in edit mode" in new Fixture {

      val newRequest = FakeRequest(POST, routes.ExciseGoodsController.post().url)
        .withFormUrlEncodedBody("exciseGoods" -> "false")

      mockApplicationStatus(SubmissionDecisionRejected)

      val result = controller.post(true)(newRequest)
      status(result)           must be(SEE_OTHER)
      redirectLocation(result) must be(Some(controllers.hvd.routes.SummaryController.get.url))
    }

    "fail with validation error when mandatory field is missing" in new Fixture {
      val newRequest = FakeRequest(POST, routes.ExciseGoodsController.post().url)
        .withFormUrlEncodedBody("" -> "")

      val result = controller.post()(newRequest)
      status(result)          must be(BAD_REQUEST)
      contentAsString(result) must include(messages("error.required.hvd.excise.goods"))
    }

    "redirect to dateOfChange" when {

      "the model has been changed and application is approved and in edit mode" in new Fixture with DateOfChangeHelper {

        val hvd        = Hvd(exciseGoods = Some(ExciseGoods(true)))
        val newRequest = FakeRequest(POST, routes.ExciseGoodsController.post().url)
          .withFormUrlEncodedBody("exciseGoods" -> "false")

        mockApplicationStatus(SubmissionDecisionApproved)
        mockCacheFetch(Some(hvd))

        val result = controller.post(true)(newRequest)
        status(result)           must be(SEE_OTHER)
        redirectLocation(result) must be(
          Some(controllers.hvd.routes.HvdDateOfChangeController.get(DateOfChangeRedirect.checkYourAnswers).url)
        )
      }

      "the model has been changed and application is ready for renewal and in edit mode" in new Fixture
        with DateOfChangeHelper {

        val hvd        = Hvd(exciseGoods = Some(ExciseGoods(true)))
        val newRequest = FakeRequest(POST, routes.ExciseGoodsController.post().url)
          .withFormUrlEncodedBody("exciseGoods" -> "false")

        mockApplicationStatus(ReadyForRenewal(None))
        mockCacheFetch(Some(hvd))

        val result = controller.post(true)(newRequest)
        status(result)           must be(SEE_OTHER)
        redirectLocation(result) must be(
          Some(controllers.hvd.routes.HvdDateOfChangeController.get(DateOfChangeRedirect.checkYourAnswers).url)
        )
      }

      "the model has been changed and application is approved" in new Fixture with DateOfChangeHelper {

        val hvd        = Hvd(exciseGoods = Some(ExciseGoods(true)))
        val newRequest = FakeRequest(POST, routes.ExciseGoodsController.post().url)
          .withFormUrlEncodedBody("exciseGoods" -> "false")

        mockApplicationStatus(SubmissionDecisionApproved)
        mockCacheFetch(Some(hvd))

        val result = controller.post(false)(newRequest)
        status(result)           must be(SEE_OTHER)
        redirectLocation(result) must be(
          Some(controllers.hvd.routes.HvdDateOfChangeController.get(DateOfChangeRedirect.howWillYouSellGoods).url)
        )
      }

      "the model has been changed and application is ready for renewal" in new Fixture with DateOfChangeHelper {

        val hvd        = Hvd(exciseGoods = Some(ExciseGoods(true)))
        val newRequest = FakeRequest(POST, routes.ExciseGoodsController.post().url)
          .withFormUrlEncodedBody("exciseGoods" -> "false")

        mockApplicationStatus(ReadyForRenewal(None))
        mockCacheFetch(Some(hvd))

        val result = controller.post(false)(newRequest)
        status(result)           must be(SEE_OTHER)
        redirectLocation(result) must be(
          Some(controllers.hvd.routes.HvdDateOfChangeController.get(DateOfChangeRedirect.howWillYouSellGoods).url)
        )
      }
    }
  }

  "Calling POST" when {
    "the submission is approved" when {
      "the sector has just been added" must {
        "progress to the next page" in new Fixture {
          val newRequest = FakeRequest(POST, routes.ExciseGoodsController.post().url)
            .withFormUrlEncodedBody("exciseGoods" -> "true")

          mockApplicationStatus(SubmissionDecisionApproved)
          mockIsNewActivityNewAuth(true, Some(HighValueDealing))

          val result = controller.post()(newRequest)

          status(result)           must be(SEE_OTHER)
          redirectLocation(result) must be(Some(controllers.hvd.routes.HowWillYouSellGoodsController.get().url))
        }
      }
    }
  }
}
