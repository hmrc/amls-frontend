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

package controllers.businessactivities

import connectors.DataCacheConnector
import models.Country
import models.businessactivities.{NonUkAccountantsAddress, UkAccountantsAddress, WhoIsYourAccountant, BusinessActivities}
import org.jsoup.Jsoup
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.PrivateMethodTester
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.{AuthorisedFixture, AmlsSpec}

import scala.concurrent.Future

class WhoIsYourAccountantControllerSpec extends AmlsSpec
  with MockitoSugar
  with ScalaFutures
  with PrivateMethodTester {

  trait Fixture extends AuthorisedFixture {
    self =>
    val request = addToken(authRequest)

    val controller = new WhoIsYourAccountantController {
      override val dataCacheConnector = mock[DataCacheConnector]
      override val authConnector = self.authConnector
    }
  }

  val emptyCache = CacheMap("", Map.empty)

  val mockCacheMap = mock[CacheMap]

  "InvolvedInOtherController" when {

    "get is called" must {
      "show the who is your accountant page with default UK address selected when there is no existing data" in new Fixture {

        when(controller.dataCacheConnector.fetch[BusinessActivities](any())(any(), any(), any()))
          .thenReturn(Future.successful(None))

        val result = controller.get()(request)
        status(result) must be(OK)

        val page = Jsoup.parse(contentAsString(result))
        page.getElementById("name").`val` must be("")
        page.getElementById("tradingName").`val` must be("")
        page.getElementById("isUK-true").hasAttr("checked") must be(true)
        page.getElementById("isUK-false").hasAttr("checked") must be(false)
        page.getElementById("addressLine1").`val` must be("")
        page.getElementById("addressLine2").`val` must be("")
        page.getElementById("addressLine3").`val` must be("")
        page.getElementById("addressLine4").`val` must be("")
        page.getElementById("postCode").`val` must be("")
        page.select("#country option[selected]").attr("value") must be("")
      }

      "show the who is your accountant page when there is existing data" in new Fixture {

        when(controller.dataCacheConnector.fetch[BusinessActivities](any())(any(), any(), any()))
          .thenReturn(Future.successful(Some(BusinessActivities(
            whoIsYourAccountant = Some(WhoIsYourAccountant(
              "testname",
              Some("testtradingName"),
              NonUkAccountantsAddress("line1","line2",Some("line3"),Some("line4"), Country("Albania", "AL"))
            ))
          ))))

        val result = controller.get()(request)
        status(result) must be(OK)

        val page = Jsoup.parse(contentAsString(result))
        page.getElementById("name").`val` must be("testname")
        page.getElementById("tradingName").`val` must be("testtradingName")
        page.getElementById("isUK-true").hasAttr("checked") must be(false)
        page.getElementById("isUK-false").hasAttr("checked") must be(true)
        page.getElementById("addressLine1").`val` must be("line1")
        page.getElementById("addressLine2").`val` must be("line2")
        page.getElementById("addressLine3").`val` must be("line3")
        page.getElementById("addressLine4").`val` must be("line4")
        page.getElementById("postCode").`val` must be("")
        page.select("#country option[selected]").attr("value") must be("AL")
      }
    }

    "post is called" when {

      "given invalid data" must {
        "respond with BAD_REQUEST" in new Fixture {

          val newRequest = request.withFormUrlEncodedBody(
            "name" -> ""
          )

          val result = controller.post()(request)
          status(result) must be(BAD_REQUEST)

        }
      }

      "edit is true" must {
        "respond with SEE_OTHER and redirect to the SummaryController" in new Fixture {

          val newRequest = request.withFormUrlEncodedBody(
            "name" -> "testName",
            "tradingName" -> "tradingName",
            "isUK" -> "true",
            "addressLine1" -> "line1",
            "addressLine2" -> "line2",
            "addressLine3" -> "line3",
            "addressLine4" -> "line4",
            "postCode" -> "AA11AA"
          )

          when(controller.dataCacheConnector.fetch[BusinessActivities](any())
            (any(), any(), any())).thenReturn(Future.successful(None))

          when(controller.dataCacheConnector.save[BusinessActivities](any(), any())
            (any(), any(), any())).thenReturn(Future.successful(emptyCache))

          val result = controller.post(true)(newRequest)
          status(result) must be(SEE_OTHER)

          redirectLocation(result) must be(Some(routes.SummaryController.get().url))
        }
      }

      "edit is false" must {
        "respond with SEE_OTHER and redirect to the TaxMattersController" in new Fixture {

          val newRequest = request.withFormUrlEncodedBody(
            "name" -> "testName",
            "tradingName" -> "tradingName",
            "isUK" -> "true",
            "addressLine1" -> "line1",
            "addressLine2" -> "line2",
            "addressLine3" -> "line3",
            "addressLine4" -> "line4",
            "postCode" -> "AA11AA"
          )

          when(controller.dataCacheConnector.fetch[BusinessActivities](any())
            (any(), any(), any())).thenReturn(Future.successful(None))

          when(controller.dataCacheConnector.save[BusinessActivities](any(), any())
            (any(), any(), any())).thenReturn(Future.successful(emptyCache))

          val result = controller.post(false)(newRequest)
          status(result) must be(SEE_OTHER)

          redirectLocation(result) must be(Some(routes.TaxMattersController.get().url))
        }
      }
    }
  }
}
