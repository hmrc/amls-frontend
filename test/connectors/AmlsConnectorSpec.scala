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

package connectors

import config.ApplicationConfig
import generators.{AmlsReferenceNumberGenerator, PaymentGenerator}
import models.declaration.AddPerson
import models.declaration.release7.RoleWithinBusinessRelease7
import models.deregister.{DeRegisterSubscriptionRequest, DeRegisterSubscriptionResponse, DeregistrationReason}
import models.payments._
import models.registrationdetails.RegistrationDetails
import models.withdrawal._
import models.{AmendVariationRenewalResponse, _}
import org.mockito.Matchers
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.test.Helpers._
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.HttpClient

import java.time.{LocalDate, LocalDateTime}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AmlsConnectorSpec extends PlaySpec with MockitoSugar with ScalaFutures with IntegrationPatience with AmlsReferenceNumberGenerator with PaymentGenerator {

  val amlsConnector = new AmlsConnector(http = mock[HttpClient],
                                        appConfig = mock[ApplicationConfig])

  val safeId = "SAFEID"
  val accountTypeId = ("org", "id")

  val subscriptionRequest = SubscriptionRequest(
    businessMatchingSection = None,
    eabSection = None,
    tradingPremisesSection = None,
    businessDetailsSection = None,
    bankDetailsSection = None,
    aboutYouSection = None,
    businessActivitiesSection = None,
    responsiblePeopleSection = None,
    tcspSection = None,
    aspSection = None,
    msbSection = None,
    hvdSection = None,
    ampSection = None,
    supervisionSection = None
  )

  val viewResponse = ViewResponse(
    etmpFormBundleNumber = "FORMBUNDLENUMBER",
    businessMatchingSection = None,
    eabSection = None,
    tradingPremisesSection = None,
    businessDetailsSection = None,
    bankDetailsSection = Seq(None),
    aboutYouSection = AddPerson("FirstName", None, "LastName", RoleWithinBusinessRelease7(Set(models.declaration.release7.BeneficialShareholder))),
    businessActivitiesSection = None,
    responsiblePeopleSection = None,
    tcspSection = None,
    aspSection = None,
    msbSection = None,
    hvdSection = None,
    ampSection = None,
    supervisionSection = None
  )

  val subscriptionResponse = SubscriptionResponse(
    etmpFormBundleNumber = "",
    amlsRefNo = amlsRegistrationNumber,
    Some(SubscriptionFees(
      paymentReference = "",
      registrationFee = 0,
      fpFee = None,
      fpFeeRate = None,
      approvalCheckFee = None,
      approvalCheckFeeRate = None,
      premiseFee = 0,
      premiseFeeRate = None,
      totalFees = 0)
    )
  )

  val amendmentResponse = AmendVariationRenewalResponse(
    processingDate = "",
    etmpFormBundleNumber = "",
    registrationFee = 0,
    fpFee = Some(0),
    fpFeeRate = None,
    approvalCheckFee = None,
    approvalCheckFeeRate = None,
    premiseFee = 0,
    premiseFeeRate = None,
    totalFees = 0,
    paymentReference = Some(""),
    difference = Some(0)
  )

  val renewalResponse = AmendVariationRenewalResponse(
    processingDate = "",
    etmpFormBundleNumber = "",
    registrationFee = 0,
    fpFee = Some(0),
    fpFeeRate = None,
    approvalCheckFee = None,
    approvalCheckFeeRate = None,
    premiseFee = 0,
    premiseFeeRate = None,
    totalFees = 0,
    paymentReference = Some(""),
    difference = Some(0)
  )

  val readStatusResponse = ReadStatusResponse(LocalDateTime.now(), "Approved", None, None, None, None, false)

  implicit val hc = HeaderCarrier()

  "subscribe" must {

    "successfully subscribe" in {

      when {
        amlsConnector.http.POST[SubscriptionRequest, SubscriptionResponse](eqTo(s"${amlsConnector.url}/org/id/$safeId"), eqTo(subscriptionRequest), any())(any(), any(), any(), any())
      } thenReturn Future.successful(subscriptionResponse)

      whenReady(amlsConnector.subscribe(subscriptionRequest, safeId, accountTypeId)) {
        _ mustBe subscriptionResponse
      }
    }
  }

  "get status" must {

    "return correct status" in {

      when {
        amlsConnector.http.GET[ReadStatusResponse](any(), any(), any())(any(), any(), any())
      } thenReturn Future.successful(readStatusResponse)

      whenReady(amlsConnector.status(amlsRegistrationNumber, accountTypeId)) {
        _ mustBe readStatusResponse
      }
    }
  }

  "get view" must {

    "a view response" in {
      when {
        amlsConnector.http.GET[ViewResponse](any(), any(), any())(any(), any(), any())
      } thenReturn Future.successful(viewResponse)

      whenReady(amlsConnector.view(amlsRegistrationNumber, accountTypeId)) {
        _ mustBe viewResponse
      }
    }
  }

  "update" must {

    "successfully submit amendment" in {
      when {
        amlsConnector.http.POST[SubscriptionRequest, AmendVariationRenewalResponse](eqTo(s"${amlsConnector.url}/org/id/$amlsRegistrationNumber/update")
          , eqTo(subscriptionRequest), any())(any(), any(), any(), any())
      }.thenReturn(Future.successful(amendmentResponse))

      whenReady(amlsConnector.update(subscriptionRequest, amlsRegistrationNumber, accountTypeId)) {
        _ mustBe amendmentResponse
      }
    }
  }

  "variation" must {
    "successfully submit variation" in {
      when {
        amlsConnector.http.POST[SubscriptionRequest, AmendVariationRenewalResponse](eqTo(s"${amlsConnector.url}/org/id/$amlsRegistrationNumber/variation")
          , eqTo(subscriptionRequest), any())(any(), any(), any(), any())
      }.thenReturn(Future.successful(amendmentResponse))

      whenReady(amlsConnector.variation(subscriptionRequest, amlsRegistrationNumber, accountTypeId)) {
        _ mustBe amendmentResponse
      }
    }
  }

  "renewal" must {
    "successfully submit renewal" in {
      when {
        amlsConnector.http.POST[SubscriptionRequest, AmendVariationRenewalResponse](
          eqTo(s"${amlsConnector.url}/org/id/$amlsRegistrationNumber/renewal"), eqTo(subscriptionRequest), any())(any(), any(), any(), any())
      } thenReturn Future.successful(renewalResponse)

      whenReady(amlsConnector.renewal(subscriptionRequest, amlsRegistrationNumber, accountTypeId)) {
        _ mustBe renewalResponse
      }
    }

    "successfully submit renewalAmendment" in {
      when {
        amlsConnector.http.POST[SubscriptionRequest, AmendVariationRenewalResponse](
          eqTo(s"${amlsConnector.url}/org/id/$amlsRegistrationNumber/renewalAmendment"), eqTo(subscriptionRequest), any())(any(), any(), any(), any())
      } thenReturn Future.successful(renewalResponse)

      whenReady(amlsConnector.renewalAmendment(subscriptionRequest, amlsRegistrationNumber, accountTypeId)) {
        _ mustBe renewalResponse
      }
    }

    "withdraw" must {
      "successfully withdraw the application" in {
        val postUrl = s"${amlsConnector.url}/${accountTypeId._1}/${accountTypeId._2}/$amlsRegistrationNumber/withdrawal"
        val request = WithdrawSubscriptionRequest(amlsRegistrationNumber, LocalDate.now(), WithdrawalReason.OutOfScope)
        val response = WithdrawSubscriptionResponse(LocalDateTime.now().toString)

        when {
          amlsConnector.http.POST[WithdrawSubscriptionRequest, WithdrawSubscriptionResponse](eqTo(postUrl), eqTo(request), any())(any(), any(), any(), any())
        } thenReturn Future.successful(response)

        whenReady(amlsConnector.withdraw(amlsRegistrationNumber, request, accountTypeId)) {
          _ mustBe response
        }
      }
    }

    "deregister" must {
      "successfully deregister the application" in {
        val postUrl = s"${amlsConnector.url}/${accountTypeId._1}/${accountTypeId._2}/$amlsRegistrationNumber/deregistration"
        val request = DeRegisterSubscriptionRequest(amlsRegistrationNumber, LocalDate.now(), DeregistrationReason.OutOfScope)
        val response = DeRegisterSubscriptionResponse("some date")

        when {
          amlsConnector.http.POST[DeRegisterSubscriptionRequest, DeRegisterSubscriptionResponse](eqTo(postUrl), eqTo(request), any())(any(), any(), any(), any())
        } thenReturn Future.successful(response)

        whenReady(amlsConnector.deregister(amlsRegistrationNumber, request, accountTypeId)) {
          _ mustBe response
        }
      }
    }
  }

  "savePayment" must {
    "provide a paymentId and report the status of the response" in {

      val id = "fcguhio"

      when {
        amlsConnector.http.POSTString[HttpResponse](Matchers.any(), eqTo(id), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any())
      } thenReturn Future.successful(HttpResponse(CREATED, ""))

      whenReady(amlsConnector.savePayment(id, amlsRegistrationNumber, safeId, accountTypeId)) {
        _.status mustBe CREATED
      }

    }
  }

  "createBacsPayment" must {
    "send a request to AMLS to create a BACS payment" in {
      val request = createBacsPaymentGen.sample.get
      val payment = paymentGen.sample.get
      val postUrl = s"${amlsConnector.paymentUrl}/${accountTypeId._1}/${accountTypeId._2}/bacs"

      when {
        amlsConnector.http.POST[CreateBacsPaymentRequest, Payment](eqTo(postUrl), eqTo(request), any())(any(), any(), any(), any())
      } thenReturn Future.successful(payment)

      whenReady(amlsConnector.createBacsPayment(accountTypeId, request)) { result =>
        result mustBe payment
      }

    }
  }

  "getPaymentByPaymentReference" must {
    "retrieve a payment given the payment reference" in {
      val paymentRef = paymentRefGen.sample.get
      val payment = paymentGen.sample.get.copy(reference = paymentRef)

      when {
        amlsConnector.http.GET[Payment](any(), any(), any())(any(), any(), any())
      } thenReturn Future.successful(payment)

      whenReady(amlsConnector.getPaymentByPaymentReference(paymentRef, accountTypeId)) {
        case Some(result) => result mustBe payment
        case _ => fail("Payment was not found")
      }
    }

    "return None when the payment record is not found" in {
      val paymentRef = paymentRefGen.sample.get

      when {
        amlsConnector.http.GET[Payment](any(), any(), any())(any(), any(), any())
      } thenReturn Future.failed(new NotFoundException("Payment was not found"))

      whenReady(amlsConnector.getPaymentByPaymentReference(paymentRef, accountTypeId)) {
        case Some(_) => fail("None should be returned")
        case _ =>
      }
    }
  }

  "getPaymentByAmlsReference" must {
    "retrieve a payment given an AMLS reference number" in {
      val amlsRef = amlsRefNoGen.sample.get
      val payment = paymentGen.sample.get.copy(amlsRefNo = amlsRef)

      when {
        amlsConnector.http.GET[Payment](any(), any(), any())(any(), any(), any())
      } thenReturn Future.successful(payment)

      whenReady(amlsConnector.getPaymentByAmlsReference(amlsRef, accountTypeId)) {
        case Some(result) => result mustBe payment
        case _ => fail("Payment was not found")
      }
    }

    "return None when the payment record is not found" in {
      val amlsRef = amlsRefNoGen.sample.get

      when {
        amlsConnector.http.GET[Payment](any(), any(), any())(any(), any(), any())
      } thenReturn Future.failed(new NotFoundException("Payment was not found"))

      whenReady(amlsConnector.getPaymentByAmlsReference(amlsRef, accountTypeId)) {
        case Some(_) => fail("None should be returned")
        case _ =>
      }
    }
  }

  "updateBacsStatus" must {
    "send the isBacs flag to the middle tier" in {
      val paymentRef = paymentRefGen.sample.get
      val bacsRequest = UpdateBacsRequest(true)
      val result = HttpResponse(OK, "")

      when {
        amlsConnector.http.PUT[UpdateBacsRequest, HttpResponse](any(), any(), any())(any(), any(), any(), any())
      } thenReturn Future.successful(result)

      whenReady(amlsConnector.updateBacsStatus(accountTypeId, paymentRef, bacsRequest)) { r => r mustBe result }
    }
  }

  "amlsConnector" must {
    "refesh the payment status" in {
      val result = paymentStatusResultGen.sample.get

      when {
        amlsConnector.http.PUT[RefreshPaymentStatusRequest, PaymentStatusResult](any(), any(), any())(any(), any(), any(), any())
      } thenReturn Future.successful(result)

      whenReady(amlsConnector.refreshPaymentStatus(result.amlsRef, accountTypeId)) { r => r mustBe result }
    }
  }

  "registrationDetails" must {
    "retrieve the registration details given a safe ID" in {
      val safeId = "SAFE_ID"

      when {
        amlsConnector.http.GET[RegistrationDetails](any(), any(), any())(any(), any(), any())
      } thenReturn Future.successful(RegistrationDetails("Test Company", isIndividual = false))

      whenReady(amlsConnector.registrationDetails(accountTypeId, safeId)) { result =>
        result.companyName mustBe "Test Company"
      }
    }
  }
}
