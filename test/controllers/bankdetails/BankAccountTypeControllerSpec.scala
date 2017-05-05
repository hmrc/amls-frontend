package controllers.bankdetails

import connectors.DataCacheConnector
import models.bankdetails._
import models.status.{SubmissionDecisionApproved, SubmissionReady, SubmissionReadyForReview}
import org.jsoup.Jsoup
import org.mockito.Matchers
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.matchers.Matcher
import org.scalatest.mock.MockitoSugar
import  utils.GenericTestHelper
import play.api.i18n.Messages
import play.api.test.Helpers._
import services.StatusService
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.{AuthorisedFixture, StatusConstants}

import scala.concurrent.Future

class BankAccountTypeControllerSpec extends GenericTestHelper with MockitoSugar {

  trait Fixture extends AuthorisedFixture {
    self => val request = addToken(authRequest)

    val controller = new BankAccountTypeController {
      override val dataCacheConnector = mock[DataCacheConnector]
      override val authConnector = self.authConnector
      override implicit val statusService = mock[StatusService]
    }
  }

  val emptyCache = CacheMap("", Map.empty)

  "BankAccountTypeController" when {
    "get:" must {
      "respond with OK and display the blank 'bank account type' page" when {
        "there is no bank account type information yet" in new Fixture {

          when(controller.dataCacheConnector.fetch[Seq[BankDetails]](any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(Seq(BankDetails(None, None, status = Some(StatusConstants.Deleted))))))
          when(controller.statusService.getStatus(any(),any(),any()))
            .thenReturn(Future.successful(SubmissionReady))

          val result = controller.get(1, false)(request)

          status(result) must be(OK)
          contentAsString(result) must include(Messages("bankdetails.accounttype.title"))
          val document = Jsoup.parse(contentAsString(result))

          document.select("input[type=radio][name=bankAccountType][value=01]").hasAttr("checked") must be(false)
          document.select("input[type=radio][name=bankAccountType][value=02]").hasAttr("checked") must be(false)
          document.select("input[type=radio][name=bankAccountType][value=03]").hasAttr("checked") must be(false)
          document.select("input[type=radio][name=bankAccountType][value=04]").hasAttr("checked") must be(false)
          document.select("input[type=radio][name=bankAccountType]").size() must be(4)
        }

        "load bank account type UI with out the option 'user does not have bank account'" when {
          "user alreday added an account" in new Fixture {

            when(controller.dataCacheConnector.fetch[Seq[BankDetails]](any())(any(), any(), any()))
              .thenReturn(Future.successful(Some(Seq(BankDetails(Some(PersonalAccount)), BankDetails(Some(PersonalAccount))))))
            when(controller.statusService.getStatus(any(),any(),any()))
              .thenReturn(Future.successful(SubmissionReady))

            val result = controller.get(2, false)(request)

            status(result) must be(OK)
            contentAsString(result) must include(Messages("bankdetails.accounttype.title"))
            val document = Jsoup.parse(contentAsString(result))

            document.select("input[type=radio][name=bankAccountType][value=01]").hasAttr("checked") must be(true)
            document.select("input[type=radio][name=bankAccountType][value=02]").hasAttr("checked") must be(false)
            document.select("input[type=radio][name=bankAccountType]").size() must be(3)

          }
        }

        "there is already a bank account type" in new Fixture {

          when(controller.dataCacheConnector.fetch[Seq[BankDetails]](any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(Seq(BankDetails(Some(PersonalAccount), None)))))
          when(controller.statusService.getStatus(any(),any(),any()))
            .thenReturn(Future.successful(SubmissionReady))

          val result = controller.get(1)(request)
          val document = Jsoup.parse(contentAsString(result)).select("input[value=01]").hasAttr("checked")

          status(result) must be(OK)
          document must be(true)
        }
      }

      "respond with NOT_FOUND" when {
        "there is no bank account information at all" in new Fixture {

          when(controller.dataCacheConnector.fetch[Seq[BankDetails]](any())(any(), any(), any()))
            .thenReturn(Future.successful(None))
          when(controller.statusService.getStatus(any(),any(),any()))
            .thenReturn(Future.successful(SubmissionReady))

          val result = controller.get(1, false)(request)

          status(result) must be(NOT_FOUND)
        }
        "editing an amendment" in new Fixture {

          when(controller.dataCacheConnector.fetch[Seq[BankDetails]](any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(Seq(BankDetails(Some(PersonalAccount)), BankDetails(Some(PersonalAccount))))))
          when(controller.statusService.getStatus(any(),any(),any()))
            .thenReturn(Future.successful(SubmissionReadyForReview))

          val result = controller.get(1, true)(request)

          status(result) must be(NOT_FOUND)
        }
        "editing an variation" in new Fixture {

          when(controller.dataCacheConnector.fetch[Seq[BankDetails]](any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(Seq(BankDetails(Some(PersonalAccount)), BankDetails(Some(PersonalAccount))))))
          when(controller.statusService.getStatus(any(),any(),any()))
            .thenReturn(Future.successful(SubmissionDecisionApproved))

          val result = controller.get(1, true)(request)

          status(result) must be(NOT_FOUND)
        }

      }
    }

    "post is called" must {
      "resopnd with OK and redirect to the bank account details page" when {

        "not editing and there is valid account type" in new Fixture {
          val newRequest = request.withFormUrlEncodedBody(
            "bankAccountType" -> "01"
          )

          when(controller.dataCacheConnector.fetch[Seq[BankDetails]](any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(Seq(BankDetails(Some(PersonalAccount), None)))))
          when(controller.dataCacheConnector.save[Seq[BankDetails]](any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(emptyCache))

          val result = controller.post(1, false)(newRequest)

          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(routes.BankAccountController.get(1, false).url))
        }

          "not editing and there is no bank account" in new Fixture {
          val newRequest = request.withFormUrlEncodedBody(
            "bankAccountType" -> "04" // something weird going on here?
          )

          when(controller.dataCacheConnector.fetch[Seq[BankDetails]](any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(Seq(BankDetails(None, None)))))
          when(controller.dataCacheConnector.save[Seq[BankDetails]](any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(emptyCache))

          val result = controller.post(1, false)(newRequest)

          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(routes.SummaryController.get(false).url))
        }

        "editing and there is valid account type but no account details" in new Fixture {
          val newRequest = request.withFormUrlEncodedBody(
            "bankAccountType" -> "01"
          )

          when(controller.dataCacheConnector.fetch[Seq[BankDetails]](any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(Seq(BankDetails(Some(PersonalAccount), None)))))
          when(controller.dataCacheConnector.save[Seq[BankDetails]](any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(emptyCache))

          val result = controller.post(1, true)(newRequest)

          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(routes.BankAccountController.get(1, true).url))
        }

        "editing and there is both a valid account type and valid account details" in new Fixture {
          val newRequest = request.withFormUrlEncodedBody(
            "bankAccountType" -> "01"
          )

          when(controller.dataCacheConnector.fetch[Seq[BankDetails]](any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(Seq(BankDetails(
              Some(PersonalAccount),
              Some(BankAccount("AccountName", UKAccount("12341234", "000000"))))))))
          when(controller.dataCacheConnector.save[Seq[BankDetails]](any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(emptyCache))

          val result = controller.post(1, true)(newRequest)

          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(routes.BankAccountController.get(1, true).url))
        }
      }


      "respond with BAD_REQUEST" when {
        "there is invalid data" in new Fixture {
          val newRequest = request.withFormUrlEncodedBody(
            "bankAccountType" -> "10"
          )

          when(controller.dataCacheConnector.fetch[Seq[BankDetails]](any())(any(), any(), any()))
            .thenReturn(Future.successful(None))
          when(controller.dataCacheConnector.save[Seq[BankDetails]](any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(emptyCache))

          val result = controller.post(0, false)(newRequest)
          val document = Jsoup.parse(contentAsString(result)).select("span").html()

          status(result) must be(BAD_REQUEST)
          document must include("Invalid value")
        }
      }

      "respond with NOT_FOUND" when {
        "the given index is out of bounds" in new Fixture {
          val newRequest = request.withFormUrlEncodedBody(
            "bankAccountType" -> "04"
          )

          when(controller.statusService.getStatus(any(),any(),any()))
            .thenReturn(Future.successful(SubmissionDecisionApproved))
          when(controller.dataCacheConnector.fetch[Seq[BankDetails]](any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(Seq(BankDetails(None, None)))))
          when(controller.dataCacheConnector.save[Seq[BankDetails]](any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(emptyCache))

          val result = controller.post(3, false)(newRequest)

          status(result) must be(NOT_FOUND)
        }
      }
    }
  }
}
