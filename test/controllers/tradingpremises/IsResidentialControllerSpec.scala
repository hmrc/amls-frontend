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

package controllers.tradingpremises

import controllers.actions.SuccessfulAuthAction
import forms.tradingpremises.IsResidentialFormProvider
import models.businessmatching.BusinessMatching
import models.tradingpremises._
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Injecting}
import utils.{AmlsSpec, DependencyMocks, StatusConstants}
import views.html.tradingpremises.IsResidentialView

import java.time.LocalDate

class IsResidentialControllerSpec extends AmlsSpec with ScalaFutures with MockitoSugar with Injecting {

  trait Fixture extends DependencyMocks { self =>

    val request = addToken(authRequest)

    val ytp = YourTradingPremises(
      "foo",
      Address(
        "1st line of address",
        Some("2nd line of address"),
        Some("3rd line of address"),
        Some("4th line of address"),
        "AA1 1BB",
        None
      ),
      Some(true),
      Some(LocalDate.of(2010, 10, 10)),
      None
    )

    val pageTitle  = messages("tradingpremises.isResidential.title", "firstname lastname") + " - " +
      messages("summary.tradingpremises") + " - " +
      messages("title.amls") + " - " + messages("title.gov")

    mockCacheGetEntry[Seq[TradingPremises]](Some(Seq(TradingPremises())), TradingPremises.key)
    mockCacheGetEntry[BusinessMatching](Some(BusinessMatching()), BusinessMatching.key)
    lazy val view  = app.injector.instanceOf[IsResidentialView]
    val controller = new IsResidentialController(
      messagesApi,
      SuccessfulAuthAction,
      ds = commonDependencies,
      mockCacheConnector,
      cc = mockMcc,
      inject[IsResidentialFormProvider],
      view = view,
      error = errorView
    )

    mockCacheSave[Seq[TradingPremises]]
  }

  "IsResidentialController" when {

    "get is called" must {

      "load is residential page" when {
        "with empty form" in new Fixture {

          mockCacheGetEntry[Seq[TradingPremises]](
            Some(Seq(TradingPremises(yourTradingPremises = Some(ytp.copy(isResidential = None))))),
            TradingPremises.key
          )

          val result = controller.get(1)(request)
          status(result) must be(OK)

          val document = Jsoup.parse(contentAsString(result))
          document.title mustBe pageTitle

        }

        "with pre - populated data form" in new Fixture {

          mockCacheGetEntry[Seq[TradingPremises]](
            Some(Seq(TradingPremises(yourTradingPremises = Some(ytp)))),
            TradingPremises.key
          )

          val result = controller.get(1)(request)
          status(result) must be(OK)

          val document = Jsoup.parse(contentAsString(result))
          document.title mustBe pageTitle
          document.select("input[value=true]").hasAttr("checked") must be(true)
          document.body().text()                                  must include(ytp.tradingPremisesAddress.addressLine1)
          document.body().text()                                  must include(ytp.tradingPremisesAddress.addressLine2.get)
          document.body().text()                                  must include(ytp.tradingPremisesAddress.addressLine3.get)
          document.body().text()                                  must include(ytp.tradingPremisesAddress.addressLine4.get)
          document.body().text()                                  must include(ytp.tradingPremisesAddress.postcode)
        }

      }

      "respond with not found page" when {
        "TradingPremises is None" in new Fixture {

          mockCacheGetEntry[Seq[TradingPremises]](None, TradingPremises.key)

          val result = controller.get(1)(request)
          status(result) must be(NOT_FOUND)
        }
      }

    }

    "post is called" must {

      "on valid request" must {

        "redirect to WhatDoesYourBusinessDoController" in new Fixture {
          val postRequest = FakeRequest(POST, routes.IsResidentialController.post(1, false).url)
            .withFormUrlEncodedBody(
              "isResidential" -> "true"
            )

          mockCacheGetEntry[Seq[TradingPremises]](
            Some(Seq(TradingPremises(yourTradingPremises = Some(ytp)))),
            TradingPremises.key
          )

          val result = controller.post(1)(postRequest)

          status(result)           must be(SEE_OTHER)
          redirectLocation(result) must be(Some(routes.WhatDoesYourBusinessDoController.get(1).url))
        }

        "redirect to DetailedAnswersController in edit mode" in new Fixture {
          val postRequest = FakeRequest(POST, routes.IsResidentialController.post(1, true).url)
            .withFormUrlEncodedBody(
              "isResidential" -> "false"
            )

          mockCacheGetEntry[Seq[TradingPremises]](
            Some(Seq(TradingPremises(yourTradingPremises = Some(ytp)))),
            TradingPremises.key
          )

          val result = controller.post(1, true)(postRequest)

          status(result)           must be(SEE_OTHER)
          redirectLocation(result) must be(Some(routes.CheckYourAnswersController.get(1).url))

        }

      }

      "on invalid request" must {
        "respond with BAD_REQUEST" in new Fixture {
          val postRequest = FakeRequest(POST, routes.IsResidentialController.post(1, false).url)
            .withFormUrlEncodedBody(
              "isResidential" -> ""
            )

          val result = controller.post(1)(postRequest)
          status(result) must be(BAD_REQUEST)

        }
      }

    }

    "isFirstTradingPremises is called" must {
      "return true" when {
        "trading premises have been deleted" in new Fixture {

          val tp = Seq(
            TradingPremises(status = Some(StatusConstants.Deleted)),
            TradingPremises(status = Some(StatusConstants.Deleted)),
            TradingPremises(),
            TradingPremises()
          )

          val result = controller.isFirstTradingPremises(tp, 3)

          result must be(true)
        }

        "trading premises have not been deleted" in new Fixture {

          val tp = Seq(
            TradingPremises(),
            TradingPremises()
          )

          val result = controller.isFirstTradingPremises(tp, 1)

          result must be(true)
        }
      }

      "return false" when {
        "trading premises have been deleted" in new Fixture {

          val tp = Seq(
            TradingPremises(),
            TradingPremises(status = Some(StatusConstants.Deleted)),
            TradingPremises(),
            TradingPremises()
          )

          val result = controller.isFirstTradingPremises(tp, 3)

          result must be(false)

        }

        "trading premises have not been deleted" in new Fixture {

          val tp = Seq(
            TradingPremises(status = Some(StatusConstants.Deleted)),
            TradingPremises()
          )

          val result = controller.isFirstTradingPremises(tp, 1)

          result must be(false)
        }
      }
    }
  }

  it must {

    "save an updated Trading Premises model" in new Fixture {
      val postRequest = FakeRequest(POST, routes.IsResidentialController.post(1, false).url)
        .withFormUrlEncodedBody(
          "isResidential" -> "true"
        )

      override val ytp = YourTradingPremises(
        "foo",
        Address("1", None, None, None, "AA1 1BB", None),
        Some(false),
        Some(LocalDate.of(2010, 10, 10)),
        None
      )

      val tradingPremises = Seq(TradingPremises(yourTradingPremises = Some(ytp)))

      mockCacheGetEntry[Seq[TradingPremises]](
        Some(tradingPremises),
        TradingPremises.key
      )

      val result = controller.post(1)(postRequest)

      status(result) must be(SEE_OTHER)

      verify(controller.dataCacheConnector).save[Seq[TradingPremises]](
        any(),
        eqTo(TradingPremises.key),
        eqTo(
          Seq(
            TradingPremises(
              yourTradingPremises = Some(ytp.copy(isResidential = Some(true))),
              hasChanged = true
            )
          )
        )
      )(any())

    }

  }

}
