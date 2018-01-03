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
import models.businessactivities.{AccountantForAMLSRegulations, BusinessActivities}
import org.jsoup.Jsoup
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import  utils.GenericTestHelper
import play.api.i18n.Messages
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.AuthorisedFixture

import scala.concurrent.Future


class AccountantForAMLSRegulationsControllerSpec extends GenericTestHelper with MockitoSugar {

  trait Fixture extends AuthorisedFixture {
    self => val request = addToken(authRequest)
    val controller = new AccountantForAMLSRegulationsController {
      override val dataCacheConnector = mock[DataCacheConnector]
      override val authConnector = self.authConnector
    }
  }

  val emptyCache = CacheMap("", Map.empty)

  "AccountantForAMLSRegulationsController" when {

    "get is called" must {

      "load the Accountant For AMLSRegulations page with an empty form" in new Fixture {

        when(controller.dataCacheConnector.fetch[BusinessActivities](any())(any(), any(), any()))
          .thenReturn(Future.successful(None))

        val result = controller.get()(request)
        status(result) must be(OK)

        val htmlValue = Jsoup.parse(contentAsString(result))
        htmlValue.getElementById("accountantForAMLSRegulations-true").hasAttr("checked") must be(false)
        htmlValue.getElementById("accountantForAMLSRegulations-false").hasAttr("checked") must be(false)
      }

      "pre-populate the form when data is already present" in new Fixture {

        val accountantForAMLSRegulations = Some(AccountantForAMLSRegulations(true))
        val activities = BusinessActivities(accountantForAMLSRegulations = accountantForAMLSRegulations)

        when(controller.dataCacheConnector.fetch[BusinessActivities](any())(any(), any(), any()))
          .thenReturn(Future.successful(Some(activities)))

        val result = controller.get()(request)
        status(result) must be(OK)

        val htmlValue = Jsoup.parse(contentAsString(result))
        htmlValue.getElementById("accountantForAMLSRegulations-true").hasAttr("checked") must be(true)
        htmlValue.getElementById("accountantForAMLSRegulations-false").hasAttr("checked") must be(false)

      }
    }

    "Post is called" must {

      "respond with SEE_OTHER" when {
        "edit is true" must {
          "redirect to the WhoIsYourAccountantController when 'yes' is selected'" in new Fixture {

            val newRequest = request.withFormUrlEncodedBody("accountantForAMLSRegulations" -> "true")

            when(controller.dataCacheConnector.fetch[BusinessActivities](any())(any(), any(), any()))
              .thenReturn(Future.successful(None))

            when(controller.dataCacheConnector.save[BusinessActivities](any(), any())(any(), any(), any()))
              .thenReturn(Future.successful(emptyCache))

            val result = controller.post(true)(newRequest)
            status(result) must be(SEE_OTHER)
            redirectLocation(result) must be(Some(controllers.businessactivities.routes.WhoIsYourAccountantController.get().url))
          }

          "successfully redirect to the SummaryController on selection of Option 'No'" in new Fixture {

            val newRequest = request.withFormUrlEncodedBody(
              "accountantForAMLSRegulations" -> "false"
            )
            when(controller.dataCacheConnector.fetch[BusinessActivities](any())(any(), any(), any()))
              .thenReturn(Future.successful(None))

            when(controller.dataCacheConnector.save[BusinessActivities](any(), any())(any(), any(), any()))
              .thenReturn(Future.successful(emptyCache))

            val result = controller.post(true)(newRequest)
            status(result) must be(SEE_OTHER)
            redirectLocation(result) must be(Some(controllers.businessactivities.routes.SummaryController.get().url))
          }
        }

        "edit is false" must {
          "redirect to the WhoIsYourAccountantController on selection of 'Yes'" in new Fixture {
            val newRequest = request.withFormUrlEncodedBody("accountantForAMLSRegulations" -> "true")

            when(controller.dataCacheConnector.fetch[BusinessActivities](any())(any(), any(), any()))
              .thenReturn(Future.successful(None))

            when(controller.dataCacheConnector.save[BusinessActivities](any(), any())(any(), any(), any()))
              .thenReturn(Future.successful(emptyCache))

            val result = controller.post(false)(newRequest)
            status(result) must be(SEE_OTHER)
            redirectLocation(result) must be(Some(controllers.businessactivities.routes.WhoIsYourAccountantController.get().url))
          }

          "successfully redirect to the SummaryController on selection of Option 'No'" in new Fixture {
            val newRequest = request.withFormUrlEncodedBody(
              "accountantForAMLSRegulations" -> "false"
            )
            when(controller.dataCacheConnector.fetch[BusinessActivities](any())(any(), any(), any()))
              .thenReturn(Future.successful(None))

            when(controller.dataCacheConnector.save[BusinessActivities](any(), any())(any(), any(), any()))
              .thenReturn(Future.successful(emptyCache))

            val result = controller.post(false)(newRequest)
            status(result) must be(SEE_OTHER)
            redirectLocation(result) must be(Some(controllers.businessactivities.routes.SummaryController.get().url))
          }
        }
      }

      "respond with BAD_REQUEST" when {
        "no options are selected so that the request body is empty" in new Fixture {

          val newRequest = request.withFormUrlEncodedBody()

          when(controller.dataCacheConnector.fetch[BusinessActivities](any())(any(), any(), any()))
            .thenReturn(Future.successful(None))

          val result = controller.post()(newRequest)
          status(result) must be(BAD_REQUEST)
          contentAsString(result) must include(Messages("error.required.ba.business.use.accountant"))

        }

        "given invalid json" in new Fixture {

          val newRequest = request.withFormUrlEncodedBody(
            "WhatYouNeedController" -> ""
          )

          when(controller.dataCacheConnector.fetch[BusinessActivities](any())(any(), any(), any()))
            .thenReturn(Future.successful(None))

          val result = controller.post()(newRequest)
          status(result) must be(BAD_REQUEST)
          contentAsString(result) must include(Messages("err.summary"))

        }
      }
    }
  }
}
