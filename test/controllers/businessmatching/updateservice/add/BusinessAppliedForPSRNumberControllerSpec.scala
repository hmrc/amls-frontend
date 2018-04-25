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

package controllers.businessmatching.updateservice.add

import cats.data.OptionT
import cats.implicits._
import controllers.businessmatching.updateservice.UpdateServiceHelper
import generators.businessmatching.BusinessMatchingGenerator
import models.businessmatching._
import models.flowmanagement.{AddServiceFlowModel, BusinessAppliedForPSRNumberPageId}
import models.status.SubmissionDecisionApproved
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import play.api.i18n.Messages
import play.api.test.Helpers._
import services.businessmatching.BusinessMatchingService
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.{AuthorisedFixture, DependencyMocks, GenericTestHelper}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class BusinessAppliedForPSRNumberControllerSpec extends GenericTestHelper
  with MockitoSugar
  with ScalaFutures
  with BusinessMatchingGenerator {

  val emptyCache = CacheMap("", Map.empty)

  trait Fixture extends AuthorisedFixture with DependencyMocks {
    self =>

    val request = addToken(authRequest)

    val mockBusinessMatchingService = mock[BusinessMatchingService]
    val mockUpdateServiceHelper = mock[UpdateServiceHelper]

    val controller = new BusinessAppliedForPSRNumberController(
      authConnector = self.authConnector,
      dataCacheConnector = mockCacheConnector,
      statusService = mockStatusService,
      businessMatchingService = mockBusinessMatchingService,
      helper = mockUpdateServiceHelper,
      router = createRouter[AddServiceFlowModel]
    )

    mockCacheUpdate[AddServiceFlowModel](Some(AddServiceFlowModel.key), AddServiceFlowModel())
    mockCacheFetch(Some(AddServiceFlowModel(Some(HighValueDealing))))
    mockApplicationStatus(SubmissionDecisionApproved)

    val businessMatching = businessMatchingGen.sample.get

    when {
      controller.businessMatchingService.getModel(any(), any(), any())
    } thenReturn OptionT.some[Future, BusinessMatching](businessMatching)

    when {
      controller.businessMatchingService.updateModel(any())(any(), any(), any())
    } thenReturn OptionT.some[Future, CacheMap](mockCacheMap)

    when(controller.dataCacheConnector.fetch[BusinessMatching](any())
      (any(), any(), any())).thenReturn(Future.successful(None))

    when(controller.dataCacheConnector.save[BusinessMatching](any(), any())
      (any(), any(), any())).thenReturn(Future.successful(emptyCache))
  }

  "BusinessAppliedForPSRNumberController" when {

    "get is called" must {
      "return OK with the business_applied_for_psr_number view" in new Fixture {

        val result = controller.get()(request)

        status(result) must be(OK)
        Jsoup.parse(contentAsString(result)).title() must include(Messages("businessmatching.updateservice.psr.number.title"))

      }

      "return OK and display business_applied_for_psr_number view with pre populated data" in new Fixture {
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
      "with a valid request and not in edit mode" must {
        "progress to the 'no psr' page" when {
          "no is selected" in new Fixture {
            val flowModel = AddServiceFlowModel(activity = Some(MoneyServiceBusiness),
              msbServices = Some(MsbServices(Set(TransmittingMoney))),
              businessAppliedForPSRNumber = Some(BusinessAppliedForPSRNumberNo),
              hasChanged = true)

            mockCacheUpdate[AddServiceFlowModel](Some(AddServiceFlowModel.key), flowModel)

            val newRequest = request.withFormUrlEncodedBody(
              "appliedFor" -> "false"
            )

            val result = controller.post(false)(newRequest)

            status(result) must be(SEE_OTHER)
            controller.router.verify(BusinessAppliedForPSRNumberPageId, flowModel)

          }
        }

        "progress to the 'fit and proper' page" when {
          "yes is selected and a PSR is supplied" in new Fixture {
            mockCacheUpdate[AddServiceFlowModel](Some(AddServiceFlowModel.key),
              AddServiceFlowModel(businessAppliedForPSRNumber = Some(BusinessAppliedForPSRNumberYes("123789"))))

            val newRequest = request.withFormUrlEncodedBody(
              "appliedFor" -> "true",
              "regNumber" -> "123789"
            )

            val result = controller.post(false)(newRequest)

            status(result) must be(SEE_OTHER)
            controller.router.verify(BusinessAppliedForPSRNumberPageId,
              AddServiceFlowModel(businessAppliedForPSRNumber = Some(BusinessAppliedForPSRNumberYes("123789"))))

          }
        }
      }

      "with a valid request and in edit mode" must {
        "progress to the 'no psr' page" when {
          "no is selected" in new Fixture {
            mockCacheUpdate[AddServiceFlowModel](Some(AddServiceFlowModel.key),
              AddServiceFlowModel(businessAppliedForPSRNumber = Some(BusinessAppliedForPSRNumberNo)))

            val newRequest = request.withFormUrlEncodedBody(
              "appliedFor" -> "false"
            )

            val result = controller.post(true)(newRequest)

            status(result) must be(SEE_OTHER)
            controller.router.verify(BusinessAppliedForPSRNumberPageId,
              AddServiceFlowModel(businessAppliedForPSRNumber = Some(BusinessAppliedForPSRNumberNo)), true)

          }
        }

        "progress to the 'update services summary' page" when {
          "yes is selected and a PSR is supplied" in new Fixture {
            mockCacheUpdate[AddServiceFlowModel](Some(AddServiceFlowModel.key),
              AddServiceFlowModel(businessAppliedForPSRNumber = Some(BusinessAppliedForPSRNumberYes("123789"))))

            val newRequest = request.withFormUrlEncodedBody(
              "appliedFor" -> "true",
              "regNumber" -> "123789"
            )

            val result = controller.post(true)(newRequest)

            status(result) must be(SEE_OTHER)
            controller.router.verify(BusinessAppliedForPSRNumberPageId,
              AddServiceFlowModel(businessAppliedForPSRNumber = Some(BusinessAppliedForPSRNumberYes("123789"))), true)

          }
        }
      }

      "with an invalid request (missing PSR)" must {
        "return an error"  in new Fixture {
          mockCacheUpdate[AddServiceFlowModel](Some(AddServiceFlowModel.key),
            AddServiceFlowModel())
          val newRequest = request.withFormUrlEncodedBody(
            "appliedFor" -> "true",
            "regNumber" -> ""
          )

          val result = controller.post()(newRequest)
          status(result) must be(BAD_REQUEST)

          val document: Document = Jsoup.parse(contentAsString(result))
          document.select("span").html() must include(Messages("error.invalid.msb.psr.number"))
        }
      }
    }
  }
}







