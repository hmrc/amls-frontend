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

package controllers

import cats.implicits._
import connectors._
import generators.{AmlsReferenceNumberGenerator, PaymentGenerator}
import models.businesscustomer.{Address, ReviewDetails}
import models.businessmatching.BusinessMatching
import models.confirmation.{BreakdownRow, Currency}
import models.payments.PaymentStatuses.{Cancelled, Failed}
import models.payments._
import models.renewal.{InvolvedInOtherNo, Renewal}
import models.status._
import models.{ReadStatusResponse, SubscriptionFees, SubscriptionResponse}
import org.joda.time.LocalDate
import org.jsoup.Jsoup
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import play.api.i18n.Messages
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.Cookie
import play.api.test.Helpers._
import play.api.{Application, Mode}
import services.{AuthEnrolmentsService, PaymentsService, StatusService, SubmissionResponseService}
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpResponse}
import utils.{AuthorisedFixture, GenericTestHelper}

import scala.concurrent.{ExecutionContext, Future}

class ConfirmationControllerSpec extends GenericTestHelper with MockitoSugar with AmlsReferenceNumberGenerator with PaymentGenerator {

  val paymentsConnector = mock[PayApiConnector]
  val mockAmlsConnector = mock[AmlsConnector]
  val paymentsService = new PaymentsService(mockAmlsConnector, paymentsConnector, mock[SubmissionResponseService],mock[StatusService])

  implicit override lazy val app: Application = new GuiceApplicationBuilder()
    .disable[com.kenshoo.play.metrics.PlayModule]
    .bindings(bindModules: _*).in(Mode.Test)
    .bindings(bind[PayApiConnector].to(paymentsConnector))
    .bindings(bind[PaymentsService].to(paymentsService))
    .configure("microservice.services.feature-toggle.payments-url-lookup" -> true)
    .configure("microservice.services.feature-toggle.business-name-lookup" -> false)
    .build()

  trait Fixture extends AuthorisedFixture {

    self =>

    implicit val authContext = mock[AuthContext]
    implicit val executionContext = mock[ExecutionContext]
    implicit val headerCarrier = HeaderCarrier()

    val baseUrl = "http://localhost"
    val request = addToken(authRequest).copyFakeRequest(uri = baseUrl)

    val controller = new ConfirmationController {
      override protected val authConnector = self.authConnector
      override private[controllers] val submissionResponseService = mock[SubmissionResponseService]
      override val statusService: StatusService = mock[StatusService]
      override val keystoreConnector = mock[KeystoreConnector]
      override val dataCacheConnector = mock[DataCacheConnector]
      override val amlsConnector = mockAmlsConnector
      override val authEnrolmentsService = mock[AuthEnrolmentsService]
    }

    val paymentRefNo = "XA000000000000"

    val response = SubscriptionResponse(
      etmpFormBundleNumber = "",
      amlsRefNo = "", Some(SubscriptionFees(
        paymentReference = paymentRefNo,
        registrationFee = 0,
        fpFee = None,
        fpFeeRate = None,
        premiseFee = 0,
        premiseFeeRate = None,
        totalFees = 0
      ))
    )

    protected val mockCacheMap = mock[CacheMap]
    val companyName = "My Test Company"

    setupBusinessMatching(companyName)

    reset(paymentsConnector)

    when {
      controller.submissionResponseService.getSubscription(any(), any(), any())
    } thenReturn {
      Future.successful((Some(paymentRefNo), Currency.fromInt(0), Seq(), Left(amlsRegistrationNumber)))
    }

    when {
      controller.keystoreConnector.setConfirmationStatus(any(), any())
    } thenReturn Future.successful()

    when {
      controller.authEnrolmentsService.amlsRegistrationNumber(any(), any(), any())
    } thenReturn {
      Future.successful(Some(amlsRegistrationNumber))
    }

    when {
      paymentsConnector.createPayment(any())(any(), any())
    } thenReturn Future.successful(Some(CreatePaymentResponse(PayApiLinks("/payments"), Some(amlsRegistrationNumber))))

    when {
      mockAmlsConnector.refreshPaymentStatus(any())(any(), any(), any())
    } thenReturn Future.successful(paymentStatusResultGen.sample.get.copy(currentStatus = PaymentStatuses.Successful))

    when {
      mockAmlsConnector.getPaymentByReference(any())(any(), any(), any())
    } thenReturn Future.successful(paymentGen.sample)

    when {
      mockAmlsConnector.savePayment(any(), any())(any(), any(), any())
    } thenReturn {
      Future.successful(HttpResponse(CREATED))
    }

    def paymentsReturnLocation(ref: String) = ReturnLocation(controllers.routes.ConfirmationController.paymentConfirmation(ref))

    def setupBusinessMatching(companyName: String) = {
      val model = BusinessMatching(
        reviewDetails = Some(ReviewDetails(companyName, None, mock[Address], ""))
      )

      when {
        controller.dataCacheConnector.fetch[BusinessMatching](eqTo(BusinessMatching.key))(any(), any(), any())
      } thenReturn Future.successful(Some(model))

      when {
        controller.dataCacheConnector.fetch[Renewal](eqTo(Renewal.key))(any(), any(), any())
      } thenReturn Future.successful(None)
    }

    def setupStatus(status: SubmissionStatus): Unit = {

      when {
        controller.statusService.getStatus(any(), any(), any())
      } thenReturn Future.successful(status)

      val statusResponse = mock[ReadStatusResponse]
      when(statusResponse.safeId) thenReturn safeIdGen.sample

      when {
        controller.statusService.getDetailedStatus(any(), any(), any())
      } thenReturn Future.successful((status, Some(statusResponse)))
    }
  }

  "ConfirmationController" must {

    "write a confirmation value to Keystore" in new Fixture {

      setupStatus(SubmissionReady)

      val result = controller.get()(request)

      status(result) mustBe OK

      verify(controller.keystoreConnector).setConfirmationStatus(any(), any())

    }

    "notify user of progress if application has not already been submitted" in new Fixture {
      setupStatus(SubmissionReady)

      val result = controller.get()(request)
      status(result) mustBe OK
      Jsoup.parse(contentAsString(result)).title must include("Application fee and reference")
      contentAsString(result) must include(paymentRefNo)
    }

    "notify user there is no fee" when {

      "an amendment has difference(/Some(0))" in new Fixture {

        setupStatus(SubmissionDecisionApproved)

        when {
          controller.submissionResponseService.getSubmissionData(eqTo(SubmissionReadyForReview))(any(),any(),any())
        } thenReturn Future.successful(Some((Some(paymentRefNo), Currency.fromInt(0), Seq(), Right(Some(Currency.fromInt(0))))))

        val result = controller.get()(request)
        status(result) mustBe OK

        Jsoup.parse(contentAsString(result)).title must include("You’ve submitted your updated information")
        contentAsString(result) must include(Messages("confirmation.no.fee"))
        contentAsString(result) must include(companyName)

      }

      "an amendment has no difference(/None)" in new Fixture {

        setupStatus(SubmissionReadyForReview)

        when {
          controller.submissionResponseService.getSubmissionData(eqTo(SubmissionReadyForReview))(any(),any(),any())
        } thenReturn Future.successful(Some((Some(paymentRefNo), Currency.fromInt(0), Seq(), Right(None))))

        val result = controller.get()(request)
        status(result) mustBe OK

        Jsoup.parse(contentAsString(result)).title must include("You’ve submitted your updated information")
        contentAsString(result) must include(Messages("confirmation.no.fee"))
        contentAsString(result) must include(companyName)
      }

      "an amendment has no payment reference" in new Fixture {

        setupStatus(SubmissionReadyForReview)

        when {
          controller.submissionResponseService.getSubmissionData(eqTo(SubmissionReadyForReview))(any(),any(),any())
        } thenReturn Future.successful(Some((None, Currency.fromInt(0), Seq(), Right(None))))

        val result = controller.get()(request)
        status(result) mustBe OK

        Jsoup.parse(contentAsString(result)).title must include("You’ve submitted your updated information")
        contentAsString(result) must include(Messages("confirmation.no.fee"))
        contentAsString(result) must include(companyName)

      }

      "a variation has no payment reference" in new Fixture {

        setupStatus(SubmissionDecisionApproved)

        when {
          controller.submissionResponseService.getSubmissionData(eqTo(SubmissionDecisionApproved))(any(),any(),any())
        } thenReturn Future.successful(Some(Some(""), Currency.fromInt(0), Seq(), Right(None)))

        val result = controller.get()(request)
        status(result) mustBe OK

        Jsoup.parse(contentAsString(result)).title must include("You’ve submitted your updated information")
        contentAsString(result) must include(Messages("confirmation.no.fee"))
        contentAsString(result) must include(companyName)
      }

      "a variation without the addition of tp or rp" in new Fixture {

        setupStatus(SubmissionDecisionApproved)

        when {
          controller.submissionResponseService.getSubmissionData(eqTo(SubmissionDecisionApproved))(any(),any(),any())
        } thenReturn Future.successful(Some(Some(""), Currency.fromInt(0), Seq(), Right(None)))

        val result = controller.get()(request)

        status(result) mustBe OK

        Jsoup.parse(contentAsString(result)).title must include("You’ve submitted your updated information")
        contentAsString(result) must include(Messages("confirmation.no.fee"))
        contentAsString(result) must include(companyName)
      }

      "a variation when status is ready for renewal and no renewal data in save4later" in new Fixture {

        setupStatus(ReadyForRenewal(Some(new LocalDate)))

        when {
          controller.submissionResponseService.getSubmissionData(eqTo(ReadyForRenewal(Some(new LocalDate))))(any(),any(),any())
        } thenReturn Future.successful(Some(Some(""), Currency.fromInt(0), Seq(), Right(None)))

        val result = controller.get()(request)

        status(result) mustBe OK

        Jsoup.parse(contentAsString(result)).title must include("You’ve submitted your updated information")
        contentAsString(result) must include(Messages("confirmation.no.fee"))
        contentAsString(result) must include(companyName)
      }

      "a renewal has no payment reference" in new Fixture {

        setupStatus(ReadyForRenewal(Some(new LocalDate)))

        when {
          controller.dataCacheConnector.fetch[Renewal](eqTo(Renewal.key))(any(), any(), any())
        } thenReturn Future.successful(Some(Renewal(Some(InvolvedInOtherNo))))

        when {
          controller.submissionResponseService.getSubmissionData(eqTo(ReadyForRenewal(Some(new LocalDate))))(any(),any(),any())
        } thenReturn Future.successful(Some((None, Currency.fromInt(0), Seq(), Right(None))))

        val result = controller.get()(request)
        status(result) mustBe OK

        Jsoup.parse(contentAsString(result)).title must include("You’ve submitted your updated information")
        contentAsString(result) must include(Messages("confirmation.no.fee"))
        contentAsString(result) must include(companyName)
      }

      "a renewal status and has data then load renewal confirmation" in new Fixture {
        setupStatus(ReadyForRenewal(Some(new LocalDate)))

        when {
          controller.dataCacheConnector.fetch[Renewal](eqTo(Renewal.key))(any(), any(), any())
        } thenReturn Future.successful(Some(Renewal()))

        when {
          controller.submissionResponseService.getSubmissionData(eqTo(ReadyForRenewal(Some(new LocalDate))))(any(),any(),any())
        } thenReturn Future.successful(Some((Some("payeref"), Currency.fromInt(100000), Seq(BreakdownRow("", 10, Currency(10), Currency(10))), Right(None))))

        val result = controller.get()(request)
        status(result) mustBe OK
        Jsoup.parse(contentAsString(result)).title must include(Messages("confirmation.renewal.title"))
      }

      "a renewal status and has 1 FP RP and 1 Not FP RP then load renewal confirmation showing each row with respective costs" in new Fixture {

        setupStatus(ReadyForRenewal(Some(new LocalDate)))

        when {
          controller.dataCacheConnector.fetch[Renewal](eqTo(Renewal.key))(any(), any(), any())
        } thenReturn Future.successful(Some(Renewal()))


        when {
          controller.submissionResponseService.getSubmissionData(eqTo(ReadyForRenewal(Some(new LocalDate))))(any(),any(),any())
        } thenReturn Future.successful(Some((
          Some("payeref"),
          Currency.fromInt(100),
          Seq(
            BreakdownRow("confirmation.responsiblepeople.fp.passed", 1, Currency(0), Currency(0)),
            BreakdownRow("confirmation.responsiblepeople", 1, Currency(100), Currency(100)),
            BreakdownRow("confirmation.tradingpremises.half", 2, Currency(50), Currency(100))
          ), Right(None))))


        val result = controller.get()(request)
        status(result) mustBe OK

        Jsoup.parse(contentAsString(result)).title must include(Messages("confirmation.renewal.title"))
        contentAsString(result) must include(Messages("confirmation.responsiblepeople.fp.passed"))
        contentAsString(result) must include(Messages("confirmation.responsiblepeople"))
        contentAsString(result) must include(Messages("confirmation.tradingpremises.half"))
      }

      "a renewal and no data in save4later then load variation confirmation" in new Fixture {
        setupStatus(ReadyForRenewal(Some(new LocalDate)))

        when {
          controller.dataCacheConnector.fetch[Renewal](eqTo(Renewal.key))(any(), any(), any())
        } thenReturn Future.successful(None)

        when {
          controller.submissionResponseService.getSubmissionData(eqTo(ReadyForRenewal(Some(new LocalDate))))(any(),any(),any())
        } thenReturn Future.successful(Some(Some("payeref"), Currency.fromInt(100000), Seq(BreakdownRow("", 10, Currency(10), Currency(10))), Right(None)))

        val result = controller.get()(request)
        status(result) mustBe OK
        Jsoup.parse(contentAsString(result)).title must include(Messages("confirmation.amendment.header"))
      }

    }

    "allow a payment to be retried" in new Fixture {

      val paymentRef = paymentRefGen.sample.get
      val paymentsRedirectUrl = "/payments"
      val amountInPence = (paymentAmountGen.sample.get * 100).toInt
      val postData = "paymentRef" -> paymentRef
      val payment = paymentGen.sample.get

      when {
        mockAmlsConnector.getPaymentByReference(eqTo(paymentRef))(any(), any(), any())
      } thenReturn Future.successful(Some(payment.copy(reference = paymentRef, amountInPence = amountInPence)))

      val result = controller.retryPayment()(request.withFormUrlEncodedBody(postData))

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(paymentsRedirectUrl)

      verify(controller.paymentsConnector).createPayment(eqTo(
        CreatePaymentRequest("other", paymentRef, "AMLS Payment", amountInPence, paymentsReturnLocation(paymentRef))))(any(), any())
    }

    "fail if a payment cannot be retried" in new Fixture {
      val paymentRef = paymentRefGen.sample.get
      val postData = "paymentRef" -> paymentRef

      when {
        mockAmlsConnector.getPaymentByReference(eqTo(paymentRef))(any(), any(), any())
      } thenReturn Future.successful(None)

      val result = controller.retryPayment()(request.withFormUrlEncodedBody(postData))

      status(result) mustBe INTERNAL_SERVER_ERROR
    }

    "show the correct payment confirmation page" when {
      "the application status is 'new submission'" in new Fixture {
        setupStatus(SubmissionReady)

        val paymentReference = "X0000000000000"
        val result = controller.paymentConfirmation(paymentReference)(request)

        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))

        doc.select("h1.heading-large").text mustBe Messages("confirmation.payment.lede")
        doc.select(".confirmation").text must include(paymentReference)
        doc.select(".confirmation").text must include(companyName)
      }

      "the application status is 'pending'" in new Fixture {
        setupStatus(SubmissionReadyForReview)

        val paymentReference = "X00000000000000"
        val result = controller.paymentConfirmation(paymentReference)(request)

        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))

        doc.title must include(Messages("confirmation.payment.amendvariation.title"))
        doc.select("h1.heading-large").text mustBe Messages("confirmation.payment.amendvariation.lede")
        doc.select(".confirmation").text must include(paymentReference)
        doc.select(".confirmation").text must include(companyName)
        contentAsString(result) must include(Messages("confirmation.payment.amendvariation.info.keep_up_to_date"))
      }

      "the application status is 'approved'" in new Fixture {
        setupStatus(SubmissionDecisionApproved)

        val paymentReference = "X00000000000"
        val result = controller.paymentConfirmation(paymentReference)(request)

        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))

        doc.title must include(Messages("confirmation.payment.amendvariation.title"))
        doc.select("h1.heading-large").text mustBe Messages("confirmation.payment.amendvariation.lede")
        doc.select(".confirmation").text must include(paymentReference)
        doc.select(".confirmation").text must include(companyName)
        contentAsString(result) must include(Messages("confirmation.payment.amendvariation.info.keep_up_to_date"))
      }

      "the application status is 'Renewal Submitted'" in new Fixture {
        setupStatus(RenewalSubmitted(None))

        val paymentReference = "X00000000000"
        val result = controller.paymentConfirmation(paymentReference)(request)

        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))

        doc.title must include(Messages("confirmation.payment.amendvariation.title"))
        doc.select("h1.heading-large").text mustBe Messages("confirmation.payment.amendvariation.lede")
        doc.select(".confirmation").text must include(paymentReference)
        doc.select(".confirmation").text must include(companyName)
        contentAsString(result) must include(Messages("confirmation.payment.amendvariation.info.keep_up_to_date"))
      }

      "the application status is 'ready for renewal'" in new Fixture {
        setupStatus(ReadyForRenewal(Some(new LocalDate())))

        val paymentReference = "X00000000000"

        when {
          controller.dataCacheConnector.fetch[Renewal](eqTo(Renewal.key))(any(), any(), any())
        }.thenReturn(Future.successful(Some(Renewal(Some(InvolvedInOtherNo)))))

        val result = controller.paymentConfirmation(paymentReference)(request)

        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))

        doc.title must include(Messages("confirmation.payment.renewal.title"))
        doc.select("h1.heading-large").text mustBe Messages("confirmation.payment.renewal.lede")
        doc.select(".confirmation").text must include(paymentReference)
        doc.select(".confirmation").text must include(companyName)
        contentAsString(result) must include(Messages("confirmation.payment.amendvariation.info.keep_up_to_date"))
      }

      "the application status is 'ready for renewal' and user has done only variation" in new Fixture {
        setupStatus(ReadyForRenewal(Some(new LocalDate())))

        val paymentReference = "X00000000000"
        val result = controller.paymentConfirmation(paymentReference)(request)

        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))

        doc.title must include(Messages("confirmation.payment.amendvariation.title"))
        doc.select("h1.heading-large").text mustBe Messages("confirmation.payment.amendvariation.lede")
        doc.select(".confirmation").text must include(paymentReference)
        doc.select(".confirmation").text must include(companyName)
        contentAsString(result) must include(Messages("confirmation.payment.amendvariation.info.keep_up_to_date"))
      }

      "there is no business name" in new Fixture {
        setupStatus(SubmissionReady)

        val paymentReference = "X00000000000000000"

        when {
          controller.dataCacheConnector.fetch[BusinessMatching](eqTo(BusinessMatching.key))(any(), any(), any())
        } thenReturn Future.successful(none[BusinessMatching])

        val result = controller.paymentConfirmation(paymentReference)(request)

        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))

        doc.select(".confirmation p").text must startWith(Messages("confirmation.payment.reference_header", paymentReference))
      }

      "the payment failed" in new Fixture {
        setupStatus(SubmissionReadyForReview)

        val payment = paymentGen.sample.get.copy(status = Failed)
        val paymentStatus = paymentStatusResultGen.sample.get.copy(currentStatus = payment.status)

        when {
          mockAmlsConnector.refreshPaymentStatus(any())(any(), any(), any())
        } thenReturn Future.successful(paymentStatus)

        val result = controller.paymentConfirmation(payment.reference)(request)

        status(result) mustBe OK

        verify(mockAmlsConnector).refreshPaymentStatus(eqTo(payment.reference))(any(), any(), any())
        contentAsString(result) must include(Messages("confirmation.payment.failed.header"))
        contentAsString(result) must include(Messages("confirmation.payment.failed.reason.failure"))
      }

      "the payment was cancelled" in new Fixture {
        setupStatus(SubmissionReadyForReview)

        val payment = paymentGen.sample.get.copy(status = Cancelled)
        val paymentStatus = paymentStatusResultGen.sample.get.copy(currentStatus = payment.status)

        when {
          mockAmlsConnector.refreshPaymentStatus(any())(any(), any(), any())
        } thenReturn Future.successful(paymentStatus)

        val result = controller.paymentConfirmation(payment.reference)(request)

        status(result) mustBe OK

        verify(mockAmlsConnector).refreshPaymentStatus(eqTo(payment.reference))(any(), any(), any())
        contentAsString(result) must include(Messages("confirmation.payment.failed.header"))
        contentAsString(result) must include(Messages("confirmation.payment.failed.reason.cancelled"))
      }
    }
  }
}

class ConfirmationNoPaymentsSpec extends GenericTestHelper with MockitoSugar with AmlsReferenceNumberGenerator with PaymentGenerator{

  val paymentsConnector = mock[PayApiConnector]

  override lazy val app: Application = new GuiceApplicationBuilder()
    .disable[com.kenshoo.play.metrics.PlayModule]
    .bindings(bindModules: _*).in(Mode.Test)
    .bindings(bind[PayApiConnector].to(paymentsConnector))
    .configure("microservice.services.feature-toggle.payments-url-lookup" -> false)
    .build()

  trait Fixture extends AuthorisedFixture {
    self =>

    implicit val authContext = mock[AuthContext]
    implicit val headerCarrier = HeaderCarrier()

    val baseUrl = "http://localhost"
    val request = addToken(authRequest).copyFakeRequest(uri = baseUrl)

    val controller = new ConfirmationController {
      override protected val authConnector = self.authConnector
      override private[controllers] val submissionResponseService = mock[SubmissionResponseService]
      override val statusService: StatusService = mock[StatusService]
      override val keystoreConnector = mock[KeystoreConnector]
      override val dataCacheConnector = mock[DataCacheConnector]
      override val amlsConnector = mock[AmlsConnector]
      override val authEnrolmentsService = mock[AuthEnrolmentsService]
    }

    val response = SubscriptionResponse(
      etmpFormBundleNumber = "",
      amlsRefNo = amlsRegistrationNumber,
      Some(SubscriptionFees(
        paymentReference = paymentReferenceNumber,
        registrationFee = 0,
        fpFee = None,
        fpFeeRate = None,
        premiseFee = 0,
        premiseFeeRate = None,
        totalFees = 0
      ))
    )

    protected val mockCacheMap = mock[CacheMap]
    val paymentCookie = Cookie("test", "test-value")

    reset(paymentsConnector)

    when {
      controller.submissionResponseService.getSubscription(any(), any(), any())
    } thenReturn {
      Future.successful((Some(paymentReferenceNumber), Currency.fromInt(0), Seq(), Left(amlsRegistrationNumber)))
    }

    when {
      controller.keystoreConnector.setConfirmationStatus(any(), any())
    } thenReturn Future.successful()

    when {
      controller.authEnrolmentsService.amlsRegistrationNumber(any(), any(), any())
    } thenReturn {
      Future.successful(Some(amlsRegistrationNumber))
    }

    when {
      paymentsConnector.createPayment(any())(any(), any())
    } thenReturn Future.successful(None)

    val defaultPaymentsReturnUrl = ReturnLocation(controllers.routes.ConfirmationController.paymentConfirmation(paymentReferenceNumber))

  }

  "ConfirmationController" must {

    "show the old confirmation screen when the payments url lookup is toggled off" in new Fixture {
      val companyName = "My Test Company"

      val model = BusinessMatching(
        reviewDetails = Some(ReviewDetails(companyName, None, mock[Address], ""))
      )

      val status = SubmissionReadyForReview

      when {
        controller.dataCacheConnector.fetch[BusinessMatching](eqTo(BusinessMatching.key))(any(), any(), any())
      } thenReturn Future.successful(Some(model))

      //noinspection ScalaStyle
      when {
        controller.submissionResponseService.getAmendment(any(), any(), any())
      } thenReturn Future.successful(Some((Some(paymentReferenceNumber), Currency.fromInt(100), Seq(), Some(Currency.fromInt(100)))))

      when {
        controller.statusService.getStatus(any(), any(), any())
      } thenReturn Future.successful(status)

      when {
        controller.submissionResponseService.getSubmissionData(eqTo(status))(any(),any(),any())
      } thenReturn Future.successful(Some((Some(paymentReferenceNumber), Currency.fromInt(0), Seq(), Right(Some(Currency.fromInt(0))))))


      val result = controller.get()(request)
      val body = contentAsString(result)

      verify(paymentsConnector).createPayment(eqTo {
        //noinspection ScalaStyle
        CreatePaymentRequest(
          "other",
          paymentReferenceNumber,
          "AMLS Payment",
          10000,
          ReturnLocation(controllers.routes.ConfirmationController.paymentConfirmation(paymentReferenceNumber)))
      })(any(), any())

      Option(Jsoup.parse(body).select("div.confirmation")).isDefined mustBe true
    }
  }
}
