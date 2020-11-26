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

package controllers.asp

import controllers.actions.SuccessfulAuthAction
import models.asp.{Asp, OtherBusinessTaxMattersYes}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import play.api.i18n.Messages
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.{AmlsSpec, DependencyMocks}
import views.html.asp.other_business_tax_matters

import scala.concurrent.Future

class OtherBusinessTaxMattersControllerSpec extends AmlsSpec with MockitoSugar with ScalaFutures {

  val emptyCache = CacheMap("", Map.empty)

  trait Fixture extends DependencyMocks {
    self =>
    val request = addToken(authRequest)
    lazy val view = app.injector.instanceOf[other_business_tax_matters]
    mockCacheFetch[Asp](None)

    mockCacheSave[Asp]

    val controller = new OtherBusinessTaxMattersController(
      mockCacheConnector,
      authAction = SuccessfulAuthAction,
      ds = commonDependencies,
      cc = mockMcc,
      other_business_tax_matters = view
    )

    val newRequest = requestWithUrlEncodedBody(
      "otherBusinessTaxMatters" -> "true"
    )
  }


  "OtherBusinessTaxMattersController" when {
    "get is called" must {
      "display the are you registered with HMRC to handle other business's tax matters page" in new Fixture {
        val result = controller.get()(request)
        status(result) must be(OK)
        contentAsString(result) must include(Messages("asp.other.business.tax.matters.title"))
      }

      "display the the Does your business use the services of another Trust or Company Service Provider page with pre populated data" in new Fixture {
        when(controller.dataCacheConnector.fetch[Asp](any(), any())
          (any(), any())).thenReturn(Future.successful(Some(Asp(otherBusinessTaxMatters = Some(OtherBusinessTaxMattersYes)))))

        val result = controller.get()(request)
        status(result) must be(OK)

        val document = Jsoup.parse(contentAsString(result))
        document.getElementById("otherBusinessTaxMatters-true").hasAttr("checked") must be(true)
      }
    }

    "post is called" must {
      "on post with valid data" in new Fixture {
        val result = controller.post()(newRequest)

        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some(routes.SummaryController.get().url))
      }

      "on post with invalid boolean data" in new Fixture {
        val newRequestInvalid = requestWithUrlEncodedBody("otherBusinessTaxMatters" -> "invalidBoolean")
        val result = controller.post()(newRequestInvalid)

        status(result) must be(BAD_REQUEST)
        val document: Document = Jsoup.parse(contentAsString(result))
        document.select("span").html() must include(Messages("error.required.asp.other.business.tax.matters"))
      }

      "On post with missing boolean data" in new Fixture {
        val newRequestInvalid = requestWithUrlEncodedBody("otherBusinessTaxMatters" -> "")
        val result = controller.post()(newRequestInvalid)

        status(result) must be(BAD_REQUEST)

        val document: Document = Jsoup.parse(contentAsString(result))
        document.select("span").html() must include(Messages("error.required.asp.other.business.tax.matters"))
      }


      "on post with valid data in edit mode" in new Fixture {
        val result = controller.post(true)(newRequest)

        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some(routes.SummaryController.get().url))
      }
    }
  }

}
