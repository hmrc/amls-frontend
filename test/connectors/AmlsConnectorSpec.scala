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
import models._
import models.declaration.AddPerson
import models.declaration.release7.RoleWithinBusinessRelease7
import models.deregister.{DeRegisterSubscriptionRequest, DeRegisterSubscriptionResponse, DeregistrationReason}
import models.payments._
import models.registrationdetails.RegistrationDetails
import models.withdrawal._
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.test.Helpers._
import play.api.{Configuration, Environment}
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import utils.HttpClientMocker

import java.net.URL
import java.time.{LocalDate, LocalDateTime}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AmlsConnectorSpec extends PlaySpec with MockitoSugar with ScalaFutures with IntegrationPatience with
  AmlsReferenceNumberGenerator with PaymentGenerator {

  trait TestSetup {
    val mocker = new HttpClientMocker()
    private val configuration: Configuration = Configuration.load(Environment.simple())
    private val config = new ApplicationConfig(configuration, new ServicesConfig(configuration))
    val amlsConnector = new AmlsConnector(
      httpClient = mocker.httpClient,
      appConfig = config
    )
  }

  val safeId: String = "SAFEID"
  val accountTypeId: (String, String) = ("org", "id")
  val (accountType, accountId) = accountTypeId

  val subscriptionRequest: SubscriptionRequest = SubscriptionRequest(
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

  val viewResponse: ViewResponse = ViewResponse(
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

  val subscriptionResponse: SubscriptionResponse = SubscriptionResponse(
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

  val amendmentResponse: AmendVariationRenewalResponse = AmendVariationRenewalResponse(
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

  val renewalResponse: AmendVariationRenewalResponse = AmendVariationRenewalResponse(
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

  val readStatusResponse: ReadStatusResponse = ReadStatusResponse(LocalDateTime.now(), "Approved", None, None, None, None, renewalConFlag = false)

  implicit val hc: HeaderCarrier = HeaderCarrier()

  "subscribe" must {

    "successfully subscribe" in new TestSetup {

      mocker.mockPost(
        url = url"${amlsConnector.url}/org/id/$safeId",
        requestBody = subscriptionRequest,
        response = subscriptionResponse)

      val eventualResponse: Future[SubscriptionResponse] = amlsConnector.subscribe(subscriptionRequest, safeId, accountTypeId)

      val result = eventualResponse.futureValue

      println(result)
      result mustBe subscriptionResponse
    }
  }

  "get status" must {

    "return correct status" in new TestSetup {

      mocker.mockGet(
        url = url"${amlsConnector.url}/$accountType/$accountId/$amlsRegistrationNumber/status",
        response = readStatusResponse)

      whenReady(amlsConnector.status(amlsRegistrationNumber, accountTypeId)) {
        _ mustBe readStatusResponse
      }
    }
  }

  "get view" must {

    "a view response" in new TestSetup {

      mocker.mockGet(
        url = url"${amlsConnector.url}/$accountType/$accountId/$amlsRegistrationNumber",
        response = viewResponse)

      whenReady(amlsConnector.view(amlsRegistrationNumber, accountTypeId)) {
        _ mustBe viewResponse
      }
    }
  }

  "update" must {

    "successfully submit amendment" in new TestSetup {
      mocker.mockPost(
        url = url"${amlsConnector.url}/org/id/$amlsRegistrationNumber/update",
        requestBody = subscriptionRequest,
        response = amendmentResponse)

      whenReady(amlsConnector.update(subscriptionRequest, amlsRegistrationNumber, accountTypeId)) {
        _ mustBe amendmentResponse
      }
    }
  }

  "variation" must {
    "successfully submit variation" in new TestSetup {

      mocker.mockPost(
        url = url"${amlsConnector.url}/org/id/$amlsRegistrationNumber/variation",
        requestBody = subscriptionRequest,
        response = amendmentResponse)

      whenReady(amlsConnector.variation(subscriptionRequest, amlsRegistrationNumber, accountTypeId)) {
        _ mustBe amendmentResponse
      }
    }
  }

  "renewal" must {
    "successfully submit renewal" in new TestSetup {
      mocker.mockPost(
        url = url"${amlsConnector.url}/org/id/$amlsRegistrationNumber/renewal",
        requestBody = subscriptionRequest,
        response = renewalResponse)

      whenReady(amlsConnector.renewal(subscriptionRequest, amlsRegistrationNumber, accountTypeId)) {
        _ mustBe renewalResponse
      }
    }

    "successfully submit renewalAmendment" in new TestSetup {

      mocker.mockPost(
        url = url"${amlsConnector.url}/org/id/$amlsRegistrationNumber/renewalAmendment",
        requestBody = subscriptionRequest,
        response = renewalResponse)

      whenReady(amlsConnector.renewalAmendment(subscriptionRequest, amlsRegistrationNumber, accountTypeId)) {
        _ mustBe renewalResponse
      }
    }

    "withdraw" must {
      "successfully withdraw the application" in new TestSetup {
        val postUrl = url"${amlsConnector.url}/${accountTypeId._1}/${accountTypeId._2}/$amlsRegistrationNumber/withdrawal"
        val request = WithdrawSubscriptionRequest(amlsRegistrationNumber, LocalDate.now(), WithdrawalReason.OutOfScope)
        val response = WithdrawSubscriptionResponse(LocalDateTime.now().toString)

        mocker.mockPost(
          url = postUrl,
          requestBody = request,
          response = response)

        whenReady(amlsConnector.withdraw(amlsRegistrationNumber, request, accountTypeId)) {
          _ mustBe response
        }
      }
    }

    "deregister" must {
      "successfully deregister the application" in new TestSetup {
        val postUrl = url"${amlsConnector.url}/${accountTypeId._1}/${accountTypeId._2}/$amlsRegistrationNumber/deregistration"
        val request = DeRegisterSubscriptionRequest(amlsRegistrationNumber, LocalDate.now(), DeregistrationReason.OutOfScope)
        val response = DeRegisterSubscriptionResponse("some date")

        mocker.mockPost(
          url = postUrl,
          requestBody = request,
          response = response)

        whenReady(amlsConnector.deregister(amlsRegistrationNumber, request, accountTypeId)) {
          _ mustBe response
        }
      }
    }
  }

  "savePayment" must {
    "provide a paymentId and report the status of the response" in new TestSetup {

      val id: String = "fcguhio"
      val postUrl: URL = url"${amlsConnector.paymentUrl}/$accountType/$accountId/$amlsRegistrationNumber/$safeId"

      mocker.mockPost(
        url = postUrl,
        requestBody = id,
        response = HttpResponse(status = CREATED, body = ""))

      whenReady(amlsConnector.savePayment(id, amlsRegistrationNumber, safeId, accountTypeId)) {
        _.status mustBe CREATED
      }

    }
  }

  "createBacsPayment" must {
    "send a request to AMLS to create a BACS payment" in new TestSetup {
      val request = createBacsPaymentGen.sample.get
      val payment: Payment = paymentGen.sample.get
      val postUrl = url"${amlsConnector.paymentUrl}/$accountType/$accountId/bacs"


      mocker.mockPost(
        url = postUrl,
        requestBody = request,
        response = payment)

      whenReady(amlsConnector.createBacsPayment(accountTypeId, request)) { result =>
        result mustBe payment
      }

    }
  }

  "getPaymentByPaymentReference" must {
    "retrieve a payment given the payment reference" in new TestSetup {
      val paymentRef: String = paymentRefGen.sample.get
      val payment: Payment = paymentGen.sample.get.copy(reference = paymentRef)

      mocker.mockGet[Option[Payment]](
        url = url"${amlsConnector.paymentUrl}/$accountType/$accountId/payref/$paymentRef",
        response = Some(payment))

      whenReady(amlsConnector.getPaymentByPaymentReference(paymentRef, accountTypeId)) {
        case Some(result) => result mustBe payment
        case _ => fail("Payment was not found")
      }
    }

    "return None when the payment record is not found" in new TestSetup {
      val paymentRef = paymentRefGen.sample.get

      mocker.mockGet[Option[Payment]](
        url = url"${amlsConnector.paymentUrl}/$accountType/$accountId/payref/$paymentRef",
        response = None
      )

      whenReady(amlsConnector.getPaymentByPaymentReference(paymentRef, accountTypeId)) {
        case Some(_) => fail("None should be returned")
        case _ =>
      }
    }
  }

  "getPaymentByAmlsReference" must {
    "retrieve a payment given an AMLS reference number" in new TestSetup {
      val amlsRef = amlsRefNoGen.sample.get
      val payment = paymentGen.sample.get.copy(amlsRefNo = amlsRef)

      mocker.mockGet[Option[Payment]](
        url = url"${amlsConnector.paymentUrl}/$accountType/$accountId/amlsref/$amlsRef",
        response = Some(payment))

      whenReady(amlsConnector.getPaymentByAmlsReference(amlsRef, accountTypeId)) {
        case Some(result) => result mustBe payment
        case _ => fail("Payment was not found")
      }
    }

    "return None when the payment record is not found" in new TestSetup {
      val amlsRef = amlsRefNoGen.sample.get

      mocker.mockGet[Option[Payment]](
        url = url"${amlsConnector.paymentUrl}/$accountType/$accountId/amlsref/$amlsRef",
        response = None
      )

      whenReady(amlsConnector.getPaymentByAmlsReference(amlsRef, accountTypeId)) {
        case Some(_) => fail("None should be returned")
        case _ =>
      }
    }
  }

  "updateBacsStatus" must {
    "send the isBacs flag to the middle tier" in new TestSetup {
      val paymentRef = paymentRefGen.sample.get
      val bacsRequest = UpdateBacsRequest(true)
      val result = HttpResponse(OK, "")

      mocker.mockPut(
        url = url"${amlsConnector.paymentUrl}/${accountTypeId._1}/${accountTypeId._2}/$paymentRef/bacs",
        requestBody = bacsRequest,
        response = result)

      whenReady(amlsConnector.updateBacsStatus(accountTypeId, paymentRef, bacsRequest)) { r => r mustBe result }
    }
  }

  "amlsConnector" must {
    "refesh the payment status" in new TestSetup {
      val result = paymentStatusResultGen.sample.get
      val paymentReference = result.amlsRef

      mocker.mockPut(
        url = url"${amlsConnector.paymentUrl}/$accountType/$accountId/refreshstatus",
        requestBody = RefreshPaymentStatusRequest(paymentReference),
        response = result)

      whenReady(amlsConnector.refreshPaymentStatus(paymentReference, accountTypeId)) { r => r mustBe result }
    }
  }

  "registrationDetails" must {
    "retrieve the registration details given a safe ID" in new TestSetup {
      val safeId = "SAFE_ID"

      mocker.mockGet(
        url = url"${amlsConnector.registrationUrl}/${accountTypeId._1}/${accountTypeId._2}/details/$safeId",
        response = RegistrationDetails("Test Company", isIndividual = false))

      whenReady(amlsConnector.registrationDetails(accountTypeId, safeId)) { result =>
        result.companyName mustBe "Test Company"
      }
    }
  }
}
