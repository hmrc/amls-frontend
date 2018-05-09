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

import cats.data.OptionT
import cats.implicits._
import connectors.DataCacheConnector
import generators.businessmatching.BusinessMatchingGenerator
import models.businessmatching._
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import utils.{AuthorisedFixture, DependencyMocks, AmlsSpec}
import play.api.i18n.Messages
import play.api.test.Helpers._
import services.businessmatching.BusinessMatchingService
import uk.gov.hmrc.http.cache.client.CacheMap

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class BusinessAppliedForPSRNumberControllerSpec extends AmlsSpec
  with MockitoSugar
  with ScalaFutures
  with BusinessMatchingGenerator {

  trait Fixture extends AuthorisedFixture with DependencyMocks {
    self => val request = addToken(authRequest)

    val controller = new BusinessAppliedForPSRNumberController {
      override val dataCacheConnector = mockCacheConnector
      override val authConnector = self.authConnector
      override val businessMatchingService = mock[BusinessMatchingService]
    }

    val businessMatching = businessMatchingGen.sample.get

    when {
      controller.businessMatchingService.getModel(any(), any(), any())
    } thenReturn OptionT.some[Future, BusinessMatching](businessMatching)

    when {
      controller.businessMatchingService.updateModel(any())(any(), any(), any())
    } thenReturn OptionT.some[Future, CacheMap](mockCacheMap)
  }

  val emptyCache = CacheMap("", Map.empty)

  "BusinessAppliedForPSRNumberController" when {

    "get is called" must {
      "on get display the page 'business applied for a Payment Systems Regulator (PSR) registration number?'" in new Fixture {
        when {
          controller.businessMatchingService.getModel(any(), any(), any())
        } thenReturn OptionT.none[Future, BusinessMatching]

        val result = controller.get()(request)
        status(result) must be(OK)
      }

      "on get display the page 'business applied for a Payment Systems Regulator (PSR) registration number?' with pre populated data" in new Fixture {
        override val businessMatching = businessMatchingWithPsrGen.sample.get

        var psr = businessMatching.businessAppliedForPSRNumber match {
          case Some(BusinessAppliedForPSRNumberYes(num)) => num
          case _ => "invalid"
        }

        when {
          controller.businessMatchingService.getModel(any(), any(), any())
        } thenReturn OptionT.some[Future, BusinessMatching](businessMatching)

        val result = controller.get()(request)
        status(result) must be(OK)

        val document = Jsoup.parse(contentAsString(result))
        document.select("input[value=true]").hasAttr("checked") must be(true)
        document.select("input[name=regNumber]").`val` mustBe psr
      }
    }

    "post is called" must {
      "respond with SEE_OTHER and redirect to the SummaryController when Yes is selected and edit is false" in new Fixture {
        val newRequest = request.withFormUrlEncodedBody(
          "appliedFor" -> "true",
          "regNumber" -> "123789"
        )

        val result = controller.post(false)(newRequest)

        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some(routes.SummaryController.get().url))
      }

      "respond with SEE_OTHER and redirect to the SummaryController when Yes is selected and edit is true" in new Fixture {

        val newRequest = request.withFormUrlEncodedBody(
          "appliedFor" -> "true",
          "regNumber" -> "123789"
        )

        when(controller.dataCacheConnector.fetch[BusinessMatching](any())
          (any(), any(), any())).thenReturn(Future.successful(None))

        when(controller.dataCacheConnector.save[BusinessMatching](any(), any())
          (any(), any(), any())).thenReturn(Future.successful(emptyCache))

        val result = controller.post(true)(newRequest)
        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some(routes.SummaryController.get().url))
      }

      "remove data from the cache and redirect to the CannotContinueWithTheApplicationController when No is selected" in new Fixture {

        val newRequest = request.withFormUrlEncodedBody(
          "appliedFor" -> "false"
        )

        val result = controller.post(true)(newRequest)

        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some(routes.NoPsrController.get().url))
      }

      "respond with BAD_REQUEST when given invalid data" in new Fixture {
        val newRequest = request.withFormUrlEncodedBody(
          "appliedFor" -> "true",
          "regNumber" -> ""
        )

        val result = controller.post()(newRequest)
        status(result) must be(BAD_REQUEST)

        val document: Document = Jsoup.parse(contentAsString(result))
        document.select("span").html() must include(Messages("error.invalid.msb.psr.number"))
      }

      "return 500" when {
        "'Yes' was given but there is no model" in new Fixture {
          val newRequest = request.withFormUrlEncodedBody(
            "appliedFor" -> "true",
            "regNumber" -> "123456"
          )

          when {
            controller.businessMatchingService.getModel(any(), any(), any())
          } thenReturn OptionT.none[Future, BusinessMatching]

          val result = controller.post()(newRequest)
          status(result) mustBe INTERNAL_SERVER_ERROR
        }
      }
    }
  }
}
