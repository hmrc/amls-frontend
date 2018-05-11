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

package controllers.businessmatching

import connectors.DataCacheConnector
import models.Country
import models.businessmatching.{BusinessMatching, BusinessType}
import org.mockito.Mockito._
import org.mockito.Matchers.{eq => meq, _}
import models.businesscustomer.{ReviewDetails, Address => BusinessCustomerAddress}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.{AuthorisedFixture, AmlsSpec}

import scala.concurrent.Future


class ConfirmPostCodeControllerSpec extends AmlsSpec with MockitoSugar with ScalaFutures {

  trait Fixture extends AuthorisedFixture {
    self => val request = addToken(authRequest)

    val dataCacheConnector = mock[DataCacheConnector]

    lazy val app = new GuiceApplicationBuilder()
      .disable[com.kenshoo.play.metrics.PlayModule]
      .overrides(bind[DataCacheConnector].to(dataCacheConnector))
      .overrides(bind[AuthConnector].to(self.authConnector))
      .build()

    val controller = app.injector.instanceOf[ConfirmPostCodeController]
  }

  val emptyCache = CacheMap("", Map.empty)
  val businessAddress = BusinessCustomerAddress("line1", "line2", Some("line3"), Some("line4"), Some("AA1 1AA"), Country("United Kingdom", "GB"))
  val reviewDtls = ReviewDetails(
    "BusinessName",
    Some(BusinessType.LimitedCompany),
    BusinessCustomerAddress("line1", "line2", Some("line3"), Some("line4"), Some("AA1 1AA"), Country("United Kingdom", "GB")),
    "ghghg"
  )

  val businessMatching = BusinessMatching(reviewDetails = Some(reviewDtls))


  "ConfirmPostCodeController" must {

    "display confirm post code of your business page successfully" in new Fixture {
      val result = controller.get()(request)
      status(result) must be(OK)
    }

    "update ReviewDetails with valid input post code" in new Fixture {
      val postRequest = request.withFormUrlEncodedBody {
        "postCode" -> "BB1 1BB"
      }
      when(controller.dataCacheConnector.fetch[BusinessMatching](any())
        (any(), any(), any())).thenReturn(Future.successful(Some(businessMatching)))

      val updatedModel = businessMatching.copy(reviewDetails = Some(reviewDtls.copy(businessAddress = businessAddress.copy(postcode = Some("BB1 1BB")))))
      when(controller.dataCacheConnector.save[BusinessMatching](any(), meq(updatedModel))
        (any(), any(), any())).thenReturn(Future.successful(emptyCache))

      val result = controller.post()(postRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(routes.BusinessTypeController.get().url))
    }

    "update ReviewDetails as none when business matching-> reviewDetails is empty" in new Fixture {
      val postRequest = request.withFormUrlEncodedBody {
        "postCode" -> "BB1 1BB"
      }
      when(controller.dataCacheConnector.fetch[BusinessMatching](any())
        (any(), any(), any())).thenReturn(Future.successful(Some(businessMatching.copy(reviewDetails = None))))

      val updatedModel = businessMatching.copy(reviewDetails = None)
      when(controller.dataCacheConnector.save[BusinessMatching](any(), meq(updatedModel))
        (any(), any(), any())).thenReturn(Future.successful(emptyCache))

      val result = controller.post()(postRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(routes.BusinessTypeController.get().url))
    }

    "throw validation error on invalid field" in new Fixture {

      val postRequest = request.withFormUrlEncodedBody {
        "postCode" -> "AA1111AA"
      }
      val result = controller.post()(postRequest)
      status(result) must be(BAD_REQUEST)
    }
  }

}
