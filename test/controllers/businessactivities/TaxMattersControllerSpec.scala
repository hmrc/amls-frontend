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
import forms.businessactivities.TaxMattersFormProvider
import models.businessactivities._
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.Json
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Injecting}
import services.cache.Cache
import utils.AmlsSpec
import views.html.businessactivities.TaxMattersView

import scala.concurrent.Future

class TaxMattersControllerSpec extends AmlsSpec with MockitoSugar with ScalaFutures with Injecting {

  trait Fixture {
    self =>
    val request    = addToken(authRequest)
    lazy val view  = inject[TaxMattersView]
    val controller = new TaxMattersController(
      dataCacheConnector = mock[DataCacheConnector],
      SuccessfulAuthAction,
      ds = commonDependencies,
      cc = mockMcc,
      formProvider = inject[TaxMattersFormProvider],
      view = view,
      error = errorView
    )
  }

  "TaxMattersController" when {

    "get is called" must {
      "display the 'Manage Your Tax Affairs?' page with an empty form" in new Fixture {

        when(controller.dataCacheConnector.fetch[BusinessActivities](any(), any())(any()))
          .thenReturn(
            Future.successful(
              Some(
                BusinessActivities(
                  whoIsYourAccountant = Some(
                    WhoIsYourAccountant(
                      Some(WhoIsYourAccountantName("Accountant name", accountantsTradingName = None)),
                      Some(WhoIsYourAccountantIsUk(true)),
                      Some(UkAccountantsAddress("", None, None, None, ""))
                    )
                  )
                )
              )
            )
          )

        val result = controller.get()(request)
        status(result) must be(OK)

        val page = Jsoup.parse(contentAsString(result))

        page.getElementById("manageYourTaxAffairs").hasAttr("checked")   must be(false)
        page.getElementById("manageYourTaxAffairs-2").hasAttr("checked") must be(false)
      }

      "display the 'Manage Your Tax Affairs?' page with pre populated data if found in cache" in new Fixture {

        when(controller.dataCacheConnector.fetch[BusinessActivities](any(), any())(any()))
          .thenReturn(
            Future.successful(
              Some(
                BusinessActivities(
                  taxMatters = Some(TaxMatters(true)),
                  whoIsYourAccountant = Some(
                    WhoIsYourAccountant(
                      Some(WhoIsYourAccountantName("Accountant name", accountantsTradingName = None)),
                      Some(WhoIsYourAccountantIsUk(true)),
                      Some(UkAccountantsAddress("", None, None, None, ""))
                    )
                  )
                )
              )
            )
          )

        val result = controller.get()(request)
        status(result) must be(OK)

        val page = Jsoup.parse(contentAsString(result))

        page.getElementById("manageYourTaxAffairs").hasAttr("checked")   must be(true)
        page.getElementById("manageYourTaxAffairs-2").hasAttr("checked") must be(false)
      }
    }

    "post is called" must {
      "redirect to Check Your Answers on post with valid data" in new Fixture {

        val newRequest = FakeRequest(POST, routes.TaxMattersController.post(true).url).withFormUrlEncodedBody(
          "manageYourTaxAffairs" -> "true"
        )

        when(controller.dataCacheConnector.fetch[BusinessActivities](any(), any())(any()))
          .thenReturn(Future.successful(None))

        when(controller.dataCacheConnector.save[BusinessActivities](any(), any(), any())(any()))
          .thenReturn(Future.successful(Cache(BusinessActivities.key, Map("" -> Json.obj()))))

        val result = controller.post()(newRequest)
        status(result)           must be(SEE_OTHER)
        redirectLocation(result) must be(Some(routes.SummaryController.get.url))
      }

      "respond with Bad Request on post with invalid data" in new Fixture {
        val accountantsName = "Accountant name"
        when(controller.dataCacheConnector.fetch[BusinessActivities](any(), any())(any()))
          .thenReturn(
            Future.successful(
              Some(
                BusinessActivities(
                  whoIsYourAccountant =
                    Some(WhoIsYourAccountant(Some(WhoIsYourAccountantName(accountantsName, None)), None, None))
                )
              )
            )
          )

        val newRequest = FakeRequest(POST, routes.TaxMattersController.post(false).url).withFormUrlEncodedBody(
          "manageYourTaxAffairs" -> "grrrrr"
        )

        val result = controller.post()(newRequest)
        status(result) must be(BAD_REQUEST)
      }

      "redirect to Check Your Answers on post with valid data in edit mode" in new Fixture {

        val newRequest = FakeRequest(POST, routes.TaxMattersController.post(true).url).withFormUrlEncodedBody(
          "manageYourTaxAffairs" -> "true"
        )

        when(controller.dataCacheConnector.fetch[BusinessActivities](any(), any())(any()))
          .thenReturn(Future.successful(None))

        when(controller.dataCacheConnector.save[BusinessActivities](any(), any(), any())(any()))
          .thenReturn(Future.successful(Cache(BusinessActivities.key, Map("" -> Json.obj()))))

        val result = controller.post(true)(newRequest)
        status(result)           must be(SEE_OTHER)
        redirectLocation(result) must be(Some(routes.SummaryController.get.url))
      }
    }
  }
}
