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

package controllers.tradingpremises

import models.businessmatching.BusinessMatching
import models.tradingpremises._
import org.joda.time.LocalDate
import org.jsoup.Jsoup
import org.mockito.Matchers.{eq => eqTo, _}
import org.scalatest.PrivateMethodTester
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.mockito.Mockito._
import play.api.i18n.Messages
import play.api.test.Helpers._
import utils.{AuthorisedFixture, DependencyMocks, AmlsSpec, StatusConstants}

class IsResidentialControllerSpec extends AmlsSpec with ScalaFutures with MockitoSugar with PrivateMethodTester {

  trait Fixture extends AuthorisedFixture with DependencyMocks { self =>

    val request = addToken(authRequest)

    val ytp = YourTradingPremises("foo", Address("1","2",None,None,"AA1 1BB",None), Some(true), Some(new LocalDate(2010, 10, 10)), None)

    val pageTitle = Messages("tradingpremises.isResidential.title", "firstname lastname") + " - " +
      Messages("summary.tradingpremises") + " - " +
      Messages("title.amls") + " - " + Messages("title.gov")

    mockCacheGetEntry[Seq[TradingPremises]](Some(Seq(TradingPremises())), TradingPremises.key)
    mockCacheGetEntry[BusinessMatching](Some(BusinessMatching()), BusinessMatching.key)

    val controller = new IsResidentialController(messagesApi, self.authConnector, mockCacheConnector)

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
          val postRequest = request.withFormUrlEncodedBody(
            "isResidential" -> "true"
          )

          mockCacheGetEntry[Seq[TradingPremises]](
            Some(Seq(TradingPremises(yourTradingPremises = Some(ytp)))),
            TradingPremises.key
          )

          val result = controller.post(1)(postRequest)

          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(routes.WhatDoesYourBusinessDoController.get(1).url))
        }

        "redirect to SummaryController in edit mode" in new Fixture {
          val postRequest = request.withFormUrlEncodedBody(
            "isResidential" -> "false"
          )

          val updatedYtp = Some(YourTradingPremises("foo",
            Address("1", "2", None, None, "AA1 1BB", None), Some(false), Some(new LocalDate(2010, 10, 10)), None))
          val updatedTp = TradingPremises(yourTradingPremises = updatedYtp)
          val tp = TradingPremises(yourTradingPremises = Some(ytp))

          mockCacheGetEntry[Seq[TradingPremises]](
            Some(Seq(TradingPremises(yourTradingPremises = Some(ytp)))),
            TradingPremises.key
          )

          val result = controller.post(1, true)(postRequest)

          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(routes.SummaryController.getIndividual(1).url))

        }

      }

      "on invalid request" must {
        "respond with BAD_REQUEST" in new Fixture {
          val postRequest = request.withFormUrlEncodedBody(
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

          val isFirstTradingPremises = PrivateMethod[Boolean]('isFirstTradingPremises)

          val result = controller invokePrivate isFirstTradingPremises(tp, 3)

          result must be(true)

        }

        "trading premises have not been deleted" in new Fixture {

          val tp = Seq(
            TradingPremises(),
            TradingPremises()
          )

          val isFirstTradingPremises = PrivateMethod[Boolean]('isFirstTradingPremises)

          val result = controller invokePrivate isFirstTradingPremises(tp, 1)

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

          val isFirstTradingPremises = PrivateMethod[Boolean]('isFirstTradingPremises)

          val result = controller invokePrivate isFirstTradingPremises(tp, 3)

          result must be(false)

        }

        "trading premises have not been deleted" in new Fixture {

          val tp = Seq(
            TradingPremises(status = Some(StatusConstants.Deleted)),
            TradingPremises()
          )

          val isFirstTradingPremises = PrivateMethod[Boolean]('isFirstTradingPremises)

          val result = controller invokePrivate isFirstTradingPremises(tp, 1)

          result must be(false)

        }
      }
    }
  }

  it must {

    "save an updated Trading Premises model" in new Fixture {
      val postRequest = request.withFormUrlEncodedBody(
        "isResidential" -> "true"
      )

      override val ytp = YourTradingPremises(
        "foo",
        Address("1","2",None,None,"AA1 1BB",None),
        Some(false),
        Some(new LocalDate(2010, 10, 10)),
        None
      )

      val tradingPremises = Seq(TradingPremises(yourTradingPremises = Some(ytp)))

      mockCacheGetEntry[Seq[TradingPremises]](
        Some(tradingPremises),
        TradingPremises.key
      )

      val result = controller.post(1)(postRequest)

      status(result) must be(SEE_OTHER)

      verify(controller.dataCacheConnector).save[Seq[TradingPremises]](eqTo(TradingPremises.key), eqTo(Seq(TradingPremises(
        yourTradingPremises = Some(ytp.copy(isResidential = Some(true))),
        hasChanged = true
      ))))(any(),any(),any())

    }

  }

}