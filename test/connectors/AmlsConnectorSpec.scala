/*
 * Copyright 2017 HM Revenue & Customs
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

import generators.{AmlsReferenceNumberGenerator, PaymentGenerator}
import models.{AmendVariationRenewalResponse, _}
import models.declaration.AddPerson
import models.declaration.release7.RoleWithinBusinessRelease7
import models.deregister.{DeRegisterSubscriptionRequest, DeRegisterSubscriptionResponse, DeregistrationReason}
import models.payments._
import models.registrationdetails.RegistrationDetails
import models.withdrawal._
import org.joda.time.{LocalDate, LocalDateTime}
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import uk.gov.hmrc.domain.Org
import uk.gov.hmrc.play.frontend.auth.connectors.domain._
import uk.gov.hmrc.play.frontend.auth.{AuthContext, LoggedInUser, Principal}
import uk.gov.hmrc.play.http._
import play.api.test.Helpers._
import org.mockito.Matchers
import play.api.libs.json.Json

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import uk.gov.hmrc.http.{ HeaderCarrier, HttpGet, HttpPost, HttpPut, HttpResponse, NotFoundException }

class AmlsConnectorSpec extends PlaySpec with MockitoSugar with ScalaFutures with IntegrationPatience with AmlsReferenceNumberGenerator with PaymentGenerator {

  object AmlsConnector extends AmlsConnector {

    override private[connectors] val httpPost: HttpPost = mock[HttpPost]
    override private[connectors] val httpPut: HttpPut = mock[HttpPut]
    override private[connectors] val url: String = "amls/subscription"
    override private[connectors] val paymentUrl: String = "amls/payment"
    override private[connectors] val httpGet: HttpGet = mock[HttpGet]
    override private[connectors] val registrationUrl = "amls/registration"
  }

  val safeId = "SAFEID"

  val subscriptionRequest = SubscriptionRequest(
    businessMatchingSection = None,
    eabSection = None,
    tradingPremisesSection = None,
    aboutTheBusinessSection = None,
    bankDetailsSection = None,
    aboutYouSection = None,
    businessActivitiesSection = None,
    responsiblePeopleSection = None,
    tcspSection = None,
    aspSection = None,
    msbSection = None,
    hvdSection = None,
    supervisionSection = None
  )

  val viewResponse = ViewResponse(
    etmpFormBundleNumber = "FORMBUNDLENUMBER",
    businessMatchingSection = None,
    eabSection = None,
    tradingPremisesSection = None,
    aboutTheBusinessSection = None,
    bankDetailsSection = Seq(None),
    aboutYouSection = AddPerson("FirstName", None, "LastName", RoleWithinBusinessRelease7(Set(models.declaration.release7.BeneficialShareholder))),
    businessActivitiesSection = None,
    responsiblePeopleSection = None,
    tcspSection = None,
    aspSection = None,
    msbSection = None,
    hvdSection = None,
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
    premiseFee = 0,
    premiseFeeRate = None,
    totalFees = 0,
    paymentReference = Some(""),
    difference = Some(0)
  )

  val readStatusResponse = ReadStatusResponse(LocalDateTime.now(), "Approved", None, None, None, None, false)

  implicit val hc = HeaderCarrier()
  implicit val ac = AuthContext(
    LoggedInUser(
      "UserName",
      None,
      None,
      None,
      CredentialStrength.Weak,
      ConfidenceLevel.L50, ""),
    Principal(
      None,
      Accounts(org = Some(OrgAccount("Link", Org("TestOrgRef"))))),
    None,
    None,
    None, None)

  "subscribe" must {

    "successfully subscribe" in {

      when {
        AmlsConnector.httpPost.POST[SubscriptionRequest, SubscriptionResponse](eqTo(s"${AmlsConnector.url}/org/TestOrgRef/$safeId"), eqTo(subscriptionRequest), any())(any(), any(), any(), any())
      } thenReturn Future.successful(subscriptionResponse)

      whenReady(AmlsConnector.subscribe(subscriptionRequest, safeId)) {
        _ mustBe subscriptionResponse
      }
    }
  }

  "get status" must {

    "return correct status" in {
      when {
        AmlsConnector.httpGet.GET[ReadStatusResponse](eqTo(s"${AmlsConnector.url}/org/TestOrgRef/$amlsRegistrationNumber/status"))(any(), any(), any())
      } thenReturn Future.successful(readStatusResponse)

      whenReady(AmlsConnector.status(amlsRegistrationNumber)) {
        _ mustBe readStatusResponse
      }
    }
  }

  "get view" must {

    "a view response" in {
      when {
        AmlsConnector.httpGet.GET[ViewResponse](eqTo(s"${AmlsConnector.url}/org/TestOrgRef/$amlsRegistrationNumber"))(any(), any(), any())
      } thenReturn Future.successful(viewResponse)

      whenReady(AmlsConnector.view(amlsRegistrationNumber)) {
        _ mustBe viewResponse
      }
    }
  }

  "update" must {

    "successfully submit amendment" in {
      when {
        AmlsConnector.httpPost.POST[SubscriptionRequest, AmendVariationRenewalResponse](eqTo(s"${AmlsConnector.url}/org/TestOrgRef/$amlsRegistrationNumber/update")
          , eqTo(subscriptionRequest), any())(any(), any(), any(), any())
      }.thenReturn(Future.successful(amendmentResponse))

      whenReady(AmlsConnector.update(subscriptionRequest, amlsRegistrationNumber)) {
        _ mustBe amendmentResponse
      }
    }
  }

  "variation" must {
    "successfully submit variation" in {
      when {
        AmlsConnector.httpPost.POST[SubscriptionRequest, AmendVariationRenewalResponse](eqTo(s"${AmlsConnector.url}/org/TestOrgRef/$amlsRegistrationNumber/variation")
          , eqTo(subscriptionRequest), any())(any(), any(), any(), any())
      }.thenReturn(Future.successful(amendmentResponse))

      whenReady(AmlsConnector.variation(subscriptionRequest, amlsRegistrationNumber)) {
        _ mustBe amendmentResponse
      }
    }
  }

  "renewal" must {
    "successfully submit renewal" in {
      when {
        AmlsConnector.httpPost.POST[SubscriptionRequest, AmendVariationRenewalResponse](
          eqTo(s"${AmlsConnector.url}/org/TestOrgRef/$amlsRegistrationNumber/renewal"), eqTo(subscriptionRequest), any())(any(), any(), any(), any())
      } thenReturn Future.successful(renewalResponse)

      whenReady(AmlsConnector.renewal(subscriptionRequest, amlsRegistrationNumber)) {
        _ mustBe renewalResponse
      }
    }

    "successfully submit renewalAmendment" in {
      when {
        AmlsConnector.httpPost.POST[SubscriptionRequest, AmendVariationRenewalResponse](
          eqTo(s"${AmlsConnector.url}/org/TestOrgRef/$amlsRegistrationNumber/renewalAmendment"), eqTo(subscriptionRequest), any())(any(), any(), any(), any())
      } thenReturn Future.successful(renewalResponse)

      whenReady(AmlsConnector.renewalAmendment(subscriptionRequest, amlsRegistrationNumber)) {
        _ mustBe renewalResponse
      }
    }

    "withdraw" must {
      "successfully withdraw the application" in {
        val postUrl = s"${AmlsConnector.url}/org/TestOrgRef/$amlsRegistrationNumber/withdrawal"
        val request = WithdrawSubscriptionRequest(amlsRegistrationNumber, LocalDate.now(), WithdrawalReason.OutOfScope)
        val response = WithdrawSubscriptionResponse(LocalDateTime.now().toString)

        when {
          AmlsConnector.httpPost.POST[WithdrawSubscriptionRequest, WithdrawSubscriptionResponse](eqTo(postUrl), eqTo(request), any())(any(), any(), any(), any())
        } thenReturn Future.successful(response)

        whenReady(AmlsConnector.withdraw(amlsRegistrationNumber, request)) {
          _ mustBe response
        }
      }
    }

    "deregister" must {
      "successfully deregister the application" in {
        val postUrl = s"${AmlsConnector.url}/org/TestOrgRef/$amlsRegistrationNumber/deregistration"
        val request = DeRegisterSubscriptionRequest(amlsRegistrationNumber, LocalDate.now(), DeregistrationReason.OutOfScope)
        val response = DeRegisterSubscriptionResponse("some date")

        when {
          AmlsConnector.httpPost.POST[DeRegisterSubscriptionRequest, DeRegisterSubscriptionResponse](eqTo(postUrl), eqTo(request), any())(any(), any(), any(), any())
        } thenReturn Future.successful(response)

        whenReady(AmlsConnector.deregister(amlsRegistrationNumber, request)) {
          _ mustBe response
        }
      }
    }
  }

  "savePayment" must {
    "provide a paymentId and report the status of the response" in {

      val id = "fcguhio"

      when {
        AmlsConnector.httpPost.POSTString[HttpResponse](Matchers.any(), eqTo(id), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any())
      } thenReturn Future.successful(HttpResponse(CREATED))

      whenReady(AmlsConnector.savePayment(id, amlsRegistrationNumber, safeId)) {
        _.status mustBe CREATED
      }

    }
  }

  "createBacsPayment" must {
    "send a request to AMLS to create a BACS payment" in {
      val request = createBacsPaymentGen.sample.get
      val payment = paymentGen.sample.get
      val postUrl = s"${AmlsConnector.paymentUrl}/org/TestOrgRef/bacs"

      when {
        AmlsConnector.httpPost.POST[CreateBacsPaymentRequest, Payment](eqTo(postUrl), eqTo(request), any())(any(), any(), any(), any())
      } thenReturn Future.successful(payment)

      whenReady(AmlsConnector.createBacsPayment(request)) { result =>
        result mustBe payment
      }

    }
  }

  "getPaymentByPaymentReference" must {
    "retrieve a payment given the payment reference" in {
      val paymentRef = paymentRefGen.sample.get
      val payment = paymentGen.sample.get.copy(reference = paymentRef)
      val getUrl = s"${AmlsConnector.paymentUrl}/org/TestOrgRef/payref/$paymentRef"

      when {
        AmlsConnector.httpGet.GET[Payment](eqTo(getUrl))(any(), any(), any())
      } thenReturn Future.successful(payment)

      whenReady(AmlsConnector.getPaymentByPaymentReference(paymentRef)) {
        case Some(result) => result mustBe payment
        case _ => fail("Payment was not found")
      }
    }

    "return None when the payment record is not found" in {
      val paymentRef = paymentRefGen.sample.get

      when {
        AmlsConnector.httpGet.GET[Payment](any())(any(), any(), any())
      } thenReturn Future.failed(new NotFoundException("Payment was not found"))

      whenReady(AmlsConnector.getPaymentByPaymentReference(paymentRef)) {
        case Some(_) => fail("None should be returned")
        case _ =>
      }
    }
  }

  "getPaymentByAmlsReference" must {
    "retrieve a payment given an AMLS reference number" in {
      val amlsRef = amlsRefNoGen.sample.get
      val payment = paymentGen.sample.get.copy(amlsRefNo = amlsRef)
      val getUrl = s"${AmlsConnector.paymentUrl}/org/TestOrgRef/amlsref/$amlsRef"

      when {
        AmlsConnector.httpGet.GET[Payment](eqTo(getUrl))(any(), any(), any())
      } thenReturn Future.successful(payment)

      whenReady(AmlsConnector.getPaymentByAmlsReference(amlsRef)) {
        case Some(result) => result mustBe payment
        case _ => fail("Payment was not found")
      }
    }

    "return None when the payment record is not found" in {
      val amlsRef = amlsRefNoGen.sample.get

      when {
        AmlsConnector.httpGet.GET[Payment](any())(any(), any(), any())
      } thenReturn Future.failed(new NotFoundException("Payment was not found"))

      whenReady(AmlsConnector.getPaymentByAmlsReference(amlsRef)) {
        case Some(_) => fail("None should be returned")
        case _ =>
      }
    }
  }

  "updateBacsStatus" must {
    "send the isBacs flag to the middle tier" in {
      val paymentRef = paymentRefGen.sample.get
      val putUrl = s"${AmlsConnector.paymentUrl}/org/TestOrgRef/$paymentRef/bacs"
      val bacsRequest = UpdateBacsRequest(true)

      when {
        AmlsConnector.httpPut.PUT[UpdateBacsRequest, HttpResponse](any(), any())(any(), any(), any(), any())
      } thenReturn Future.successful(HttpResponse(OK))

      whenReady(AmlsConnector.updateBacsStatus(paymentRef, bacsRequest)) { result =>
        verify(AmlsConnector.httpPut).PUT(eqTo(putUrl), eqTo(bacsRequest))(any(), any(), any(), any())
      }
    }
  }

  "AmlsConnector" must {
    "refesh the payment status" in {
      val result = paymentStatusResultGen.sample.get

      when {
        AmlsConnector.httpPut.PUT[RefreshPaymentStatusRequest, PaymentStatusResult](any(), any())(any(), any(), any(), any())
      } thenReturn Future.successful(result)

      whenReady(AmlsConnector.refreshPaymentStatus(result.amlsRef)) { r => r mustBe result }
    }
  }

  "registrationDetails" must {
    "retrieve the registration details given a safe ID" in {
      val safeId = "SAFE_ID"
      val url = s"${AmlsConnector.registrationUrl}/org/TestOrgRef/details/$safeId"

      when {
        AmlsConnector.httpGet.GET[RegistrationDetails](eqTo(url))(any(), any(), any())
      } thenReturn Future.successful(RegistrationDetails("Test Company", isIndividual = false))

      whenReady(AmlsConnector.registrationDetails(safeId)) { result =>
        result.companyName mustBe "Test Company"
      }
    }
  }
}
