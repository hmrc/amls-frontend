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

import connectors.DataCacheConnector
import models.businessactivities.BusinessActivities
import models.moneyservicebusiness.{BusinessUseAnIPSPNo, BusinessUseAnIPSPYes, FundsTransfer, MoneyServiceBusiness}
import org.jsoup.Jsoup
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import  utils.AmlsSpec
import play.api.i18n.Messages
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.AuthorisedFixture

import scala.concurrent.Future

class BusinessUseAnIPSPControllerSpec  extends AmlsSpec {

  trait Fixture extends AuthorisedFixture {
    self => val request = addToken(authRequest)

    val controller = new BusinessUseAnIPSPController {
      override val dataCacheConnector = mock[DataCacheConnector]
      override val authConnector = self.authConnector
    }
  }

  val emptyCache = CacheMap("", Map.empty)

  "BusinessUseAnIPSPController" must {

    "on get display the Business Use An IPSP page" in new Fixture {

      when(controller.dataCacheConnector.fetch[MoneyServiceBusiness](any())
        (any(), any(), any())).thenReturn(Future.successful(None))

      val result = controller.get()(request)
      status(result) must be(OK)
      contentAsString(result) must include(Messages("msb.ipsp.title"))
    }

    "on get display the Business Use An IPSP page with pre populated data" in new Fixture {

      when(controller.dataCacheConnector.fetch[MoneyServiceBusiness](any())
        (any(), any(), any())).thenReturn(Future.successful(Some(MoneyServiceBusiness(None, Some(BusinessUseAnIPSPYes("test", "123456789123456"))))))

      val result = controller.get()(request)
      status(result) must be(OK)

      val document = Jsoup.parse(contentAsString(result))
      document.select("input[value=true]").hasAttr("checked") must be(true)
      document.select("input[name=name]").`val` mustEqual "test"
      document.select("input[name=referenceNumber]").`val` mustEqual "123456789123456"
    }


    "on post with invalid data" in new Fixture {

      val result = controller.post()(request)
      status(result) must be(BAD_REQUEST)
      contentAsString(result) must include (Messages("error.required.msb.ipsp"))
    }

    "on post with valid data" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "useAnIPSP" -> "true",
        "name" -> "name",
        "referenceNumber" -> "123456789123456"
      )

      when(controller.dataCacheConnector.fetch[MoneyServiceBusiness](eqTo(MoneyServiceBusiness.key))
        (any(), any(), any())).thenReturn(Future.successful(None))

      when(controller.dataCacheConnector.save[MoneyServiceBusiness](eqTo(MoneyServiceBusiness.key), any())
        (any(), any(), any())).thenReturn(Future.successful(emptyCache))

      val result = controller.post()(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(controllers.msb.routes.FundsTransferController.get().url))
    }

    "on post with valid data in edit mode with next page's data" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "useAnIPSP" -> "false"
      )

      val incomingModel = MoneyServiceBusiness(
        fundsTransfer = Some(FundsTransfer(true))
      )

      val outgoingModel = incomingModel.copy(
        businessUseAnIPSP = Some(BusinessUseAnIPSPNo)
      )

      when(controller.dataCacheConnector.fetch[MoneyServiceBusiness](eqTo(MoneyServiceBusiness.key))
        (any(), any(), any())).thenReturn(Future.successful(Some(incomingModel)))

      when(controller.dataCacheConnector.save[MoneyServiceBusiness](eqTo(MoneyServiceBusiness.key), any())
        (any(), any(), any())).thenReturn(Future.successful(emptyCache))

      val result = controller.post(true)(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(controllers.msb.routes.SummaryController.get().url))
    }

    "on post with valid data in edit mode without next page's data" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "useAnIPSP" -> "false"
      )

      val incomingModel = MoneyServiceBusiness(
        fundsTransfer = Some(FundsTransfer(true))
      )

      when(controller.dataCacheConnector.fetch[MoneyServiceBusiness](eqTo(MoneyServiceBusiness.key))
        (any(), any(), any())).thenReturn(Future.successful(None))

      when(controller.dataCacheConnector.save[MoneyServiceBusiness](eqTo(MoneyServiceBusiness.key), any())
        (any(), any(), any())).thenReturn(Future.successful(emptyCache))

      val result = controller.post(true)(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(controllers.msb.routes.FundsTransferController.get(true).url))
    }
  }
}
