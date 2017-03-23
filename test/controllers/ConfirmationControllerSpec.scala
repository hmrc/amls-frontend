package controllers

import connectors.{AuthenticatorConnector, DataCacheConnector, KeystoreConnector, PaymentsConnector}
import models.SubscriptionResponse
import models.businesscustomer.{Address, ReviewDetails}
import models.businessmatching.BusinessMatching
import models.confirmation.Currency
import models.payments.{PaymentRedirectRequest, PaymentServiceRedirect, ReturnLocation}
import models.status.{SubmissionDecisionApproved, SubmissionReady, SubmissionReadyForReview}
import org.jsoup.Jsoup
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.i18n.Messages
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.Cookie
import play.api.test.Helpers._
import play.api.{Application, Mode}
import services.{StatusService, SubmissionService}
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpResponse}
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

    reset(paymentsConnector)

    when(controller.submissionService.getSubscription(any(), any(), any()))
      .thenReturn(Future.successful((paymentRefNo, Currency.fromInt(0), Seq())))

    when(controller.keystoreConnector.setConfirmationStatus(any(), any())) thenReturn Future.successful()

    when {
      paymentsConnector.requestPaymentRedirectUrl(any())(any(), any(), any())
    } thenReturn Future.successful(Some(PaymentServiceRedirect("/payments", Seq(paymentCookie))))

    val defaultPaymentsReturnUrl = ReturnLocation(controllers.routes.LandingController.get())(request)

  }

  "ConfirmationController" must {

    "write a confirmation value to Keystore" in new Fixture {

      when(controller.statusService.getStatus(any(), any(), any()))
        .thenReturn(Future.successful(SubmissionReady))

      val result = controller.get()(request)

      status(result) mustBe OK

      verify(controller.keystoreConnector).setConfirmationStatus(any(), any())

    }

    "query the payments service for the payments url for an amendment" in new Fixture {

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

      Jsoup.parse(body).select("a.button").attr("href") mustBe "/payments"
    }

    "query the payments service for the payments url for a variation" in new Fixture {

      val companyName = "My Test Company"

      val model = BusinessMatching(
        reviewDetails = Some(ReviewDetails(companyName, None, mock[Address], ""))
      )

      when {
        controller.dataCacheConnector.fetch[BusinessMatching](eqTo(BusinessMatching.key))(any(), any(), any())
      } thenReturn Future.successful(Some(model))

      when(controller.submissionService.getVariation(any(), any(), any()))
        .thenReturn(Future.successful(Some((Some(paymentRefNo), Currency.fromInt(150), Seq()))))

      when(controller.statusService.getStatus(any(), any(), any()))
        .thenReturn(Future.successful(SubmissionDecisionApproved))

      val result = controller.get()(request)
      val body = contentAsString(result)

      verify(paymentsConnector).requestPaymentRedirectUrl(eqTo(PaymentRedirectRequest(paymentRefNo, 150, defaultPaymentsReturnUrl)))(any(), any(), any())

      cookies(result) must contain(paymentCookie)

      Jsoup.parse(body).select("a.button").attr("href") mustBe "/payments"
    }

    "query the payments service for the payments url for a new submission" in new Fixture {

      when(controller.statusService.getStatus(any(), any(), any()))
        .thenReturn(Future.successful(SubmissionReady))

      val result = controller.get()(request)
      val body = contentAsString(result)
      val submissionReturnUrl = ReturnLocation(controllers.routes.ConfirmationController.paymentConfirmation(paymentRefNo))(request)

      verify(paymentsConnector).requestPaymentRedirectUrl(eqTo(PaymentRedirectRequest(paymentRefNo, 0, submissionReturnUrl)))(any(), any(), any())

      cookies(result) must contain(paymentCookie)

      Jsoup.parse(body).select("a.button").attr("href") mustBe "/payments"
    }

    "return the default configured url for payments if none was returned by the payments service" in new Fixture {

      val companyName = "My Test Company"

      val model = BusinessMatching(
        reviewDetails = Some(ReviewDetails(companyName, None, mock[Address], ""))
      )

      when {
        controller.dataCacheConnector.fetch[BusinessMatching](eqTo(BusinessMatching.key))(any(), any(), any())
      } thenReturn Future.successful(Some(model))

      when(controller.submissionService.getVariation(any(), any(), any()))
        .thenReturn(Future.successful(Some((Some(paymentRefNo), Currency.fromInt(150), Seq()))))

      when(controller.statusService.getStatus(any(), any(), any()))
        .thenReturn(Future.successful(SubmissionDecisionApproved))

      when(paymentsConnector.requestPaymentRedirectUrl(any())(any(), any(), any())) thenReturn Future.successful(None)

      val result = await(controller.get()(request))

      verify(paymentsConnector).requestPaymentRedirectUrl(eqTo(PaymentRedirectRequest(paymentRefNo, 150, defaultPaymentsReturnUrl)))(any(), any(), any())
    }

    "notify user of progress if application has not already been submitted" in new Fixture {

      when(controller.statusService.getStatus(any(), any(), any()))
        .thenReturn(Future.successful(SubmissionReady))

      val result = controller.get()(request)
      status(result) mustBe OK
      Jsoup.parse(contentAsString(result)).title must include("You’ve submitted your application")
      contentAsString(result) must include(paymentRefNo)
    }

    "notify user of amendment if application has already been submitted but not approved with difference" in new Fixture {

      val companyName = "My Test Company"

      val model = BusinessMatching(
        reviewDetails = Some(ReviewDetails(companyName, None, mock[Address], ""))
      )

      when {
        controller.dataCacheConnector.fetch[BusinessMatching](eqTo(BusinessMatching.key))(any(), any(), any())
      } thenReturn Future.successful(Some(model))

      when(controller.statusService.getStatus(any(), any(), any()))
        .thenReturn(Future.successful(SubmissionReadyForReview))

      when(controller.submissionService.getAmendment(any(), any(), any()))
        .thenReturn(Future.successful(Some((Some(paymentRefNo), Currency.fromInt(0), Seq(), Some(Currency.fromInt(100))))))

      val result = controller.get()(request)
      status(result) mustBe OK
      Jsoup.parse(contentAsString(result)).title must include("You’ve submitted an updated application")
      contentAsString(result) must include(Messages("confirmation.amendment.fee"))
      contentAsString(result) must include(Messages("confirmation.amendment.thankyou.p"))
      contentAsString(result) must include(Messages("confirmation.amendment.previousfees.p"))
      contentAsString(result) must include(paymentRefNo)
    }

    "notify user of variation if application has been submitted and approved and fees have been accrued" in new Fixture {

      val companyName = "My Test Company"

      val model = BusinessMatching(
        reviewDetails = Some(ReviewDetails(companyName, None, mock[Address], ""))
      )

      when {
        controller.dataCacheConnector.fetch[BusinessMatching](eqTo(BusinessMatching.key))(any(), any(), any())
      } thenReturn Future.successful(Some(model))

      when(controller.statusService.getStatus(any(), any(), any()))
        .thenReturn(Future.successful(SubmissionDecisionApproved))

      when(controller.submissionService.getVariation(any(), any(), any()))
        .thenReturn(Future.successful(Some((Some(paymentRefNo), Currency.fromInt(100), Seq()))))

      val result = controller.get()(request)
      status(result) mustBe OK
      Jsoup.parse(contentAsString(result)).title must include("You’ve submitted your updated information")
      contentAsString(result) must include(Messages("confirmation.amendment.fee"))
      contentAsString(result) must include(Messages("confirmation.amendment.thankyou.p"))
      contentAsString(result) must include(Messages("confirmation.amendment.previousfees.p"))
      contentAsString(result) must include(paymentRefNo)
    }

    "notify user there is no fee" when {

      "an amendment has difference(/Some(0))" in new Fixture {

        val companyName = "My Test Company"

        val model = BusinessMatching(
          reviewDetails = Some(ReviewDetails(companyName, None, mock[Address], ""))
        )

        when {
          controller.dataCacheConnector.fetch[BusinessMatching](eqTo(BusinessMatching.key))(any(), any(), any())
        } thenReturn Future.successful(Some(model))

        when(controller.statusService.getStatus(any(), any(), any()))
          .thenReturn(Future.successful(SubmissionReadyForReview))

        when(controller.submissionService.getAmendment(any(), any(), any()))
          .thenReturn(Future.successful(Some((Some(paymentRefNo), Currency.fromInt(0), Seq(), Some(Currency.fromInt(0))))))

        val result = controller.get()(request)
        status(result) mustBe OK
        Jsoup.parse(contentAsString(result)).title must include("You’ve submitted your updated information")
        contentAsString(result) must include(Messages("confirmation.no.fee"))
        contentAsString(result) must include(companyName)
      }
      "an amendment has no difference(/None)" in new Fixture {

        val companyName = "My Test Company"

        val model = BusinessMatching(
          reviewDetails = Some(ReviewDetails(companyName, None, mock[Address], ""))
        )

        when {
          controller.dataCacheConnector.fetch[BusinessMatching](eqTo(BusinessMatching.key))(any(), any(), any())
        } thenReturn Future.successful(Some(model))

        when(controller.statusService.getStatus(any(), any(), any()))
          .thenReturn(Future.successful(SubmissionReadyForReview))

        when(controller.submissionService.getAmendment(any(), any(), any()))
          .thenReturn(Future.successful(Some((Some(paymentRefNo), Currency.fromInt(0), Seq(), None))))

        val result = controller.get()(request)
        status(result) mustBe OK
        Jsoup.parse(contentAsString(result)).title must include("You’ve submitted your updated information")
        contentAsString(result) must include(Messages("confirmation.no.fee"))
        contentAsString(result) must include(companyName)
      }

      "an amendment has no payment reference" in new Fixture {

        val companyName = "My Test Company"

        val model = BusinessMatching(
          reviewDetails = Some(ReviewDetails(companyName, None, mock[Address], ""))
        )

        when {
          controller.dataCacheConnector.fetch[BusinessMatching](eqTo(BusinessMatching.key))(any(), any(), any())
        } thenReturn Future.successful(Some(model))

        when(controller.statusService.getStatus(any(), any(), any()))
          .thenReturn(Future.successful(SubmissionReadyForReview))

        when(controller.submissionService.getAmendment(any(), any(), any()))
          .thenReturn(Future.successful(Some((None, Currency.fromInt(0), Seq(), None))))

        val result = controller.get()(request)
        status(result) mustBe OK
        Jsoup.parse(contentAsString(result)).title must include("You’ve submitted your updated information")
        contentAsString(result) must include(Messages("confirmation.no.fee"))
        contentAsString(result) must include(companyName)
      }

      "a variation has no payment reference" in new Fixture {

        val companyName = "My Test Company"

        val model = BusinessMatching(
          reviewDetails = Some(ReviewDetails(companyName, None, mock[Address], ""))
        )

        when {
          controller.dataCacheConnector.fetch[BusinessMatching](eqTo(BusinessMatching.key))(any(), any(), any())
        } thenReturn Future.successful(Some(model))

        when(controller.statusService.getStatus(any(), any(), any()))
          .thenReturn(Future.successful(SubmissionDecisionApproved))

        when(controller.submissionService.getVariation(any(), any(), any()))
          .thenReturn(Future.successful(Some((None, Currency.fromInt(0), Seq()))))

        val result = controller.get()(request)
        status(result) mustBe OK
        Jsoup.parse(contentAsString(result)).title must include("You’ve submitted your updated information")
        contentAsString(result) must include(Messages("confirmation.no.fee"))
        contentAsString(result) must include(companyName)
      }

      "a variation without the addition of tp or rp" in new Fixture {

        val companyName = "My Test Company"

        val model = BusinessMatching(
          reviewDetails = Some(ReviewDetails(companyName, None, mock[Address], ""))
        )

        when {
          controller.dataCacheConnector.fetch[BusinessMatching](eqTo(BusinessMatching.key))(any(), any(), any())
        } thenReturn Future.successful(Some(model))

        when(controller.statusService.getStatus(any(), any(), any()))
          .thenReturn(Future.successful(SubmissionDecisionApproved))

        when(controller.submissionService.getVariation(any(), any(), any()))
          .thenReturn(Future.successful(Some((Some(""), Currency.fromInt(0), Seq()))))

        val result = controller.get()(request)
        status(result) mustBe OK
        Jsoup.parse(contentAsString(result)).title must include("You’ve submitted your updated information")
        contentAsString(result) must include(Messages("confirmation.no.fee"))
        contentAsString(result) must include(companyName)
      }

    }

    "be able to show the payments confirmation page" in new Fixture {

      val paymentReference = "XMHSG357567686"
      val companyName = "My Test Company"

      val model = BusinessMatching(
        reviewDetails = Some(ReviewDetails(companyName, None, mock[Address], ""))
      )

      when {
        controller.dataCacheConnector.fetch[BusinessMatching](eqTo(BusinessMatching.key))(any(), any(), any())
      } thenReturn Future.successful(Some(model))

      val result = controller.paymentConfirmation(paymentReference)(request)

      status(result) must be(OK)

      contentAsString(result) must include(paymentReference)
      contentAsString(result) must include(companyName)
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

    val defaultPaymentsReturnUrl = ReturnLocation(controllers.routes.LandingController.get().url)(request)

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
