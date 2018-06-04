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

package controllers.msb

import models.moneyservicebusiness._
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import play.api.test.Helpers._
import utils.{AmlsSpec, AuthorisedFixture, DependencyMocks}
import play.api.i18n.Messages
import uk.gov.hmrc.http.cache.client.CacheMap

import scala.concurrent.Future

class FundsTransferControllerSpec extends AmlsSpec with MockitoSugar with ScalaFutures {

  trait Fixture extends AuthorisedFixture with DependencyMocks{
    self => val request = addToken(authRequest)

    val controller = new FundsTransferController(mockCacheConnector, authConnector = self.authConnector)
  }

  val emptyCache = CacheMap("", Map.empty)

  "FundsTransferControllerSpec" should {

    "on get, display the 'Do you transfer money without using formal banking systems?' page" in new Fixture {
      when(mockCacheConnector.fetch[MoneyServiceBusiness](any())
          (any(), any(), any())).thenReturn(Future.successful(None))
      val result = controller.get()(request)
      status(result) must be(OK)
      contentAsString(result) must include(Messages("msb.fundstransfer.title") + " - " + Messages("summary.msb") + " - " + Messages("title.amls") + " - " + Messages("title.gov"))
    }

    "on get, display the 'Do you transfer money without using formal banking systems?' page with pre populated data" in new Fixture {

      when(mockCacheConnector.fetch[MoneyServiceBusiness](any())
      (any(), any(), any())).thenReturn(Future.successful(Some(MoneyServiceBusiness(fundsTransfer = Some(FundsTransfer(true))))))
      val result = controller.get()(request)

      status(result) must be(OK)

      val page = Jsoup.parse(contentAsString(result))

      page.select("input[type=radio][name=transferWithoutFormalSystems][checked]").`val`() must be("true")
    }

    "on post with invalid data" in new Fixture {
      val newRequest = request.withFormUrlEncodedBody(
        "transferWithoutFormalSystems" -> ""
      )

      val result = controller.post()(newRequest)
      status(result) must be(BAD_REQUEST)

      val document: Document = Jsoup.parse(contentAsString(result))
      document.select("span").html() must include(Messages("error.required.msb.fundsTransfer"))
    }

    "on post with valid data when user selects Yes" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "transferWithoutFormalSystems" -> "true"
      )

      when(mockCacheConnector.fetch[MoneyServiceBusiness](any())
      (any(), any(), any())).thenReturn(Future.successful(None))

      when(mockCacheConnector.save[MoneyServiceBusiness](any(), any())
      (any(), any(), any())).thenReturn(Future.successful(emptyCache))

      val result = controller.post()(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(routes.TransactionsInNext12MonthsController.get().url))
    }

    "on post with valid data whe user selects No" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "transferWithoutFormalSystems" -> "false"
      )

      when(mockCacheConnector.fetch[MoneyServiceBusiness](any())
        (any(), any(), any())).thenReturn(Future.successful(None))

      when(mockCacheConnector.save[MoneyServiceBusiness](any(), any())
        (any(), any(), any())).thenReturn(Future.successful(emptyCache))

      val result = controller.post()(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(routes.TransactionsInNext12MonthsController.get().url))
    }

    "on post with valid data in edit mode when the next page's data is in the store" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "transferWithoutFormalSystems" -> "true"
      )

      val incomingModel = MoneyServiceBusiness(
        transactionsInNext12Months = Some(
          TransactionsInNext12Months("10")
        )
      )

      val outgoingModel = incomingModel.copy(
        fundsTransfer = Some(
          FundsTransfer(true)
        ), hasChanged = true
      )

      when(mockCacheConnector.fetch[MoneyServiceBusiness](eqTo(MoneyServiceBusiness.key))
       (any(), any(), any())).thenReturn(Future.successful(Some(incomingModel)))

      when(mockCacheConnector.save[MoneyServiceBusiness](eqTo(MoneyServiceBusiness.key), eqTo(outgoingModel))
       (any(), any(), any())).thenReturn(Future.successful(emptyCache))

      val result = controller.post(true)(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(routes.SummaryController.get().url))
    }

    "on post with valid data in edit mode when the next page's data is not in the store" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "transferWithoutFormalSystems" -> "true"
      )

      val incomingModel = MoneyServiceBusiness()

      val outgoingModel = incomingModel.copy(
        fundsTransfer = Some(
          FundsTransfer(true)
        ), hasChanged = true
      )

      when(mockCacheConnector.fetch[MoneyServiceBusiness](eqTo(MoneyServiceBusiness.key))
        (any(), any(), any())).thenReturn(Future.successful(Some(incomingModel)))

      when(mockCacheConnector.save[MoneyServiceBusiness](eqTo(MoneyServiceBusiness.key), eqTo(outgoingModel))
        (any(), any(), any())).thenReturn(Future.successful(emptyCache))

      val result = controller.post(true)(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(routes.TransactionsInNext12MonthsController.get(true).url))
    }
  }
}