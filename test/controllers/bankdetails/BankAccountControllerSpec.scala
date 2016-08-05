package controllers.bankdetails

import connectors.DataCacheConnector
import models.bankdetails._
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.AuthorisedFixture

import scala.concurrent.Future

class BankAccountControllerSpec extends PlaySpec with OneAppPerSuite with MockitoSugar {

  trait Fixture extends AuthorisedFixture {
    self =>
    val controller = new BankAccountController {
      override val dataCacheConnector = mock[DataCacheConnector]
      override val authConnector = self.authConnector
    }
  }

  val emptyCache = CacheMap("", Map.empty)
  val fieldElements = Array("accountName", "accountNumber", "sortCode", "IBANNumber")

  "BankAccountController" when {
    "get is called" must {
      "respond with OK" when {
        "there is no bank account detail information yet" in new Fixture {
          when(controller.dataCacheConnector.fetch[Seq[BankDetails]](any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(Seq(BankDetails(None, None)))))

          val result = controller.get(1, false)(request)
          val document: Document = Jsoup.parse(contentAsString(result))

          status(result) must be(OK)
          for (field <- fieldElements)
            document.select(s"input[name=$field]").`val` must be(empty)
        }

        "there is already bank account detail information" in new Fixture {
          val ukBankAccount = BankAccount("My Account", UKAccount("12345678", "202502"))

          when(controller.dataCacheConnector.fetch[Seq[BankDetails]](any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(Seq(BankDetails(None, Some(ukBankAccount))))))

          val result = controller.get(1, true)(request)
          status(result) must be(OK)
          // check the radio buttons are checked
        }
      }

      "respond with NOT_FOUND" when {
        "there is no bank account information at all" in new Fixture {
          when(controller.dataCacheConnector.fetch[Seq[BankDetails]](any())(any(), any(), any()))
            .thenReturn(Future.successful(None))

          val result = controller.get(1, false)(request)

          status(result) must be(NOT_FOUND)
        }
      }
    }

    "post is called" must {
      "respond with SEE_OTHER" when {
        "given valid data in edit mode" in new Fixture {

          val newRequest = request.withFormUrlEncodedBody(
            "accountName" -> "test",
            "isUK" -> "false",
            "nonUKAccountNumber" -> "1234567890123456789012345678901234567890",
            "isIBAN" -> "false"
          )

          when(controller.dataCacheConnector.fetch[Seq[BankDetails]](any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(Seq(BankDetails(Some(PersonalAccount), None)))))
          when(controller.dataCacheConnector.save[Seq[BankDetails]](any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(emptyCache))

          val result = controller.post(1, true)(newRequest)

          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(routes.SummaryController.get().url))
        }
        "blahblah given valid data in edit mode" in new Fixture {

          val newRequest = request.withFormUrlEncodedBody(
            "accountName" -> "test",
            "isUK" -> "false",
            "nonUKAccountNumber" -> "1234567890123456789012345678901234567890",
            "isIBAN" -> "false"
          )

          when(controller.dataCacheConnector.fetch[Seq[BankDetails]](any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(Seq(BankDetails(None, None)))))
          when(controller.dataCacheConnector.save[Seq[BankDetails]](any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(emptyCache))

          val result = controller.post(1, true)(newRequest)

          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(routes.SummaryController.get().url))
        }
      }

      "respond with NOT_FOUND" when {
        "given valid data in edit mode" in new Fixture {

          val newRequest = request.withFormUrlEncodedBody(
            "accountName" -> "test",
            "isUK" -> "false",
            "nonUKAccountNumber" -> "1234567890123456789012345678901234567890",
            "isIBAN" -> "false"
          )

          when(controller.dataCacheConnector.fetch[Seq[BankDetails]](any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(Seq(BankDetails(None, None)))))
          when(controller.dataCacheConnector.save[Seq[BankDetails]](any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(emptyCache))

          val result = controller.post(50, true)(newRequest)

          status(result) must be(NOT_FOUND)
        }
      }


      "respond with BAD_REQUEST" when {
        "given invalid data" in new Fixture {

          val newRequest = request.withFormUrlEncodedBody(
            "accountName" -> "test",
            "isUK" -> "true"
          )

          when(controller.dataCacheConnector.fetch[Seq[BankDetails]](any())
            (any(), any(), any())).thenReturn(Future.successful(None))
          when(controller.dataCacheConnector.save[Seq[BankDetails]](any(), any())
            (any(), any(), any())).thenReturn(Future.successful(emptyCache))

          val result = controller.post(1, true)(newRequest)

          status(result) must be(BAD_REQUEST)
        }
      }
    }
  }
}
