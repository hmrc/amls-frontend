/*
 * Copyright 2021 HM Revenue & Customs
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
import controllers.actions.SuccessfulAuthAction
import controllers.businessmatching.updateservice.ChangeSubSectorHelper
import generators.businessmatching.BusinessMatchingGenerator
import models.businessmatching._
import models.businessmatching.updateservice.ServiceChangeRegister
import models.flowmanagement.{ChangeSubSectorFlowModel, PsrNumberPageId}
import models.moneyservicebusiness.MoneyServiceBusiness
import models.status.NotCompleted
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import play.api.i18n.Messages
import play.api.test.Helpers._
import services.businessmatching.BusinessMatchingService
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.{AmlsSpec, DependencyMocks}
import views.html.businessmatching.psr_number


import scala.concurrent.Future

class PSRNumberControllerSpec extends AmlsSpec
  with MockitoSugar
  with ScalaFutures
  with BusinessMatchingGenerator {

  trait Fixture extends DependencyMocks { self =>

    val request = addToken(authRequest)
    lazy val view = app.injector.instanceOf[psr_number]
    val controller = new PSRNumberController(
      SuccessfulAuthAction,
      ds = commonDependencies,
      mockCacheConnector,
      mockStatusService,
      mock[BusinessMatchingService],
      createRouter2[ChangeSubSectorFlowModel],
      mock[ChangeSubSectorHelper],
      cc = mockMcc,
      psr_number = view
    )

    when {
      mockStatusService.isPreSubmission(Some(any()), any(), any())(any(), any())
    } thenReturn Future.successful(true)

    when {
      mockStatusService.isPending(any())
    } thenReturn false

    when {
      mockStatusService.isPreSubmission(any())
    } thenReturn true

    mockApplicationStatus(NotCompleted)

    val businessMatching = businessMatchingGen.sample.get

    mockCacheFetch[ServiceChangeRegister](None, None)

    val emptyCache = CacheMap("", Map.empty)
  }

  "BusinessAppliedForPSRNumberController" when {

    "get is called" must {
      "on get display the page 'business applied for a Payment Systems Regulator (PSR) registration number?'" in new Fixture {
        val model = BusinessMatching(
          businessAppliedForPSRNumber = Some(BusinessAppliedForPSRNumberYes("1"))
        )

        when {
          controller.businessMatchingService.getModel(any())(any())
        } thenReturn OptionT.some[Future, BusinessMatching](model)

        val result = controller.get()(request)
        status(result) mustBe OK
      }

      "on get display the page 'business applied for a Payment Systems Regulator (PSR) registration number?' with pre populated data" in new Fixture {
        override val businessMatching = businessMatchingWithPsrGen.sample.get

        val psr = businessMatching.businessAppliedForPSRNumber match {
          case Some(BusinessAppliedForPSRNumberYes(num)) => num
          case _ => "invalid"
        }

        when {
          controller.businessMatchingService.getModel(any())(any())
        } thenReturn OptionT.some[Future, BusinessMatching](businessMatching)

        val result = controller.get()(request)
        status(result) mustBe OK

        val document = Jsoup.parse(contentAsString(result))
        document.select("input[value=true]").hasAttr("checked") must be(true)
        document.select("input[name=regNumber]").`val` mustBe psr
      }
    }

    "post is called" must {
      "respond with SEE_OTHER and redirect to the SummaryController when Yes is selected and edit is false" in new Fixture {
        val flowModel = ChangeSubSectorFlowModel(Some(Set(TransmittingMoney)))

        when {
          controller.helper.getOrCreateFlowModel(any())(any(), any())
        } thenReturn Future.successful(flowModel)

        when {
          controller.helper.updateSubSectors(any(), any())(any(), any())
        } thenReturn Future.successful((mock[MoneyServiceBusiness], mock[BusinessMatching], Seq.empty))

        val newRequest = requestWithUrlEncodedBody(
          "appliedFor" -> "true",
          "regNumber" -> "123789"
        )

        mockCacheUpdate[ChangeSubSectorFlowModel](Some(ChangeSubSectorFlowModel.key), ChangeSubSectorFlowModel(Some(Set(TransmittingMoney))))

        val result = controller.post()(newRequest)

        status(result) mustBe SEE_OTHER

        controller.router.verify("internalId", PsrNumberPageId, ChangeSubSectorFlowModel(
            Some(Set(TransmittingMoney)),
            Some(BusinessAppliedForPSRNumberYes("123789"))))
      }

      "redirect when No is selected" in new Fixture {
        val flowModel = ChangeSubSectorFlowModel(Some(Set(TransmittingMoney)))

        when {
          controller.helper.getOrCreateFlowModel(any())(any(), any())
        } thenReturn Future.successful(flowModel)

        mockCacheUpdate[ChangeSubSectorFlowModel](Some(ChangeSubSectorFlowModel.key), ChangeSubSectorFlowModel.empty)

        val newRequest = requestWithUrlEncodedBody(
          "appliedFor" -> "false"
        )

        val result = controller.post(true)(newRequest)

        status(result) mustBe SEE_OTHER
        controller.router.verify("internalId", PsrNumberPageId, ChangeSubSectorFlowModel(Some(Set(TransmittingMoney)), Some(BusinessAppliedForPSRNumberNo)), edit = true)
      }

      "respond with BAD_REQUEST when given invalid data" in new Fixture {
        val newRequest = requestWithUrlEncodedBody(
          "appliedFor" -> "true",
          "regNumber" -> ""
        )

        when {
          controller.businessMatchingService.getModel(any())(any())
        } thenReturn OptionT.some[Future, BusinessMatching](businessMatching)

        val result = controller.post()(newRequest)
        status(result) mustBe BAD_REQUEST

        val document: Document = Jsoup.parse(contentAsString(result))
        document.select("span").html() must include(Messages("error.invalid.msb.psr.number"))
      }
    }
  }
}
