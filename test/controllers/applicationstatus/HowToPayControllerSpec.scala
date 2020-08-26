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

package controllers.applicationstatus

import controllers.actions.SuccessfulAuthAction
import generators.submission.SubscriptionResponseGenerator
import models.{FeeResponse, ResponseType}
import models.ResponseType.SubscriptionResponseType
import org.joda.time.DateTime
import org.jsoup.Jsoup
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import play.api.test.Helpers._
import utils.{AmlsSpec, AuthorisedFixture, DependencyMocks, FeeHelper}
import play.api.i18n.Messages
import views.html.applicationstatus.how_to_pay

import scala.concurrent.Future

class HowToPayControllerSpec extends AmlsSpec with SubscriptionResponseGenerator {

  trait Fixture extends AuthorisedFixture with DependencyMocks {
    self =>

    val request = addToken(authRequest)
    lazy val howToPay = app.injector.instanceOf[how_to_pay]
    val controller = new HowToPayController(
      authAction = SuccessfulAuthAction,
      feeHelper = mock[FeeHelper],
      cc = mockMcc,
      ds = commonDependencies,
      how_to_pay = howToPay
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
          createdAt = DateTime.now
        )

        "There is a payment reference number" in new Fixture {
          when {
            controller.feeHelper.retrieveFeeResponse(any(), any[(String, String)](), any(), any())(any())
          } thenReturn Future.successful(Some(feeResponse(SubscriptionResponseType)))

          val result = controller.get(request)
          status(result) must be(OK)

          val doc = Jsoup.parse(contentAsString(result))
          doc.getElementById("payment-ref").html must include(paymentReferenceNumber)
        }

        "There is no payment reference number" in new Fixture {

          val amlsRegistrationNumber = "amlsRefNumber"

          when {
            controller.feeHelper.retrieveFeeResponse(any(), any[(String, String)](), any(), any())(any())
          } thenReturn Future.successful(None)

          val result = controller.get(request)
          status(result) must be(OK)

          val doc = Jsoup.parse(contentAsString(result))
          doc.getElementById("find-email-no-reference").html must include(Messages("howtopay.para.3.link"))
        }

        "There is an empty payment reference number" in new Fixture {

          val amlsRegistrationNumber = "amlsRefNumber"

          when {
            controller.feeHelper.retrieveFeeResponse(any(), any[(String, String)](), any(), any())(any())
          } thenReturn Future.successful(Some(feeResponse(SubscriptionResponseType).copy(paymentReference = Some("    "))))

          val result = controller.get(request)
          status(result) must be(OK)

          val doc = Jsoup.parse(contentAsString(result))
          doc.getElementById("find-email-no-reference").html must include(Messages("howtopay.para.3.link"))
        }
      }
    }
  }
}
