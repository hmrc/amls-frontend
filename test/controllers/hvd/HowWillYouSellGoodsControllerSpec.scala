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
import forms.hvd.SalesChannelFormProvider
import models.businessmatching.BusinessActivity.HighValueDealing
import models.hvd.SalesChannel.{Retail, Wholesale}
import models.hvd.{HowWillYouSellGoods, Hvd}
import models.status.{ReadyForRenewal, SubmissionDecisionApproved, SubmissionDecisionRejected}
import org.jsoup.Jsoup
import play.api.test.Helpers.{BAD_REQUEST, OK, SEE_OTHER, contentAsString, redirectLocation, status, _}
import play.api.test.{FakeRequest, Injecting}
import services.cache.Cache
import utils.{AmlsSpec, DateOfChangeHelper, DependencyMocks}
import views.html.hvd.HowWillYouSellGoodsView

class HowWillYouSellGoodsControllerSpec extends AmlsSpec with Injecting {

  trait Fixture extends DependencyMocks {
    self =>
    val request = addToken(authRequest)

    lazy val view  = inject[HowWillYouSellGoodsView]
    val controller = new HowWillYouSellGoodsController(
      mockCacheConnector,
      mockStatusService,
      SuccessfulAuthAction,
      ds = commonDependencies,
      mockServiceFlow,
      cc = mockMcc,
      formProvider = inject[SalesChannelFormProvider],
      view = view
    )

    mockCacheFetch[Hvd](None)
    mockCacheSave[Hvd]
    mockIsNewActivityNewAuth(false)
  }

  val emptyCache = Cache.empty

  "load UI for the first time" in new Fixture {
    val result = controller.get()(request)
    status(result) must be(OK)
    val htmlValue = Jsoup.parse(contentAsString(result))
    htmlValue.title mustBe messages("hvd.how-will-you-sell-goods.title") + " - " + messages(
      "summary.hvd"
    ) + " - " + messages("title.amls") + " - " + messages("title.gov")
  }

  "load UI from mongoCache" in new Fixture {

    mockCacheFetch(Some(Hvd(howWillYouSellGoods = Some(HowWillYouSellGoods(Set(Retail))))))

    val result = controller.get()(request)
    status(result) must be(OK)

    val htmlValue = Jsoup.parse(contentAsString(result))
    htmlValue.title mustBe messages("hvd.how-will-you-sell-goods.title") + " - " + messages(
      "summary.hvd"
    ) + " - " + messages("title.amls") + " - " + messages("title.gov")
    htmlValue.getElementById("salesChannels_1").`val`() mustBe Retail.toString
  }

  "redirect to next page" when {
    "submitted with valid data" in new Fixture {

      val newRequest = FakeRequest(POST, routes.HowWillYouSellGoodsController.post().url)
        .withFormUrlEncodedBody("salesChannels[0]" -> Retail.toString)

      mockApplicationStatus(SubmissionDecisionRejected)

      val result = controller.post()(newRequest)
      status(result)           must be(SEE_OTHER)
      redirectLocation(result) must be(Some(controllers.hvd.routes.CashPaymentController.get().url))
    }

    "submitted with valid data in edit mode" in new Fixture {

      val newRequest = FakeRequest(POST, routes.HowWillYouSellGoodsController.post().url)
        .withFormUrlEncodedBody("salesChannels[0]" -> Retail.toString)

      mockApplicationStatus(SubmissionDecisionRejected)

      val result = controller.post(true)(newRequest)
      status(result)           must be(SEE_OTHER)
      redirectLocation(result) must be(Some(controllers.hvd.routes.SummaryController.get.url))
    }
  }

  "fail with validation error when mandatory field is missing" in new Fixture {
    val newRequest = FakeRequest(POST, routes.HowWillYouSellGoodsController.post().url)
      .withFormUrlEncodedBody("" -> "")

    val result = controller.post()(newRequest)
    status(result)          must be(BAD_REQUEST)
    contentAsString(result) must include(messages("error.required.hvd.how-will-you-sell-goods"))
  }

  "redirect to dateOfChange" when {

    "the model has been changed and" when {

      val hvd = Hvd(howWillYouSellGoods = Some(HowWillYouSellGoods(Set(Wholesale))))

      "application is approved" in new Fixture with DateOfChangeHelper {
        val newRequest = FakeRequest(POST, routes.HowWillYouSellGoodsController.post().url)
          .withFormUrlEncodedBody("salesChannels[0]" -> Retail.toString)
        mockApplicationStatus(SubmissionDecisionApproved)
        mockCacheFetch(Some(hvd))
        val result     = controller.post(false)(newRequest)
        status(result)           must be(SEE_OTHER)
        redirectLocation(result) must be(
          Some(controllers.hvd.routes.HvdDateOfChangeController.get(DateOfChangeRedirect.cashPayment).url)
        )
      }

      "application is approved and in edit mode" in new Fixture with DateOfChangeHelper {
        val newRequest = FakeRequest(POST, routes.HowWillYouSellGoodsController.post().url)
          .withFormUrlEncodedBody("salesChannels[0]" -> Retail.toString)
        mockApplicationStatus(ReadyForRenewal(None))
        mockCacheFetch(Some(hvd))
        val result     = controller.post(true)(newRequest)
        status(result)           must be(SEE_OTHER)
        redirectLocation(result) must be(
          Some(controllers.hvd.routes.HvdDateOfChangeController.get(DateOfChangeRedirect.checkYourAnswers).url)
        )
      }

      "application is ready for renewal" in new Fixture with DateOfChangeHelper {
        val newRequest = FakeRequest(POST, routes.HowWillYouSellGoodsController.post().url)
          .withFormUrlEncodedBody("salesChannels[0]" -> Retail.toString)
        mockApplicationStatus(ReadyForRenewal(None))
        mockCacheFetch(Some(hvd))
        val result     = controller.post(false)(newRequest)
        status(result)           must be(SEE_OTHER)
        redirectLocation(result) must be(
          Some(controllers.hvd.routes.HvdDateOfChangeController.get(DateOfChangeRedirect.cashPayment).url)
        )
      }

      "application is ready for renewal and in edit mode" in new Fixture with DateOfChangeHelper {
        val newRequest = FakeRequest(POST, routes.HowWillYouSellGoodsController.post().url)
          .withFormUrlEncodedBody("salesChannels[0]" -> Retail.toString)
        mockApplicationStatus(ReadyForRenewal(None))
        mockCacheFetch(Some(hvd))
        val result     = controller.post(true)(newRequest)
        status(result)           must be(SEE_OTHER)
        redirectLocation(result) must be(
          Some(controllers.hvd.routes.HvdDateOfChangeController.get(DateOfChangeRedirect.checkYourAnswers).url)
        )
      }
    }
  }

  "Calling POST" when {
    "the status is approved" when {
      "the service has just been added" must {
        "redirect to the next page in the flow" in new Fixture {
          val newRequest = FakeRequest(POST, routes.HowWillYouSellGoodsController.post().url)
            .withFormUrlEncodedBody("salesChannels[0]" -> Retail.toString)

          mockApplicationStatus(SubmissionDecisionApproved)
          mockIsNewActivityNewAuth(true, Some(HighValueDealing))

          val result = controller.post()(newRequest)
          status(result)           must be(SEE_OTHER)
          redirectLocation(result) must be(Some(controllers.hvd.routes.CashPaymentController.get().url))
        }
      }
    }
  }
}
