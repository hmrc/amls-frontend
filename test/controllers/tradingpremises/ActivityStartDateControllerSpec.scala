/*
 * Copyright 2019 HM Revenue & Customs
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
import connectors.DataCacheConnector
import controllers.actions.SuccessfulAuthAction
import models.tradingpremises._
import org.joda.time.LocalDate
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.Matchers.{any, eq => meq}
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.{AmlsSpec, AuthAction, AuthorisedFixture}

import scala.collection.JavaConverters._
import scala.concurrent.Future

class ActivityStartDateControllerSpec extends AmlsSpec with ScalaFutures with MockitoSugar {

  val address = Address("1", "2", None, None, "AA1 1BB", None)

  trait Fixture  {
    self =>
    val request = addToken(authRequest)

    val cache: DataCacheConnector = mock[DataCacheConnector]
    val authAction: AuthAction = SuccessfulAuthAction

    val controller = new ActivityStartDateController(messagesApi, authAction, commonDependencies, self.cache, cc = mockMcc)
  }

  "ActivityStartDateController" must {
    val ytpModel = YourTradingPremises("foo", address, None, Some(new LocalDate(2010, 10, 10)), None)
    val ytp = Some(ytpModel)

    val emptyCache = CacheMap("", Map.empty)
    "GET:" must {

      "successfully load activity start page with empty form" in new Fixture {

        when(controller.dataCacheConnector.fetch[Seq[TradingPremises]](any(), any())(any(), any()))
          .thenReturn(Future.successful(Some(Seq(TradingPremises(yourTradingPremises =  Some(ytpModel.copy(startDate = None)))))))

        val result = controller.get(1, false)(request)
        status(result) must be(OK)
      }

      "redirect  to not found page when YourTradingPremises is None" in new Fixture {

        when(controller.dataCacheConnector.fetch[Seq[TradingPremises]](any(), any())(any(), any()))
          .thenReturn(Future.successful(None))

        val result = controller.get(1, false)(request)
        status(result) must be(NOT_FOUND)
      }

      "successfully load activity start page with pre - populated data form" in new Fixture {
        when(controller.dataCacheConnector.fetch[Seq[TradingPremises]](any(), any())(any(), any()))
          .thenReturn(Future.successful(Some(Seq(TradingPremises(yourTradingPremises = ytp)))))

        val result = controller.get(1, false)(request)
        status(result) must be(OK)
        val document: Document = Jsoup.parse(contentAsString(result))
        document.getElementsByTag("p").asScala.count(e => e.text.contains(address.postcode)) mustEqual 1
        document.select("input[name=startDate.day]").`val` must include("10")
        document.select("input[name=startDate.month]").`val` must include("10")
        document.select("input[name=startDate.year]").`val` must include("2010")
      }
    }

    "POST:" must {
      "successfully redirect to next page on valid input" in new Fixture {
        val postRequest = requestWithUrlEncodedBody(
          "startDate.day" -> "20",
          "startDate.month" -> "5",
          "startDate.year" -> "2014"
        )
        when(controller.dataCacheConnector.fetch[Seq[TradingPremises]](any(), any())(any(), any()))
          .thenReturn(Future.successful(Some(Seq(TradingPremises(yourTradingPremises = ytp)))))

        when(controller.dataCacheConnector.save[TradingPremises](any(), any(), any())(any(), any()))
          .thenReturn(Future.successful(emptyCache))

        val result = controller.post(1, false)(postRequest)
        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some(routes.IsResidentialController.get(1, false).url))
      }

      "successfully redirect to next page on valid input in edit mode" in new Fixture {
        val postRequest = requestWithUrlEncodedBody(
          "startDate.day" -> "20",
          "startDate.month" -> "5",
          "startDate.year" -> "2014"
        )
        val updatedYtp = Some(YourTradingPremises("foo",
          Address("1","2",None,None,"AA1 1BB",None), None, Some(new LocalDate(2014, 5, 20)), None))

        val updatedTp = TradingPremises(yourTradingPremises = updatedYtp, hasChanged = true)
        val tp = TradingPremises(yourTradingPremises = ytp, hasChanged = true)

        when(controller.dataCacheConnector.fetch[Seq[TradingPremises]](any(), any())(any(), any()))
          .thenReturn(Future.successful(Some(Seq(TradingPremises(yourTradingPremises = ytp)))))

        when(controller.dataCacheConnector.save[Seq[TradingPremises]](any(), any(), meq(Seq(updatedTp)))(any(),  any()))
          .thenReturn(Future.successful(emptyCache))

        val result = controller.post(1, true)(postRequest)
        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some(routes.DetailedAnswersController.get(1).url))

      }

      "throw error on missing required field" in new Fixture {
        val postRequest = requestWithUrlEncodedBody(
          "startDate.day" -> "",
          "startDate.month" -> "5",
          "startDate.year" -> "2014"
        )

        when(controller.dataCacheConnector.fetch[Seq[TradingPremises]](any(), any())(any(), any()))
          .thenReturn(Future.successful(Some(Seq(TradingPremises(yourTradingPremises = ytp)))))

        val result = controller.post(1, false)(postRequest)
        status(result) must be(BAD_REQUEST)

      }
    }

  }
}
