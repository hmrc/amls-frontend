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

package controllers.tradingpremises

import models.tradingpremises._
import org.joda.time.LocalDate
import org.jsoup.Jsoup
import org.mockito.Matchers.{eq => meq}
import org.scalatest.PrivateMethodTester
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import play.api.i18n.Messages
import play.api.test.Helpers._
import utils.{AuthorisedFixture, DependencyMocks, GenericTestHelper}

class IsResidentialControllerSpec extends GenericTestHelper with ScalaFutures with MockitoSugar with PrivateMethodTester{

  trait Fixture extends AuthorisedFixture with DependencyMocks { self =>

    val request = addToken(authRequest)

    val ytpModel = YourTradingPremises("foo", Address("1","2",None,None,"AA1 1BB",None), Some(true), Some(new LocalDate(2010, 10, 10)), None)
    val ytp = Some(ytpModel)

    val pageTitle = Messages("tradingpremises.isResidential.title", "firstname lastname") + " - " +
      Messages("summary.tradingpremises") + " - " +
      Messages("title.amls") + " - " + Messages("title.gov")

    val controller = new IsResidentialController(messagesApi, self.authConnector, mockCacheConnector)
  }

  "IsResidentialController" when {

    "get is called" must {

      "load is residential page with empty form" in new Fixture {

        mockCacheFetch[Seq[TradingPremises]](Some(Seq(TradingPremises(yourTradingPremises =  Some(ytpModel.copy(isResidential = None))))))

        val result = controller.get(1)(request)
        status(result) must be(OK)

        val document = Jsoup.parse(contentAsString(result))
        document.title mustBe pageTitle

      }

      "respond with not found page when YourTradingPremises is None" in new Fixture {

        mockCacheFetch[Seq[TradingPremises]](None)

        val result = controller.get(1)(request)
        status(result) must be(NOT_FOUND)
      }

      "load is residential page with pre - populated data form" in new Fixture {

        mockCacheFetch[Seq[TradingPremises]](Some(Seq(TradingPremises(yourTradingPremises = ytp)))))

        val result = controller.get(1)(request)
        status(result) must be(OK)

        val document = Jsoup.parse(contentAsString(result))
        document.title mustBe pageTitle
        document.select("input[value=true]").hasAttr("checked") must be(true)
      }

    }

    "post is called" must {

      "redirect to next page on valid input" in new Fixture {
        val postRequest = request.withFormUrlEncodedBody(
          "isResidential" -> "true"
        )

        mockCacheFetch[Seq[TradingPremises]](Some(Seq(TradingPremises(yourTradingPremises = ytp)))))
        mockCacheSave[Seq[TradingPremises]]

        val result = controller.post(1)(postRequest)

        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some(routes.WhatDoesYourBusinessDoController.get(1, false).url))
      }

      "redirect to next page on valid input in edit mode" in new Fixture {
        val postRequest = request.withFormUrlEncodedBody(
          "isResidential" -> "false"
        )

        val updatedYtp = Some(YourTradingPremises("foo",
          Address("1","2",None,None,"AA1 1BB",None), Some(false), Some(new LocalDate(2010, 10, 10)), None))
        val updatedTp = TradingPremises(yourTradingPremises = updatedYtp)
        val tp = TradingPremises(yourTradingPremises = ytp)

        mockCacheFetch[Seq[TradingPremises]](Some(Seq(TradingPremises(yourTradingPremises = ytp)))))
        mockCacheSave[Seq[TradingPremises]]

        val result = controller.post(1, true)(postRequest)

        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some(routes.SummaryController.getIndividual(1).url))

      }

      "respond with BAD_REQUEST for invalid form" in new Fixture {
        val postRequest = request.withFormUrlEncodedBody(
          "isResidential" -> ""
        )

        val result = controller.post(1)(postRequest)
        status(result) must be(BAD_REQUEST)

      }
    }

    "isFirstTradingPremises is called" must {
      "return true" in new Fixture {

        val isFirstTradingPremises = PrivateMethod[Boolean]('isFirstTradingPremises)

        val result = controller invokePrivate isFirstTradingPremises(3)

        result must be(true)

      }
    }
  }
}
