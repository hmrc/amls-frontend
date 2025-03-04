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

package controllers.asp

import controllers.actions.SuccessfulAuthAction
import forms.asp.OtherBusinessTaxMattersFormProvider
import models.asp.{Asp, OtherBusinessTaxMattersYes}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc.{AnyContentAsEmpty, AnyContentAsFormUrlEncoded, Request, Result}
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Injecting}
import services.cache.Cache
import utils.{AmlsSpec, DependencyMocks}
import views.html.asp.OtherBusinessTaxMattersView

import scala.concurrent.Future

class OtherBusinessTaxMattersControllerSpec extends AmlsSpec with MockitoSugar with ScalaFutures with Injecting {

  val emptyCache: Cache = Cache.empty

  trait Fixture extends DependencyMocks {
    self =>
    val request: Request[AnyContentAsEmpty.type] = addToken(authRequest)
    lazy val view: OtherBusinessTaxMattersView   = inject[OtherBusinessTaxMattersView]
    mockCacheFetch[Asp](None)

    mockCacheSave[Asp]

    val controller = new OtherBusinessTaxMattersController(
      mockCacheConnector,
      authAction = SuccessfulAuthAction,
      ds = commonDependencies,
      cc = mockMcc,
      formProvider = inject[OtherBusinessTaxMattersFormProvider],
      view = view
    )

    val newRequest: FakeRequest[AnyContentAsFormUrlEncoded] =
      FakeRequest(POST, routes.OtherBusinessTaxMattersController.post().url)
        .withFormUrlEncodedBody(
          "otherBusinessTaxMatters" -> "true"
        )
  }

  "OtherBusinessTaxMattersController" when {
    "get is called" must {
      "display the are you registered with HMRC to handle other business's tax matters page" in new Fixture {
        val result: Future[Result] = controller.get()(request)
        status(result)          must be(OK)
        contentAsString(result) must include(messages("asp.other.business.tax.matters.title"))
      }

      "display the the Does your business use the services of another Trust or Company Service Provider page with pre populated data" in new Fixture {
        when(controller.dataCacheConnector.fetch[Asp](any(), any())(any()))
          .thenReturn(Future.successful(Some(Asp(otherBusinessTaxMatters = Some(OtherBusinessTaxMattersYes)))))

        val result: Future[Result] = controller.get()(request)
        status(result) must be(OK)

        val document: Document = Jsoup.parse(contentAsString(result))
        document.getElementById("otherBusinessTaxMatters").hasAttr("checked") must be(true)
      }
    }

    "post is called" must {
      "on post with valid data" in new Fixture {
        val result: Future[Result] = controller.post()(newRequest)

        status(result)           must be(SEE_OTHER)
        redirectLocation(result) must be(Some(routes.SummaryController.get.url))
      }

      "on post with invalid boolean data" in new Fixture {
        val newRequestInvalid: FakeRequest[AnyContentAsFormUrlEncoded] =
          FakeRequest(POST, routes.OtherBusinessTaxMattersController.post().url)
            .withFormUrlEncodedBody("otherBusinessTaxMatters" -> "invalidBoolean")
        val result: Future[Result]                                     = controller.post()(newRequestInvalid)

        status(result) must be(BAD_REQUEST)
        val document: Document = Jsoup.parse(contentAsString(result))
        document.getElementsByClass("govuk-error-summary").text() must include(
          messages("error.required.asp.other.business.tax.matters")
        )
      }

      "On post with missing boolean data" in new Fixture {
        val newRequestInvalid: FakeRequest[AnyContentAsFormUrlEncoded] =
          FakeRequest(POST, routes.OtherBusinessTaxMattersController.post().url)
            .withFormUrlEncodedBody("otherBusinessTaxMatters" -> "")
        val result: Future[Result]                                     = controller.post()(newRequestInvalid)

        status(result) must be(BAD_REQUEST)

        val document: Document = Jsoup.parse(contentAsString(result))
        document.getElementsByClass("govuk-error-summary").text() must include(
          messages("error.required.asp.other.business.tax.matters")
        )
      }

      "on post with valid data in edit mode" in new Fixture {
        val editRequest: FakeRequest[AnyContentAsFormUrlEncoded] =
          FakeRequest(POST, routes.OtherBusinessTaxMattersController.post(true).url)
            .withFormUrlEncodedBody(
              "otherBusinessTaxMatters" -> "true"
            )

        val result: Future[Result] = controller.post(true)(editRequest)

        status(result)           must be(SEE_OTHER)
        redirectLocation(result) must be(Some(routes.SummaryController.get.url))
      }
    }
  }

}
