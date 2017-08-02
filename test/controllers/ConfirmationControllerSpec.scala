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
import generators.AmlsReferenceNumberGenerator
import models.businesscustomer.{Address, ReviewDetails}
import models.businessmatching.BusinessMatching
import models.confirmation.{BreakdownRow, Currency}
import models.payments._
import models.renewal.{InvolvedInOtherNo, Renewal}
import models.status._
import models.{AmendVariationRenewalResponse, SubscriptionFees, SubscriptionResponse}
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
import services.{AuthEnrolmentsService, StatusService, SubmissionResponseService}
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpResponse}
import utils.{AuthorisedFixture, GenericTestHelper}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ConfirmationControllerSpec extends GenericTestHelper with MockitoSugar with AmlsReferenceNumberGenerator{

  val paymentsConnector = mock[PayApiConnector]

  implicit override lazy val app: Application = new GuiceApplicationBuilder()
    .disable[com.kenshoo.play.metrics.PlayModule]
    .bindings(bindModules: _*).in(Mode.Test)
    .bindings(bind[PayApiConnector].to(paymentsConnector))
    .configure("microservice.services.feature-toggle.payments-url-lookup" -> true)
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
      Future.successful((paymentRefNo, Currency.fromInt(0), Seq(), amlsRegistrationNumber))
    }

    when {
      controller.keystoreConnector.setConfirmationStatus(any(), any())
    } thenReturn Future.successful()

    when {
      controller.authEnrolmentsService.amlsRegistrationNumber(any(),any(),any())
    } thenReturn {
      Future.successful(Some(amlsRegistrationNumber))
    }

    when {
      paymentsConnector.createPayment(any())(any(), any())
    } thenReturn Future.successful(Some(CreatePaymentResponse(PayApiLinks("/payments"), Some(amlsRegistrationNumber))))

    when {
      controller.amlsConnector.savePayment(any(),any())(any(),any(),any())
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
    }
  }

  "ConfirmationController" must {

    "write a confirmation value to Keystore" in new Fixture {

      setupStatus(SubmissionReady)

      val result = controller.get()(request)

      status(result) mustBe OK

      verify(controller.keystoreConnector).setConfirmationStatus(any(), any())

    }

    "query the payments service for the payments url for an amendment" in new Fixture {

      //noinspection ScalaStyle
      when(controller.submissionResponseService.getAmendment(any(), any(), any()))
        .thenReturn(Future.successful(Some((Some(paymentRefNo), Currency.fromInt(100), Seq(), Some(Currency.fromInt(100))))))

      setupStatus(SubmissionReadyForReview)

      val result = controller.get()(request)
      val body = contentAsString(result)

      //noinspection ScalaStyle
      verify(paymentsConnector).createPayment(eqTo {
        CreatePaymentRequest("other", paymentRefNo, "AMLS Payment", 10000, paymentsReturnLocation(paymentRefNo))
      })(any(), any())

      Jsoup.parse(body).select("a.button").attr("href") mustBe "/payments"
    }

    "query the payments service for the payments url for a variation" in new Fixture {

      //noinspection ScalaStyle
      when(controller.submissionResponseService.getVariation(any(), any(), any()))
        .thenReturn(Future.successful(Some(Some(paymentRefNo), Currency.fromInt(150), Seq(), None)))

      setupStatus(SubmissionDecisionApproved)

      val result = controller.get()(request)
      val body = contentAsString(result)

      //noinspection ScalaStyle
      verify(paymentsConnector).createPayment(eqTo {
        CreatePaymentRequest("other", paymentRefNo, "AMLS Payment", 15000, paymentsReturnLocation(paymentRefNo))
      })(any(), any())

      Jsoup.parse(body).select("a.button").attr("href") mustBe "/payments"
    }

    "query the payments service for the payments url for a renewal" in new Fixture {

      when {
        controller.dataCacheConnector.fetch[Renewal](eqTo(Renewal.key))(any(), any(), any())
      } thenReturn Future.successful(Some(Renewal(Some(InvolvedInOtherNo))))

      when{
        controller.submissionResponseService.getRenewal(any(), any(), any())
      } thenReturn Future.successful(Some((Some(paymentRefNo), Currency.fromInt(150), Seq(), None)))

      setupStatus(ReadyForRenewal(Some(new LocalDate())))

      val result = controller.get()(request)
      val body = contentAsString(result)

      verify(paymentsConnector).createPayment(eqTo {
        //noinspection ScalaStyle
        CreatePaymentRequest("other", paymentRefNo, "AMLS Payment", amountInPence = 15000, paymentsReturnLocation(paymentRefNo))
      })(any(), any())

      Jsoup.parse(body).select("a.button").attr("href") mustBe "/payments"
    }

    "query the payments service for the payments url for a new submission" in new Fixture {
      setupStatus(SubmissionReady)

      val result = controller.get()(request)
      val body = contentAsString(result)

      verify(paymentsConnector).createPayment(eqTo {
        CreatePaymentRequest("other", paymentRefNo, "AMLS Payment", amountInPence = 0, paymentsReturnLocation(paymentRefNo))
      })(any(), any())

      Jsoup.parse(body).select("a.button").attr("href") mustBe "/payments"
    }

    "return the default configured url for payments if none was returned by the payments service" in new Fixture {
      //noinspection ScalaStyle
      when {
        controller.submissionResponseService.getVariation(any(), any(), any())
      } thenReturn Future.successful(Some(Some(paymentRefNo), Currency.fromInt(150), Seq(), None))

      setupStatus(SubmissionDecisionApproved)

      when(paymentsConnector.createPayment(any())(any(), any())) thenReturn Future.successful(None)

      val result = controller.get()(request)
      val doc = Jsoup.parse(contentAsString(result))

      verify(paymentsConnector).createPayment(eqTo {
        //noinspection ScalaStyle
        CreatePaymentRequest("other", paymentRefNo, "AMLS Payment", amountInPence = 15000, paymentsReturnLocation(paymentRefNo))
      })(any(), any())

      doc.select(".button").first.attr("href") must include("/pay-online/other-taxes")
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
        setupStatus(SubmissionReadyForReview)

        when(controller.submissionResponseService.getAmendment(any(), any(), any()))
          .thenReturn(Future.successful(Some((Some(paymentRefNo), Currency.fromInt(0), Seq(), Some(Currency.fromInt(0))))))

        val result = controller.get()(request)
        status(result) mustBe OK
        Jsoup.parse(contentAsString(result)).title must include("You’ve submitted your updated information")
        contentAsString(result) must include(Messages("confirmation.no.fee"))
        contentAsString(result) must include(companyName)
      }

      "an amendment has no difference(/None)" in new Fixture {
        setupStatus(SubmissionReadyForReview)

        when(controller.submissionResponseService.getAmendment(any(), any(), any()))
          .thenReturn(Future.successful(Some((Some(paymentRefNo), Currency.fromInt(0), Seq(), None))))

        val result = controller.get()(request)
        status(result) mustBe OK
        Jsoup.parse(contentAsString(result)).title must include("You’ve submitted your updated information")
        contentAsString(result) must include(Messages("confirmation.no.fee"))
        contentAsString(result) must include(companyName)
      }

      "an amendment has no payment reference" in new Fixture {
        setupStatus(SubmissionReadyForReview)

        when(controller.submissionResponseService.getAmendment(any(), any(), any()))
          .thenReturn(Future.successful(Some((None, Currency.fromInt(0), Seq(), None))))

        val result = controller.get()(request)
        status(result) mustBe OK
        Jsoup.parse(contentAsString(result)).title must include("You’ve submitted your updated information")
        contentAsString(result) must include(Messages("confirmation.no.fee"))
        contentAsString(result) must include(companyName)
      }

      "a variation has no payment reference" in new Fixture {
        setupStatus(SubmissionDecisionApproved)

        when(controller.submissionResponseService.getVariation(any(), any(), any()))
          .thenReturn(Future.successful(Some(Some(""), Currency.fromInt(0), Seq(), None)))

        val result = controller.get()(request)
        status(result) mustBe OK
        Jsoup.parse(contentAsString(result)).title must include("You’ve submitted your updated information")
        contentAsString(result) must include(Messages("confirmation.no.fee"))
        contentAsString(result) must include(companyName)
      }

      "a variation without the addition of tp or rp" in new Fixture {
        setupStatus(SubmissionDecisionApproved)

        when(controller.submissionResponseService.getVariation(any(), any(), any()))
          .thenReturn(Future.successful(Some(Some(""), Currency.fromInt(0), Seq(), None)))

        val result = controller.get()(request)

        status(result) mustBe OK

        Jsoup.parse(contentAsString(result)).title must include("You’ve submitted your updated information")
        contentAsString(result) must include(Messages("confirmation.no.fee"))
        contentAsString(result) must include(companyName)
      }

      "a variation when status is ready for renewal and no renewal data in save4later" in new Fixture {
        setupStatus(ReadyForRenewal(Some(new LocalDate)))

        when(controller.submissionResponseService.getVariation(any(), any(), any()))
          .thenReturn(Future.successful(Some(Some(""), Currency.fromInt(0), Seq(), None)))

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

        when(controller.submissionResponseService.getRenewal(any(), any(), any()))
          .thenReturn(Future.successful(Some((None, Currency.fromInt(0), Seq(), None))))

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

        when(controller.submissionResponseService.getRenewal(any(), any(), any()))
          .thenReturn(Future.successful(Some((Some("payeref"), Currency.fromInt(100000), Seq(BreakdownRow("",10, Currency(10), Currency(10))), None))))


        val result = controller.get()(request)
        status(result) mustBe OK
        Jsoup.parse(contentAsString(result)).title must include(Messages("confirmation.renewal.title"))
      }

      "a renewal status and has 1 FP RP and 1 Not FP RP then load renewal confirmation showing each row with respective costs" in new Fixture {

        setupStatus(ReadyForRenewal(Some(new LocalDate)))

        when {
          controller.dataCacheConnector.fetch[Renewal](eqTo(Renewal.key))(any(), any(), any())
        } thenReturn Future.successful(Some(Renewal()))

        when(controller.submissionResponseService.getRenewal(any(), any(), any()))
          .thenReturn(Future.successful(Some((
            Some("payeref"),
            Currency.fromInt(100),
            Seq(
              BreakdownRow("confirmation.responsiblepeople.fp.passed",1, Currency(0), Currency(0)),
              BreakdownRow("confirmation.responsiblepeople",1, Currency(100), Currency(100)),
              BreakdownRow("confirmation.tradingpremises.half",2, Currency(50), Currency(100))
            ), None))))


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

        when(controller.submissionResponseService.getVariation(any(), any(), any()))
          .thenReturn(Future.successful(Some(Some("payeref"), Currency.fromInt(100000), Seq(BreakdownRow("",10, Currency(10), Currency(10))), None)))


        val result = controller.get()(request)
        status(result) mustBe OK
        Jsoup.parse(contentAsString(result)).title must include(Messages("confirmation.amendment.header"))
      }

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

    }
  }
}

class ConfirmationNoPaymentsSpec extends GenericTestHelper with MockitoSugar with AmlsReferenceNumberGenerator {

  val paymentsConnector = mock[PayApiConnector]

  override lazy val app: Application = new GuiceApplicationBuilder()
    .disable[com.kenshoo.play.metrics.PlayModule]
    .bindings(bindModules: _*).in(Mode.Test)
    .bindings(bind[PayApiConnector].to(paymentsConnector))
    .configure("microservice.services.feature-toggle.payments-url-lookup" -> false)
    .build()

  trait Fixture extends AuthorisedFixture { self =>

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
    val paymentCookie = Cookie("test", "test-value")

    reset(paymentsConnector)

    when{
      controller.submissionResponseService.getSubscription(any(), any(), any())
    } thenReturn {
      Future.successful((paymentRefNo, Currency.fromInt(0), Seq(), amlsRegistrationNumber))
    }

    when {
      controller.keystoreConnector.setConfirmationStatus(any(), any())
    } thenReturn Future.successful()

    when {
      controller.authEnrolmentsService.amlsRegistrationNumber(any(),any(),any())
    } thenReturn {
      Future.successful(Some(amlsRegistrationNumber))
    }

    when {
      paymentsConnector.createPayment(any())(any(), any())
    } thenReturn Future.successful(None)

    val defaultPaymentsReturnUrl = ReturnLocation(controllers.routes.ConfirmationController.paymentConfirmation(paymentRefNo))

  }

  "ConfirmationController" must {

    "show the old confirmation screen when the payments url lookup is toggled off" in new Fixture {

      val companyName = "My Test Company"

      val model = BusinessMatching(
        reviewDetails = Some(ReviewDetails(companyName, None, mock[Address], ""))
      )

      when {
        controller.dataCacheConnector.fetch[BusinessMatching](eqTo(BusinessMatching.key))(any(), any(), any())
      } thenReturn Future.successful(Some(model))

      //noinspection ScalaStyle
      when(controller.submissionResponseService.getAmendment(any(), any(), any()))
        .thenReturn(Future.successful(Some((Some(paymentRefNo), Currency.fromInt(100), Seq(), Some(Currency.fromInt(100))))))

      when(controller.statusService.getStatus(any(), any(), any()))
        .thenReturn(Future.successful(SubmissionReadyForReview))

      val result = controller.get()(request)
      val body = contentAsString(result)

      verify(paymentsConnector).createPayment(eqTo {
        //noinspection ScalaStyle
        CreatePaymentRequest(
          "other",
          paymentRefNo,
          "AMLS Payment",
          10000,
          ReturnLocation(controllers.routes.ConfirmationController.paymentConfirmation(paymentRefNo)))
      })(any(), any())

      Option(Jsoup.parse(body).select("div.confirmation")).isDefined mustBe true
    }
  }
}
