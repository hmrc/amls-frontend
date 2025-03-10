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

package controllers.applicationstatus

import controllers.actions.SuccessfulAuthAction
import generators.submission.SubscriptionResponseGenerator
import models.ResponseType.SubscriptionResponseType
import models.{FeeResponse, ResponseType}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.mvc.{AnyContentAsEmpty, Request, Result}
import play.api.test.Helpers._
import utils.{AmlsSpec, AuthorisedFixture, DependencyMocks, FeeHelper}
import views.html.applicationstatus.HowToPayView

import java.time.LocalDateTime
import scala.concurrent.Future

class HowToPayControllerSpec extends AmlsSpec with SubscriptionResponseGenerator {

  trait Fixture extends AuthorisedFixture with DependencyMocks {
    self =>

    val request: Request[AnyContentAsEmpty.type] = addToken(authRequest)
    lazy val howToPay: HowToPayView              = app.injector.instanceOf[HowToPayView]
    val controller                               = new HowToPayController(
      authAction = SuccessfulAuthAction,
      feeHelper = mock[FeeHelper],
      cc = mockMcc,
      ds = commonDependencies,
      view = howToPay
    )
  }

  "HowToPayController" must {
    "get" must {
      "redirect to HowToPay" when {
        val amlsRegistrationNumber = "amlsRefNumber"

        def feeResponse(responseType: ResponseType) = FeeResponse(
          responseType = responseType,
          amlsReferenceNumber = amlsRegistrationNumber,
          registrationFee = 100,
          fpFee = None,
          approvalCheckFee = None,
          premiseFee = 0,
          totalFees = 200,
          paymentReference = Some(paymentReferenceNumber),
          difference = Some(115),
          createdAt = LocalDateTime.now
        )

        "There is a payment reference number" in new Fixture {
          when {
            controller.feeHelper.retrieveFeeResponse(any(), any[(String, String)](), any(), any())(any(), any())
          } thenReturn Future.successful(Some(feeResponse(SubscriptionResponseType)))

          val result: Future[Result] = controller.get()(request)
          status(result) must be(OK)

          val doc: Document = Jsoup.parse(contentAsString(result))
          doc.getElementById("payment-ref").html must include(paymentReferenceNumber)
        }

        "There is no payment reference number" in new Fixture {

          when {
            controller.feeHelper.retrieveFeeResponse(any(), any[(String, String)](), any(), any())(any(), any())
          } thenReturn Future.successful(None)

          val result: Future[Result] = controller.get()(request)
          status(result) must be(OK)

          val doc: Document = Jsoup.parse(contentAsString(result))
          doc.getElementById("find-email-no-reference").html must include(messages("howtopay.para.3.link"))
        }

        "There is an empty payment reference number" in new Fixture {

          when {
            controller.feeHelper.retrieveFeeResponse(any(), any[(String, String)](), any(), any())(any(), any())
          } thenReturn Future.successful(
            Some(feeResponse(SubscriptionResponseType).copy(paymentReference = Some("    ")))
          )

          val result: Future[Result] = controller.get()(request)
          status(result) must be(OK)

          val doc: Document = Jsoup.parse(contentAsString(result))
          doc.getElementById("find-email-no-reference").html must include(messages("howtopay.para.3.link"))
        }
      }
    }
  }
}
