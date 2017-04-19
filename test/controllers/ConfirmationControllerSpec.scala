package controllers

import cats.implicits._
import connectors.{DataCacheConnector, KeystoreConnector, PaymentsConnector}
import models.SubscriptionResponse
import models.businesscustomer.{Address, ReviewDetails}
import models.businessmatching.BusinessMatching
import models.confirmation.Currency
import models.payments.{PaymentRedirectRequest, PaymentServiceRedirect, ReturnLocation}
import models.status._
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
import services.{StatusService, SubmissionService}
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http.HeaderCarrier
import utils.{AuthorisedFixture, GenericTestHelper}

import scala.concurrent.Future

class ConfirmationControllerSpec extends GenericTestHelper with MockitoSugar {

  val paymentsConnector = mock[PaymentsConnector]

  implicit override lazy val app: Application = new GuiceApplicationBuilder()
    .disable[com.kenshoo.play.metrics.PlayModule]
    .bindings(bindModules: _*).in(Mode.Test)
    .bindings(bind[PaymentsConnector].to(paymentsConnector))
    .configure("Test.microservice.services.feature-toggle.payments-url-lookup" -> true)
    .build()

  trait Fixture extends AuthorisedFixture {

    self =>

    implicit val authContext = mock[AuthContext]
    implicit val headerCarrier = HeaderCarrier()

    val baseUrl = "http://localhost"
    val request = addToken(authRequest).copyFakeRequest(uri = baseUrl)

    val controller = new ConfirmationController {
      override protected val authConnector = self.authConnector
      override private[controllers] val submissionService = mock[SubmissionService]
      override val statusService: StatusService = mock[StatusService]
      override val keystoreConnector = mock[KeystoreConnector]
      override val dataCacheConnector = mock[DataCacheConnector]
    }

    val paymentRefNo = "XA111123451111"

    val response = SubscriptionResponse(
      etmpFormBundleNumber = "",
      amlsRefNo = "",
      registrationFee = 0,
      fpFee = None,
      fpFeeRate = None,
      premiseFee = 0,
      premiseFeeRate = None,
      totalFees = 0,
      paymentReference = paymentRefNo
    )

    protected val mockCacheMap = mock[CacheMap]
    val paymentCookie = Cookie("mdtpp", "test-value")
    val companyName = "My Test Company"

    setupBusinessMatching(companyName)

    reset(paymentsConnector)

    when(controller.submissionService.getSubscription(any(), any(), any()))
      .thenReturn(Future.successful((paymentRefNo, Currency.fromInt(0), Seq())))

    when(controller.keystoreConnector.setConfirmationStatus(any(), any())) thenReturn Future.successful()

    when {
      paymentsConnector.requestPaymentRedirectUrl(any())(any(), any(), any())
    } thenReturn Future.successful(Some(PaymentServiceRedirect("/payments", Seq(paymentCookie))))

    def paymentsReturnLocation(ref: String) = ReturnLocation(controllers.routes.ConfirmationController.paymentConfirmation(ref))(request)

    def setupBusinessMatching(companyName: String) = {
      val model = BusinessMatching(
        reviewDetails = Some(ReviewDetails(companyName, None, mock[Address], ""))
      )

      when {
        controller.dataCacheConnector.fetch[BusinessMatching](eqTo(BusinessMatching.key))(any(), any(), any())
      } thenReturn Future.successful(Some(model))
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

      when(controller.submissionService.getAmendment(any(), any(), any()))
        .thenReturn(Future.successful(Some((Some(paymentRefNo), Currency.fromInt(100), Seq(), Some(Currency.fromInt(100))))))

      setupStatus(SubmissionReadyForReview)

      val result = controller.get()(request)
      val body = contentAsString(result)

      verify(paymentsConnector).requestPaymentRedirectUrl(eqTo(PaymentRedirectRequest(paymentRefNo, 100, paymentsReturnLocation(paymentRefNo))))(any(), any(), any())

      cookies(result) must contain(paymentCookie)

      Jsoup.parse(body).select("a.button").attr("href") mustBe "/payments"
    }

    "query the payments service for the payments url for a variation" in new Fixture {

      when(controller.submissionService.getVariation(any(), any(), any()))
        .thenReturn(Future.successful(Some((Some(paymentRefNo), Currency.fromInt(150), Seq()))))

      setupStatus(SubmissionDecisionApproved)

      val result = controller.get()(request)
      val body = contentAsString(result)

      verify(paymentsConnector).requestPaymentRedirectUrl(eqTo(PaymentRedirectRequest(paymentRefNo, 150, paymentsReturnLocation(paymentRefNo))))(any(), any(), any())

      cookies(result) must contain(paymentCookie)

      Jsoup.parse(body).select("a.button").attr("href") mustBe "/payments"
    }

    "query the payments service for the payments url for a renewal" in new Fixture {

      when(controller.submissionService.getRenewal(any(), any(), any()))
        .thenReturn(Future.successful(Some((Some(paymentRefNo), Currency.fromInt(150), Seq()))))

      setupStatus(ReadyForRenewal(Some(new LocalDate())))

      val result = controller.get()(request)
      val body = contentAsString(result)

      verify(paymentsConnector).requestPaymentRedirectUrl(eqTo(PaymentRedirectRequest(paymentRefNo, 150, paymentsReturnLocation(paymentRefNo))))(any(), any(), any())

      cookies(result) must contain(paymentCookie)

      Jsoup.parse(body).select("a.button").attr("href") mustBe "/payments"
    }

    "query the payments service for the payments url for a new submission" in new Fixture {
      setupStatus(SubmissionReady)

      val result = controller.get()(request)
      val body = contentAsString(result)

      verify(paymentsConnector).requestPaymentRedirectUrl(eqTo(PaymentRedirectRequest(paymentRefNo, 0, paymentsReturnLocation(paymentRefNo))))(any(), any(), any())

      cookies(result) must contain(paymentCookie)

      Jsoup.parse(body).select("a.button").attr("href") mustBe "/payments"
    }

    "return the default configured url for payments if none was returned by the payments service" in new Fixture {
      when(controller.submissionService.getVariation(any(), any(), any()))
        .thenReturn(Future.successful(Some((Some(paymentRefNo), Currency.fromInt(150), Seq()))))

      setupStatus(SubmissionDecisionApproved)

      when(paymentsConnector.requestPaymentRedirectUrl(any())(any(), any(), any())) thenReturn Future.successful(None)

      val result = controller.get()(request)
      val doc = Jsoup.parse(contentAsString(result))

      verify(paymentsConnector).requestPaymentRedirectUrl(eqTo(PaymentRedirectRequest(paymentRefNo, 150, paymentsReturnLocation(paymentRefNo))))(any(), any(), any())

      doc.select(".button").first.attr("href") must include("/pay-online/other-taxes")
    }

    "notify user of progress if application has not already been submitted" in new Fixture {
      setupStatus(SubmissionReady)

      val result = controller.get()(request)
      status(result) mustBe OK
      Jsoup.parse(contentAsString(result)).title must include("You’ve submitted your application")
      contentAsString(result) must include(paymentRefNo)
    }

    "notify user there is no fee" when {

      "an amendment has difference(/Some(0))" in new Fixture {
        setupStatus(SubmissionReadyForReview)

        when(controller.submissionService.getAmendment(any(), any(), any()))
          .thenReturn(Future.successful(Some((Some(paymentRefNo), Currency.fromInt(0), Seq(), Some(Currency.fromInt(0))))))

        val result = controller.get()(request)
        status(result) mustBe OK
        Jsoup.parse(contentAsString(result)).title must include("You’ve submitted your updated information")
        contentAsString(result) must include(Messages("confirmation.no.fee"))
        contentAsString(result) must include(companyName)
      }

      "an amendment has no difference(/None)" in new Fixture {
        setupStatus(SubmissionReadyForReview)

        when(controller.submissionService.getAmendment(any(), any(), any()))
          .thenReturn(Future.successful(Some((Some(paymentRefNo), Currency.fromInt(0), Seq(), None))))

        val result = controller.get()(request)
        status(result) mustBe OK
        Jsoup.parse(contentAsString(result)).title must include("You’ve submitted your updated information")
        contentAsString(result) must include(Messages("confirmation.no.fee"))
        contentAsString(result) must include(companyName)
      }

      "an amendment has no payment reference" in new Fixture {
        setupStatus(SubmissionReadyForReview)

        when(controller.submissionService.getAmendment(any(), any(), any()))
          .thenReturn(Future.successful(Some((None, Currency.fromInt(0), Seq(), None))))

        val result = controller.get()(request)
        status(result) mustBe OK
        Jsoup.parse(contentAsString(result)).title must include("You’ve submitted your updated information")
        contentAsString(result) must include(Messages("confirmation.no.fee"))
        contentAsString(result) must include(companyName)
      }

      "a variation has no payment reference" in new Fixture {
        setupStatus(SubmissionDecisionApproved)

        when(controller.submissionService.getVariation(any(), any(), any()))
          .thenReturn(Future.successful(Some((None, Currency.fromInt(0), Seq()))))

        val result = controller.get()(request)
        status(result) mustBe OK
        Jsoup.parse(contentAsString(result)).title must include("You’ve submitted your updated information")
        contentAsString(result) must include(Messages("confirmation.no.fee"))
        contentAsString(result) must include(companyName)
      }

      "a variation without the addition of tp or rp" in new Fixture {
        setupStatus(SubmissionDecisionApproved)

        when(controller.submissionService.getVariation(any(), any(), any()))
          .thenReturn(Future.successful(Some((Some(""), Currency.fromInt(0), Seq()))))

        val result = controller.get()(request)

        status(result) mustBe OK

        Jsoup.parse(contentAsString(result)).title must include("You’ve submitted your updated information")
        contentAsString(result) must include(Messages("confirmation.no.fee"))
        contentAsString(result) must include(companyName)
      }

      "a renewal has no payment reference" in new Fixture {
        setupStatus(ReadyForRenewal(Some(new LocalDate)))

        when(controller.submissionService.getRenewal(any(), any(), any()))
          .thenReturn(Future.successful(Some((None, Currency.fromInt(0), Seq()))))

        val result = controller.get()(request)
        status(result) mustBe OK
        Jsoup.parse(contentAsString(result)).title must include("You’ve submitted your updated information")
        contentAsString(result) must include(Messages("confirmation.no.fee"))
        contentAsString(result) must include(companyName)
      }

    }

    "show the correct payment confirmation page" when {
      "the application status is 'new submission'" in new Fixture {
        setupStatus(SubmissionReady)

        val paymentReference = "XMHSG357567686"
        val result = controller.paymentConfirmation(paymentReference)(request)

        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))

        doc.select("h1.heading-large").text mustBe Messages("confirmation.payment.lede")
        doc.select(".confirmation").text must include(paymentReference)
        doc.select(".confirmation").text must include(companyName)
      }

      "the application status is 'pending'" in new Fixture {
        setupStatus(SubmissionReadyForReview)

        val paymentReference = "XMHSG3575324788"
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

        val paymentReference = "XH8439483944"
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

        val paymentReference = "XH8439483944"
        val result = controller.paymentConfirmation(paymentReference)(request)

        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))

        doc.title must include(Messages("confirmation.payment.renewal.title"))
        doc.select("h1.heading-large").text mustBe Messages("confirmation.payment.renewal.lede")
        doc.select(".confirmation").text must include(paymentReference)
        doc.select(".confirmation").text must include(companyName)
        contentAsString(result) must include(Messages("confirmation.payment.amendvariation.info.keep_up_to_date"))
      }

      "there is no business name" in new Fixture {
        setupStatus(SubmissionReady)

        val paymentReference = "XMHSG3579873439478"

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

class ConfirmationNoPaymentsSpec extends GenericTestHelper with MockitoSugar {

  val paymentsConnector = mock[PaymentsConnector]

  override lazy val app: Application = new GuiceApplicationBuilder()
    .disable[com.kenshoo.play.metrics.PlayModule]
    .bindings(bindModules: _*).in(Mode.Test)
    .bindings(bind[PaymentsConnector].to(paymentsConnector))
    .configure("Test.microservice.services.feature-toggle.payments-url-lookup" -> false)
    .build()

  trait Fixture extends AuthorisedFixture {

    self =>

    implicit val authContext = mock[AuthContext]
    implicit val headerCarrier = HeaderCarrier()

    val baseUrl = "http://localhost"
    val request = addToken(authRequest).copyFakeRequest(uri = baseUrl)

    val controller = new ConfirmationController {
      override protected val authConnector = self.authConnector
      override private[controllers] val submissionService = mock[SubmissionService]
      override val statusService: StatusService = mock[StatusService]
      override val keystoreConnector = mock[KeystoreConnector]
      override val dataCacheConnector = mock[DataCacheConnector]
    }

    val paymentRefNo = "XA111123451111"

    val response = SubscriptionResponse(
      etmpFormBundleNumber = "",
      amlsRefNo = "",
      registrationFee = 0,
      fpFee = None,
      fpFeeRate = None,
      premiseFee = 0,
      premiseFeeRate = None,
      totalFees = 0,
      paymentReference = paymentRefNo
    )

    protected val mockCacheMap = mock[CacheMap]
    val paymentCookie = Cookie("test", "test-value")

    reset(paymentsConnector)

    when(controller.submissionService.getSubscription(any(), any(), any()))
      .thenReturn(Future.successful((paymentRefNo, Currency.fromInt(0), Seq())))

    when(controller.keystoreConnector.setConfirmationStatus(any(), any())) thenReturn Future.successful()

    when {
      paymentsConnector.requestPaymentRedirectUrl(any())(any(), any(), any())
    } thenReturn Future.successful(Some(PaymentServiceRedirect("/payments", Seq(paymentCookie))))

    val defaultPaymentsReturnUrl = ReturnLocation(controllers.routes.ConfirmationController.paymentConfirmation(paymentRefNo))(request)

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

      when(controller.submissionService.getAmendment(any(), any(), any()))
        .thenReturn(Future.successful(Some((Some(paymentRefNo), Currency.fromInt(100), Seq(), Some(Currency.fromInt(100))))))

      when(controller.statusService.getStatus(any(), any(), any()))
        .thenReturn(Future.successful(SubmissionReadyForReview))

      val result = controller.get()(request)
      val body = contentAsString(result)

      verify(paymentsConnector).requestPaymentRedirectUrl(eqTo(PaymentRedirectRequest(paymentRefNo, 100, defaultPaymentsReturnUrl)))(any(), any(), any())

      cookies(result) must contain(paymentCookie)

      Option(Jsoup.parse(body).select("div.confirmation")).isDefined mustBe true
    }

  }
}
