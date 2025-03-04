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
import forms.hvd.ProductsFormProvider
import models.businessmatching.BusinessActivity.HighValueDealing
import models.hvd.Products.{Alcohol, Antiques, Cars, Other, Tobacco}
import models.hvd._
import models.status.{ReadyForRenewal, SubmissionDecisionApproved, SubmissionDecisionRejected}
import org.jsoup.Jsoup
import org.scalatestplus.mockito.MockitoSugar
import play.api.i18n.Messages
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Injecting}
import services.cache.Cache
import utils.{AmlsSpec, DateOfChangeHelper, DependencyMocks}
import views.html.hvd.ProductsView

class ProductsControllerSpec extends AmlsSpec with MockitoSugar with Injecting {

  val emptyCache = Cache.empty

  "ProductsController" must {

    "load the 'What will your business sell?' page" in new Fixture {
      mockCacheFetch[Hvd](None)
      val result = controller.get()(request)
      status(result)          must be(OK)
      contentAsString(result) must include(Messages("hvd.products.title"))
    }

    "pre-populate the 'What will your business sell?' page" in new Fixture {
      mockCacheFetch(Some(Hvd(products = Some(Products(Set(Alcohol, Tobacco))))))
      val result = controller.get()(request)
      status(result) must be(OK)
      val document = Jsoup.parse(contentAsString(result))
      document.select(s"input[value=${Alcohol.toString}]").hasAttr("checked") must be(true)
      document.select(s"input[value=${Tobacco.toString}]").hasAttr("checked") must be(true)
    }

    "redirect successfully" when {

      "alcohol is selected" in new Fixture with RequestModifiers {
        val newRequest = requestWithAlcohol()
        mockCacheFetch[Hvd](None)
        mockApplicationStatus(SubmissionDecisionRejected)
        val result     = controller.post()(newRequest)
        status(result)           must be(SEE_OTHER)
        redirectLocation(result) must be(Some(routes.ExciseGoodsController.get().url))
      }

      "alcohol is selected in edit mode" in new Fixture with RequestModifiers {
        val newRequest = requestWithAlcohol()
        mockCacheFetch[Hvd](None)
        mockApplicationStatus(SubmissionDecisionRejected)
        val result     = controller.post(true)(newRequest)
        status(result)           must be(SEE_OTHER)
        redirectLocation(result) must be(Some(routes.ExciseGoodsController.get(true).url))
      }

      "alcohol and tobacco are not selected" in new Fixture with RequestModifiers {
        val newRequest = requestWithoutAlcohol()
        mockCacheFetch[Hvd](None)
        mockApplicationStatus(SubmissionDecisionRejected)
        val result     = controller.post()(newRequest)
        status(result)           must be(SEE_OTHER)
        redirectLocation(result) must be(Some(routes.HowWillYouSellGoodsController.get().url))
      }

      "alcohol and tobacco are not selected in edit mode" in new Fixture with RequestModifiers {
        val newRequest = requestWithoutAlcohol()
        mockCacheFetch[Hvd](None)
        mockApplicationStatus(SubmissionDecisionRejected)
        val result     = controller.post(true)(newRequest)
        status(result)           must be(SEE_OTHER)
        redirectLocation(result) must be(Some(routes.SummaryController.get.url))
      }
    }

    "display errors" when {

      "other selected but no text" in new Fixture with RequestModifiers {
        val newRequest = invalidRequestWithEmptyOther()
        mockCacheFetch[Hvd](None)
        val result     = controller.post()(newRequest)
        status(result) must be(BAD_REQUEST)
        val document = Jsoup.parse(contentAsString(result))
        document.select("a[href=#otherDetails]").html() must include(
          Messages("error.required.hvd.business.sell.other.details")
        )
      }

      "other selected but text over valid length" in new Fixture with RequestModifiers {
        val newRequest = invalidRequestWithTooLongOther()
        mockCacheFetch[Hvd](None)
        val result     = controller.post()(newRequest)
        status(result) must be(BAD_REQUEST)
        val document = Jsoup.parse(contentAsString(result))
        document.select("a[href=#otherDetails]").html() must include(
          Messages("error.invalid.hvd.business.sell.other.details")
        )
      }
    }
  }

  "redirect to dateOfChange when a change is made and" when {

    "decision is approved" when {

      "alcohol is selected" in new Fixture with DateOfChangeHelper with RequestModifiers {
        val newRequest = requestWithAlcohol()
        mockCacheFetch[Hvd](None)
        mockApplicationStatus(SubmissionDecisionApproved)
        val result     = controller.post(false)(newRequest)
        status(result)           must be(SEE_OTHER)
        redirectLocation(result) must be(
          Some(routes.HvdDateOfChangeController.get(DateOfChangeRedirect.exciseGoods).url)
        )
      }

      "alcohol is selected and in edit mode" in new Fixture with DateOfChangeHelper with RequestModifiers {
        val newRequest = requestWithAlcohol()
        mockCacheFetch[Hvd](None)
        mockApplicationStatus(SubmissionDecisionApproved)
        val result     = controller.post(true)(newRequest)
        status(result)           must be(SEE_OTHER)
        redirectLocation(result) must be(
          Some(routes.HvdDateOfChangeController.get(DateOfChangeRedirect.exciseGoodsEdit).url)
        )
      }

      "alcohol is not selected" in new Fixture with DateOfChangeHelper with RequestModifiers {
        val newRequest = requestWithoutAlcohol()
        mockCacheFetch[Hvd](None)
        mockApplicationStatus(SubmissionDecisionApproved)
        val result     = controller.post(false)(newRequest)
        status(result)           must be(SEE_OTHER)
        redirectLocation(result) must be(
          Some(routes.HvdDateOfChangeController.get(DateOfChangeRedirect.howWillYouSellGoods).url)
        )
      }

      "alcohol is not selected and in edit mode" in new Fixture with DateOfChangeHelper with RequestModifiers {
        val newRequest = requestWithoutAlcohol()
        mockCacheFetch[Hvd](None)
        mockApplicationStatus(SubmissionDecisionApproved)
        val result     = controller.post(true)(newRequest)
        status(result)           must be(SEE_OTHER)
        redirectLocation(result) must be(
          Some(routes.HvdDateOfChangeController.get(DateOfChangeRedirect.checkYourAnswers).url)
        )
      }
    }

    "decision is ready for renewal" when {
      "alcohol is selected" in new Fixture with DateOfChangeHelper with RequestModifiers {
        val newRequest = requestWithAlcohol()
        mockCacheFetch[Hvd](None)
        mockApplicationStatus(ReadyForRenewal(None))
        val result     = controller.post(false)(newRequest)
        status(result)           must be(SEE_OTHER)
        redirectLocation(result) must be(
          Some(routes.HvdDateOfChangeController.get(DateOfChangeRedirect.exciseGoods).url)
        )
      }

      "alcohol is selected and in edit mode" in new Fixture with DateOfChangeHelper with RequestModifiers {
        val newRequest = requestWithAlcohol()
        mockCacheFetch[Hvd](None)
        mockApplicationStatus(ReadyForRenewal(None))
        val result     = controller.post(true)(newRequest)
        status(result)           must be(SEE_OTHER)
        redirectLocation(result) must be(
          Some(routes.HvdDateOfChangeController.get(DateOfChangeRedirect.exciseGoodsEdit).url)
        )
      }

      "alcohol is not selected" in new Fixture with DateOfChangeHelper with RequestModifiers {
        val newRequest = requestWithoutAlcohol()
        mockCacheFetch[Hvd](None)
        mockApplicationStatus(ReadyForRenewal(None))
        val result     = controller.post(false)(newRequest)
        status(result)           must be(SEE_OTHER)
        redirectLocation(result) must be(
          Some(routes.HvdDateOfChangeController.get(DateOfChangeRedirect.howWillYouSellGoods).url)
        )
      }

      "alcohol is not selected and in edit mode" in new Fixture with DateOfChangeHelper with RequestModifiers {
        val newRequest = requestWithoutAlcohol()
        mockCacheFetch[Hvd](None)
        mockApplicationStatus(ReadyForRenewal(None))
        val result     = controller.post(true)(newRequest)
        status(result)           must be(SEE_OTHER)
        redirectLocation(result) must be(
          Some(routes.HvdDateOfChangeController.get(DateOfChangeRedirect.checkYourAnswers).url)
        )
      }
    }
  }

  "Calling POST" when {
    "the submission is approved" when {
      "the sector has just been added" must {
        "redirect to the next page" when {
          "the user selects 'alcohol' or 'tobacco" in new Fixture {
            val newRequest = FakeRequest(POST, routes.ProductsController.post().url).withFormUrlEncodedBody(
              "products[0]" -> Alcohol.toString,
              "products[1]" -> Tobacco.toString
            )

            mockIsNewActivityNewAuth(true, Some(HighValueDealing))
            mockCacheFetch[Hvd](None)
            mockApplicationStatus(SubmissionDecisionApproved)

            val result = controller.post()(newRequest)
            status(result)           must be(SEE_OTHER)
            redirectLocation(result) must be(Some(routes.ExciseGoodsController.get().url))
          }

          "the user selects something other than alcohol or tobacco" in new Fixture {
            val newRequest = FakeRequest(POST, routes.ProductsController.post().url).withFormUrlEncodedBody(
              "products[0]" -> Antiques.toString,
              "products[1]" -> Cars.toString
            )

            mockIsNewActivityNewAuth(true, Some(HighValueDealing))
            mockCacheFetch[Hvd](None)
            mockApplicationStatus(SubmissionDecisionApproved)

            val result = controller.post()(newRequest)
            status(result)           must be(SEE_OTHER)
            redirectLocation(result) must be(Some(routes.HowWillYouSellGoodsController.get().url))
          }
        }
      }
    }
  }

  trait Fixture extends DependencyMocks {
    self =>
    val request = addToken(authRequest)

    lazy val view  = inject[ProductsView]
    val controller = new ProductsController(
      mockCacheConnector,
      mockStatusService,
      SuccessfulAuthAction,
      ds = commonDependencies,
      mockServiceFlow,
      cc = mockMcc,
      formProvider = inject[ProductsFormProvider],
      view = view
    )

    mockIsNewActivityNewAuth(false)
    mockCacheSave[Hvd]
  }

  trait RequestModifiers {
    def requestWithAlcohol() =
      FakeRequest(POST, routes.ProductsController.post().url).withFormUrlEncodedBody(
        "products[0]"  -> Alcohol.toString,
        "products[1]"  -> Tobacco.toString,
        "products[2]"  -> Other("").toString,
        "otherDetails" -> "test"
      )

    def requestWithoutAlcohol() =
      FakeRequest(POST, routes.ProductsController.post().url).withFormUrlEncodedBody(
        "products[0]" -> Antiques.toString,
        "products[1]" -> Cars.toString
      )

    def invalidRequestWithTooLongOther() =
      FakeRequest(POST, routes.ProductsController.post().url).withFormUrlEncodedBody(
        "products[0]"  -> Alcohol.toString,
        "products[1]"  -> Tobacco.toString,
        "products[2]"  -> Other("").toString,
        "otherDetails" -> "g" * 256
      )

    def invalidRequestWithEmptyOther() =
      FakeRequest(POST, routes.ProductsController.post().url).withFormUrlEncodedBody(
        "products[0]"  -> Alcohol.toString,
        "products[1]"  -> Other("").toString,
        "otherDetails" -> ""
      )
  }

}
