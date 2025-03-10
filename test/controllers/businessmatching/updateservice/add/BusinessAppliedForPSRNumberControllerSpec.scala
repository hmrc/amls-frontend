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

package controllers.businessmatching.updateservice.add

import controllers.actions.SuccessfulAuthAction
import controllers.businessmatching.updateservice.AddBusinessTypeHelper
import forms.businessmatching.PSRNumberFormProvider
import generators.businessmatching.BusinessMatchingGenerator
import models.businessmatching._
import models.businessmatching.BusinessActivity.{HighValueDealing, MoneyServiceBusiness}
import models.businessmatching.BusinessMatchingMsbService.TransmittingMoney
import models.flowmanagement.{AddBusinessTypeFlowModel, PsrNumberPageId}
import models.status.SubmissionDecisionApproved
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.{FakeRequest, Injecting}
import play.api.test.Helpers._
import services.businessmatching.BusinessMatchingService
import services.cache.Cache
import utils.{AmlsSpec, DependencyMocks}
import views.html.businessmatching.updateservice.add.BusinessAppliedForPSRNumberView

class BusinessAppliedForPSRNumberControllerSpec
    extends AmlsSpec
    with MockitoSugar
    with ScalaFutures
    with BusinessMatchingGenerator
    with Injecting {

  val emptyCache = Cache.empty

  trait Fixture extends DependencyMocks {
    self =>

    val request = addToken(authRequest)

    val mockBusinessMatchingService = mock[BusinessMatchingService]
    val mockUpdateServiceHelper     = mock[AddBusinessTypeHelper]

    lazy val view  = inject[BusinessAppliedForPSRNumberView]
    val controller = new BusinessAppliedForPSRNumberController(
      authAction = SuccessfulAuthAction,
      ds = commonDependencies,
      dataCacheConnector = mockCacheConnector,
      router = createRouter[AddBusinessTypeFlowModel],
      cc = mockMcc,
      formProvider = inject[PSRNumberFormProvider],
      view = view
    )

    mockCacheUpdate[AddBusinessTypeFlowModel](Some(AddBusinessTypeFlowModel.key), AddBusinessTypeFlowModel())
    mockCacheFetch(Some(AddBusinessTypeFlowModel(Some(HighValueDealing))))
    mockApplicationStatus(SubmissionDecisionApproved)

    val businessMatching = businessMatchingGen.sample.get
    mockCacheSave[BusinessMatching]
  }

  "BusinessAppliedForPSRNumberController" when {

    "get is called" must {
      "return OK with the psr_number view if there is MSB and TM defined" in new Fixture {
        mockCacheFetch(
          Some(
            AddBusinessTypeFlowModel(
              activity = Some(MoneyServiceBusiness),
              subSectors = Some(BusinessMatchingMsbServices(Set(TransmittingMoney)))
            )
          )
        )

        val result = controller.get()(request)

        status(result)                               must be(OK)
        Jsoup.parse(contentAsString(result)).title() must include(
          messages("businessmatching.updateservice.psr.number.title")
        )
      }

      "return OK and display psr_number view with pre populated data if there is MSB and TM defined" in new Fixture {
        override val businessMatching = businessMatchingWithPsrGen.sample.get

        mockCacheFetch(
          Some(
            AddBusinessTypeFlowModel(
              activity = Some(MoneyServiceBusiness),
              businessAppliedForPSRNumber = Some(BusinessAppliedForPSRNumberYes("123456")),
              subSectors = Some(BusinessMatchingMsbServices(Set(TransmittingMoney)))
            )
          )
        )

        val result = controller.get()(request)
        status(result) must be(OK)

        val document = Jsoup.parse(contentAsString(result))
        document.select("input[value=true]").hasAttr("checked") must be(true)
        document.select("input[name=regNumber]").`val` mustBe "123456"
      }

      "redirect to RegistrationProgressController if there is no MSB with TM defined" in new Fixture {
        mockCacheFetch(
          Some(
            AddBusinessTypeFlowModel(
              activity = Some(HighValueDealing),
              subSectors = Some(BusinessMatchingMsbServices(Set(TransmittingMoney)))
            )
          )
        )

        val result = controller.get()(request)

        status(result) must be(SEE_OTHER)
        redirectLocation(result) mustBe Some(controllers.routes.RegistrationProgressController.get().url)
      }

      "redirect to RegistrationProgressController if there is MSB with no TM defined" in new Fixture {
        mockCacheFetch(Some(AddBusinessTypeFlowModel(activity = Some(MoneyServiceBusiness))))

        val result = controller.get()(request)

        status(result) must be(SEE_OTHER)
        redirectLocation(result) mustBe Some(controllers.routes.RegistrationProgressController.get().url)
      }
    }

    "post is called" must {
      "with a valid request and not in edit mode" must {
        "progress to the 'no psr' page" when {
          "no is selected" in new Fixture {
            val flowModel = AddBusinessTypeFlowModel(
              activity = Some(MoneyServiceBusiness),
              subSectors = Some(BusinessMatchingMsbServices(Set(TransmittingMoney))),
              businessAppliedForPSRNumber = Some(BusinessAppliedForPSRNumberNo),
              hasChanged = true
            )

            mockCacheUpdate[AddBusinessTypeFlowModel](Some(AddBusinessTypeFlowModel.key), flowModel)

            val newRequest = FakeRequest(POST, routes.BusinessAppliedForPSRNumberController.post().url)
              .withFormUrlEncodedBody(
                "appliedFor" -> "false"
              )

            val result = controller.post(false)(newRequest)

            status(result) must be(SEE_OTHER)
            controller.router.verify("internalId", PsrNumberPageId, flowModel)

          }
        }

        "progress to the 'fit and proper' page" when {
          "yes is selected and a PSR is supplied" in new Fixture {
            mockCacheUpdate[AddBusinessTypeFlowModel](
              Some(AddBusinessTypeFlowModel.key),
              AddBusinessTypeFlowModel(businessAppliedForPSRNumber = Some(BusinessAppliedForPSRNumberYes("123789")))
            )

            val newRequest = FakeRequest(POST, routes.BusinessAppliedForPSRNumberController.post().url)
              .withFormUrlEncodedBody(
                "appliedFor" -> "true",
                "regNumber"  -> "123789"
              )

            val result = controller.post(false)(newRequest)

            status(result) must be(SEE_OTHER)
            controller.router.verify(
              "internalId",
              PsrNumberPageId,
              AddBusinessTypeFlowModel(businessAppliedForPSRNumber = Some(BusinessAppliedForPSRNumberYes("123789")))
            )

          }
        }
      }

      "with a valid request and in edit mode" must {
        "progress to the 'no psr' page" when {
          "no is selected" in new Fixture {
            mockCacheUpdate[AddBusinessTypeFlowModel](
              Some(AddBusinessTypeFlowModel.key),
              AddBusinessTypeFlowModel(businessAppliedForPSRNumber = Some(BusinessAppliedForPSRNumberNo))
            )

            val newRequest = FakeRequest(POST, routes.BusinessAppliedForPSRNumberController.post(true).url)
              .withFormUrlEncodedBody(
                "appliedFor" -> "false"
              )

            val result = controller.post(true)(newRequest)

            status(result) must be(SEE_OTHER)
            controller.router.verify(
              "internalId",
              PsrNumberPageId,
              AddBusinessTypeFlowModel(businessAppliedForPSRNumber = Some(BusinessAppliedForPSRNumberNo)),
              true
            )

          }
        }

        "progress to the 'update services summary' page" when {
          "yes is selected and a PSR is supplied" in new Fixture {
            mockCacheUpdate[AddBusinessTypeFlowModel](
              Some(AddBusinessTypeFlowModel.key),
              AddBusinessTypeFlowModel(businessAppliedForPSRNumber = Some(BusinessAppliedForPSRNumberYes("123789")))
            )

            val newRequest = FakeRequest(POST, routes.BusinessAppliedForPSRNumberController.post(true).url)
              .withFormUrlEncodedBody(
                "appliedFor" -> "true",
                "regNumber"  -> "123789"
              )

            val result = controller.post(true)(newRequest)

            status(result) must be(SEE_OTHER)
            controller.router.verify(
              "internalId",
              PsrNumberPageId,
              AddBusinessTypeFlowModel(businessAppliedForPSRNumber = Some(BusinessAppliedForPSRNumberYes("123789"))),
              true
            )

          }
        }
      }

      "with an invalid request (missing PSR)" must {
        "return an error" in new Fixture {
          mockCacheUpdate[AddBusinessTypeFlowModel](Some(AddBusinessTypeFlowModel.key), AddBusinessTypeFlowModel())
          val newRequest = FakeRequest(POST, routes.BusinessAppliedForPSRNumberController.post().url)
            .withFormUrlEncodedBody(
              "appliedFor" -> "true",
              "regNumber"  -> ""
            )

          val result = controller.post()(newRequest)
          status(result) must be(BAD_REQUEST)

          val document: Document = Jsoup.parse(contentAsString(result))
          document.text() must include(messages("error.invalid.msb.psr.number"))
        }
      }
    }
  }
}
