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

package controllers.businessactivities

import connectors.DataCacheConnector
import models.businessactivities._
import org.jsoup.Jsoup
import org.mockito.Matchers.{eq => eqTo, any}
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import utils.{AuthorisedFixture, DependencyMocks, GenericTestHelper}
import play.api.i18n.Messages
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector

import scala.concurrent.Future

class TransactionRecordControllerSpec extends GenericTestHelper with MockitoSugar {

  trait Fixture extends AuthorisedFixture with DependencyMocks {
    self =>

    val request = addToken(authRequest)
    val controller = new TransactionRecordController(self.authConnector, mockCacheConnector)

    mockCacheSave[BusinessActivities]
  }

  val emptyCache = CacheMap("", Map.empty)

  "TransactionRecordController" when {

    "get is called" must {
      "load the Customer Record Page with an empty form" in new Fixture {
        mockCacheFetch[BusinessActivities](None)

        val result = controller.get()(request)
        status(result) must be(OK)

        val page = Jsoup.parse(contentAsString(result))
        page.getElementById("isRecorded-true").hasAttr("checked") must be(false)
        page.getElementById("isRecorded-false").hasAttr("checked") must be(false)
      }

      "pre-populate the Customer Record Page" in new Fixture {
        mockCacheFetch(Some(BusinessActivities(
          transactionRecord = Some(true)
        )))

        val result = controller.get()(request)
        status(result) must be(OK)

        val page = Jsoup.parse(contentAsString(result))
        page.getElementById("isRecorded-true").hasAttr("checked") must be(true)
      }
    }

    "post is called" must {
      "respond with SEE_OTHER" when {
        "given valid data not in edit mode" in new Fixture {
          val newRequest = request.withFormUrlEncodedBody(
            "isRecorded" -> "true"
          )

          mockCacheFetch[BusinessActivities](None)

          val result = controller.post()(newRequest)
          status(result) must be(SEE_OTHER)
          redirectLocation(result) mustBe Some(routes.TransactionTypesController.get().url)
        }

        "given valid data not in edit mode, and 'no' is selected" in new Fixture {
          val newRequest = request.withFormUrlEncodedBody(
            "isRecorded" -> "false"
          )

          mockCacheFetch[BusinessActivities](None)

          val result = controller.post()(newRequest)
          status(result) must be(SEE_OTHER)
          redirectLocation(result) mustBe Some(routes.IdentifySuspiciousActivityController.get().url)
        }

        "given valid data in edit mode and 'no' is selected" in new Fixture {

          val newRequest = request.withFormUrlEncodedBody(
            "isRecorded" -> "false"
          )

          mockCacheFetch[BusinessActivities](None)

          val result = controller.post(true)(newRequest)
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(routes.SummaryController.get().url))
        }

        "given valid data in edit mode and 'yes' is selected" in new Fixture {

          val newRequest = request.withFormUrlEncodedBody(
            "isRecorded" -> "true"
          )

          mockCacheFetch[BusinessActivities](None)

          val result = controller.post(true)(newRequest)
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(routes.TransactionTypesController.get(edit = true).url))
        }
      }

      "reset the transaction types if 'no' is selected" in new Fixture {
        val newRequest = request.withFormUrlEncodedBody(
          "isRecorded" -> "false"
        )

        mockCacheFetch[BusinessActivities](Some(BusinessActivities(
          transactionRecord = Some(true),
          transactionRecordTypes = Some(TransactionTypes(Set(Paper)))
        )))

        val result = controller.post()(newRequest)
        status(result) must be(SEE_OTHER)
        redirectLocation(result) mustBe Some(routes.IdentifySuspiciousActivityController.get().url)

        verify(mockCacheConnector).save[BusinessActivities](
          eqTo(BusinessActivities.key),
          eqTo(BusinessActivities(
            transactionRecord = Some(false),
            transactionRecordTypes = None,
            hasChanged = true,
            hasAccepted = false
          )
        ))(any(), any(), any())
      }

      "respond with BAD_REQUEST when given invalid data" in new Fixture {
        val newRequest = request.withFormUrlEncodedBody()

        mockCacheFetch[BusinessActivities](None)

        val result = controller.post()(newRequest)
        status(result) must be(BAD_REQUEST)
      }
    }
  }

}
