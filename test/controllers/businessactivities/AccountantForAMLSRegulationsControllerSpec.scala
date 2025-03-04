/*
 * Copyright 2024 HM Revenue & Customs
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
import forms.businessactivities.AccountantForAMLSRegulationsFormProvider
import models.businessactivities.{AccountantForAMLSRegulations, BusinessActivities, TaxMatters, WhoIsYourAccountant}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc.{AnyContentAsEmpty, AnyContentAsFormUrlEncoded, Request, Result}
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Injecting}
import services.cache.Cache
import utils.AmlsSpec
import views.html.businessactivities.AccountantForAMLSRegulationsView

import scala.concurrent.Future

class AccountantForAMLSRegulationsControllerSpec extends AmlsSpec with MockitoSugar with Injecting {

  trait Fixture {
    self =>
    val request: Request[AnyContentAsEmpty.type] = addToken(authRequest)

    lazy val view: AccountantForAMLSRegulationsView = inject[AccountantForAMLSRegulationsView]

    val controller = new AccountantForAMLSRegulationsController(
      dataCacheConnector = mock[DataCacheConnector],
      SuccessfulAuthAction,
      ds = commonDependencies,
      cc = mockMcc,
      formProvider = inject[AccountantForAMLSRegulationsFormProvider],
      view = view
    )
  }

  val emptyCache: Cache = Cache.empty

  "AccountantForAMLSRegulationsController" when {

    "get is called" must {

      "load the Accountant For AMLSRegulations page with an empty form" in new Fixture {

        when(controller.dataCacheConnector.fetch[BusinessActivities](any(), any())(any()))
          .thenReturn(Future.successful(None))

        val result: Future[Result] = controller.get()(request)
        status(result) must be(OK)

        val htmlValue: Document = Jsoup.parse(contentAsString(result))
        htmlValue.getElementById("accountantForAMLSRegulations").hasAttr("checked")   must be(false)
        htmlValue.getElementById("accountantForAMLSRegulations-2").hasAttr("checked") must be(false)
      }

      "pre-populate the form when data is already present" in new Fixture {

        val accountantForAMLSRegulations: Option[AccountantForAMLSRegulations] =
          Some(AccountantForAMLSRegulations(true))
        val activities: BusinessActivities                                     =
          BusinessActivities(accountantForAMLSRegulations = accountantForAMLSRegulations)

        when(controller.dataCacheConnector.fetch[BusinessActivities](any(), any())(any()))
          .thenReturn(Future.successful(Some(activities)))

        val result: Future[Result] = controller.get()(request)
        status(result) must be(OK)

        val htmlValue: Document = Jsoup.parse(contentAsString(result))
        htmlValue.getElementById("accountantForAMLSRegulations").hasAttr("checked")   must be(true)
        htmlValue.getElementById("accountantForAMLSRegulations-2").hasAttr("checked") must be(false)
      }
    }

    "Post is called" must {

      "respond with SEE_OTHER" when {
        "edit is true" must {
          "redirect to the WhoIsYourAccountantController when 'yes' is selected'" in new Fixture {

            val newRequest: FakeRequest[AnyContentAsFormUrlEncoded] =
              FakeRequest(POST, routes.AccountantForAMLSRegulationsController.post(false).url)
                .withFormUrlEncodedBody("accountantForAMLSRegulations" -> "true")

            when(controller.dataCacheConnector.fetch[BusinessActivities](any(), any())(any()))
              .thenReturn(Future.successful(None))

            when(controller.dataCacheConnector.save[BusinessActivities](any(), any(), any())(any()))
              .thenReturn(Future.successful(emptyCache))

            val result: Future[Result] = controller.post(true)(newRequest)
            status(result)           must be(SEE_OTHER)
            redirectLocation(result) must be(
              Some(controllers.businessactivities.routes.WhoIsYourAccountantNameController.get().url)
            )
          }

          "successfully redirect to the SummaryController on selection of Option 'No'" in new Fixture {

            val newRequest: FakeRequest[AnyContentAsFormUrlEncoded] =
              FakeRequest(POST, routes.AccountantForAMLSRegulationsController.post(false).url)
                .withFormUrlEncodedBody(
                  "accountantForAMLSRegulations" -> "false"
                )
            when(controller.dataCacheConnector.fetch[BusinessActivities](any(), any())(any()))
              .thenReturn(Future.successful(None))

            when(controller.dataCacheConnector.save[BusinessActivities](any(), any(), any())(any()))
              .thenReturn(Future.successful(emptyCache))

            val result: Future[Result] = controller.post(true)(newRequest)
            status(result)           must be(SEE_OTHER)
            redirectLocation(result) must be(Some(controllers.businessactivities.routes.SummaryController.get.url))
          }
        }

        "edit is false" must {
          "redirect to the WhoIsYourAccountantController on selection of 'Yes'" in new Fixture {
            val newRequest: FakeRequest[AnyContentAsFormUrlEncoded] =
              FakeRequest(POST, routes.AccountantForAMLSRegulationsController.post(false).url)
                .withFormUrlEncodedBody("accountantForAMLSRegulations" -> "true")

            when(controller.dataCacheConnector.fetch[BusinessActivities](any(), any())(any()))
              .thenReturn(Future.successful(None))

            when(controller.dataCacheConnector.save[BusinessActivities](any(), any(), any())(any()))
              .thenReturn(Future.successful(emptyCache))

            val result: Future[Result] = controller.post(false)(newRequest)
            status(result)           must be(SEE_OTHER)
            redirectLocation(result) must be(
              Some(controllers.businessactivities.routes.WhoIsYourAccountantNameController.get().url)
            )
          }

          "successfully redirect to the SummaryController on selection of Option 'No'" in new Fixture {
            val newRequest: FakeRequest[AnyContentAsFormUrlEncoded] =
              FakeRequest(POST, routes.AccountantForAMLSRegulationsController.post(false).url)
                .withFormUrlEncodedBody(
                  "accountantForAMLSRegulations" -> "false"
                )
            when(controller.dataCacheConnector.fetch[BusinessActivities](any(), any())(any()))
              .thenReturn(Future.successful(None))

            when(controller.dataCacheConnector.save[BusinessActivities](any(), any(), any())(any()))
              .thenReturn(Future.successful(emptyCache))

            val result: Future[Result] = controller.post(false)(newRequest)
            status(result)           must be(SEE_OTHER)
            redirectLocation(result) must be(Some(controllers.businessactivities.routes.SummaryController.get.url))
          }
        }
      }

      "respond with BAD_REQUEST" when {
        "no options are selected so that the request body is empty" in new Fixture {

          val newRequest: FakeRequest[AnyContentAsFormUrlEncoded] =
            FakeRequest(POST, routes.AccountantForAMLSRegulationsController.post(false).url)
              .withFormUrlEncodedBody("" -> "")

          when(controller.dataCacheConnector.fetch[BusinessActivities](any(), any())(any()))
            .thenReturn(Future.successful(None))

          val result: Future[Result] = controller.post()(newRequest)
          status(result) must be(BAD_REQUEST)
        }

        "given invalid json" in new Fixture {

          val newRequest: FakeRequest[AnyContentAsFormUrlEncoded] =
            FakeRequest(POST, routes.AccountantForAMLSRegulationsController.post(false).url)
              .withFormUrlEncodedBody(
                "WhatYouNeedController" -> ""
              )

          when(controller.dataCacheConnector.fetch[BusinessActivities](any(), any())(any()))
            .thenReturn(Future.successful(None))

          val result: Future[Result] = controller.post()(newRequest)
          status(result) must be(BAD_REQUEST)
        }
      }

      "remove the answers to dependant questions" when {
        "user selected 'no'" in new Fixture {
          val newRequest: FakeRequest[AnyContentAsFormUrlEncoded] =
            FakeRequest(POST, routes.AccountantForAMLSRegulationsController.post(true).url)
              .withFormUrlEncodedBody(
                "accountantForAMLSRegulations" -> "false"
              )

          val model: BusinessActivities = BusinessActivities(
            accountantForAMLSRegulations = Some(AccountantForAMLSRegulations(true)),
            whoIsYourAccountant = Some(mock[WhoIsYourAccountant]),
            taxMatters = Some(TaxMatters(true))
          )

          when(controller.dataCacheConnector.fetch[BusinessActivities](any(), any())(any()))
            .thenReturn(Future.successful(Some(model)))

          when(controller.dataCacheConnector.save[BusinessActivities](any(), any(), any())(any()))
            .thenReturn(Future.successful(emptyCache))

          val result: Future[Result] = controller.post(true)(newRequest)
          status(result) must be(SEE_OTHER)

          val captor = ArgumentCaptor.forClass(classOf[BusinessActivities])
          verify(controller.dataCacheConnector)
            .save[BusinessActivities](any(), eqTo(BusinessActivities.key), captor.capture())(any())

          captor.getValue.accountantForAMLSRegulations mustBe Some(AccountantForAMLSRegulations(false))
          captor.getValue.whoIsYourAccountant must not be defined
          captor.getValue.taxMatters          must not be defined
        }
      }
    }
  }
}
