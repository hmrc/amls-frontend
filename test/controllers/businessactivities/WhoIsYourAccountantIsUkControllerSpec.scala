/*
 * Copyright 2020 HM Revenue & Customs
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
import controllers.actions.SuccessfulAuthAction
import models.Country
import models.businessactivities._
import org.jsoup.Jsoup
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.PrivateMethodTester
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.{AmlsSpec, AuthorisedFixture, AutoCompleteServiceMocks}

import scala.concurrent.Future

class WhoIsYourAccountantIsUkControllerSpec extends AmlsSpec
  with MockitoSugar
  with ScalaFutures
  with PrivateMethodTester {

  trait Fixture extends AuthorisedFixture with AutoCompleteServiceMocks{
    self =>
    val request = addToken(authRequest)

    val controller = new WhoIsYourAccountantIsUkController (
      dataCacheConnector = mock[DataCacheConnector],
      authAction = SuccessfulAuthAction,
      autoCompleteService = mockAutoComplete
    )
  }

  val emptyCache = CacheMap("", Map.empty)

  val mockCacheMap = mock[CacheMap]

  "InvolvedInOtherController" when {

    "get is called" must {
      "show the who is your accountant page when there is no existing data" in new Fixture {

        when(controller.dataCacheConnector.fetch[BusinessActivities](any(), any())(any(), any()))
          .thenReturn(Future.successful(None))

        val result = controller.get()(request)
        status(result) must be(OK)

        val page = Jsoup.parse(contentAsString(result))
        page.getElementById("isUK-true").hasAttr("checked") must be(false)
        page.getElementById("isUK-false").hasAttr("checked") must be(false)
      }

      "show the who is your accountant page when there is existing data" in new Fixture {

        when(controller.dataCacheConnector.fetch[BusinessActivities](any(), any())(any(), any()))
          .thenReturn(Future.successful(Some(BusinessActivities(
            whoIsYourAccountant = Some(WhoIsYourAccountant(
              Some(WhoIsYourAccountantName("testname", Some("testtradingName"))),
              Some(WhoIsYourAccountantIsUk(false)),
              Some(NonUkAccountantsAddress("line1","line2",Some("line3"),Some("line4"), Country("Albania", "AL")))
            ))
          ))))

        val result = controller.get()(request)
        status(result) must be(OK)

        val page = Jsoup.parse(contentAsString(result))
        page.getElementById("isUK-true").hasAttr("checked") must be(false)
        page.getElementById("isUK-false").hasAttr("checked") must be(true)
      }
    }

    "post is called" when {

      "given invalid data" must {
        "respond with BAD_REQUEST" in new Fixture {

          when(controller.dataCacheConnector.fetch[BusinessActivities](any(), any())(any(), any()))
            .thenReturn(Future.successful(Some(BusinessActivities(
              whoIsYourAccountant = Some(WhoIsYourAccountant(
                Some(WhoIsYourAccountantName("testname", Some("testtradingName"))),
                None,
                None
              ))
            ))))

          val newRequest = request.withFormUrlEncodedBody(
            "isUK" -> ""
          )

          val result = controller.post()(request)
          status(result) must be(BAD_REQUEST)
        }
      }

      "edit is true" must {
        "respond with SEE_OTHER and redirect to the SummaryController" in new Fixture {

          val newRequest = request.withFormUrlEncodedBody(
            "isUK" -> "true"
          )

          when(controller.dataCacheConnector.fetch[BusinessActivities](any(), any())(any(), any()))
            .thenReturn(Future.successful(None))

          when(controller.dataCacheConnector.save[BusinessActivities](any(), any(), any())(any(), any()))
            .thenReturn(Future.successful(emptyCache))

          val result = controller.post(true)(newRequest)
          status(result) must be(SEE_OTHER)

          redirectLocation(result) must be(Some(routes.WhoIsYourAccountantUkAddressController.get(true).url))
        }
      }

      "edit is false" must {
        "respond with SEE_OTHER and redirect to the WhoIsYourAccountantUkAddressController" in new Fixture {

          val newRequest = request.withFormUrlEncodedBody(
            "isUK" -> "true"
          )

          when(controller.dataCacheConnector.fetch[BusinessActivities](any(), any())(any(), any()))
            .thenReturn(Future.successful(None))

          when(controller.dataCacheConnector.save[BusinessActivities](any(), any(), any())(any(), any()))
            .thenReturn(Future.successful(emptyCache))

          val result = controller.post(false)(newRequest)
          status(result) must be(SEE_OTHER)

          redirectLocation(result) must be(Some(routes.WhoIsYourAccountantUkAddressController.get().url))
        }

        "respond with SEE_OTHER and redirect to the WhoIsYourAccountantNonUkAddressController" in new Fixture {

          val newRequest = request.withFormUrlEncodedBody(
            "isUK" -> "false"
          )

          when(controller.dataCacheConnector.fetch[BusinessActivities](any(), any())(any(), any()))
            .thenReturn(Future.successful(None))

          when(controller.dataCacheConnector.save[BusinessActivities](any(), any(), any())(any(), any()))
            .thenReturn(Future.successful(emptyCache))

          val result = controller.post(false)(newRequest)
          status(result) must be(SEE_OTHER)

          redirectLocation(result) must be(Some(routes.WhoIsYourAccountantNonUkAddressController.get().url))
        }
      }
    }
  }
}
