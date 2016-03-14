package controllers.bankdetails

import connectors.DataCacheConnector
import models.bankdetails._
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.AuthorisedFixture

import scala.concurrent.Future

class BankAccountControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar {

  trait Fixture extends AuthorisedFixture {
    self =>
    val controller = new BankAccountController {
      override val dataCacheConnector = mock[DataCacheConnector]
      override val authConnector = self.authConnector
    }
  }

  val emptyCache = CacheMap("", Map.empty)

  val fieldElements = Array("accountName", "accountNumber", "sortCode", "IBANNumber")

  "BankAccountController" must {

    "get the blank page without values when the page is loaded first" in new Fixture {

      when(controller.dataCacheConnector.fetch[Seq[BankAccount]](any())
        (any(), any(), any())).thenReturn(Future.successful(None))

      val result = controller.get()(request)
      val document: Document = Jsoup.parse(contentAsString(result))
      for (field <- fieldElements)
        document.select(s"input[name=$field]").`val` must be(empty)
    }

    "get the page with values when the page is loaded again " in new Fixture {

      val ukBankAccount = BankAccount("My Account", UKAccount("12345678", "202502"))
      val nonUKBankAccount = BankAccount("My Account", NonUKAccountNumber("00081050223857232"))

      when(controller.dataCacheConnector.fetch[Seq[BankAccount]](any())
        (any(), any(), any())).thenReturn(Future.successful(Some(Seq(ukBankAccount))))
      val result = controller.get()(request)
    }

    "on post with valid data in edit mode" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "accountName" ->"test",
        "isUK" -> "false",
        "nonUKAccountNumber" -> "1234567890123456789012345678901234567890",
        "isIBAN" -> "false"
      )

      when(controller.dataCacheConnector.fetch[Seq[BankDetails]](any())
        (any(), any(), any())).thenReturn(Future.successful(None))

      when(controller.dataCacheConnector.save[Seq[BankDetails]](any(), any())
        (any(), any(), any())).thenReturn(Future.successful(emptyCache))

      val result = controller.post(1, true)(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(routes.SummaryController.get().url))
    }

    "on post with invalid data" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "accountName" ->"test",
        "isUK" -> "true"
      )

      when(controller.dataCacheConnector.fetch[Seq[BankDetails]](any())
        (any(), any(), any())).thenReturn(Future.successful(None))

      when(controller.dataCacheConnector.save[Seq[BankDetails]](any(), any())
        (any(), any(), any())).thenReturn(Future.successful(emptyCache))

      val result = controller.post()(newRequest)
      status(result) must be(BAD_REQUEST)
    }
  }

}
