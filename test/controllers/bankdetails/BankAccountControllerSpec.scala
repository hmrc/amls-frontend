package controllers.bankdetails

import connectors.DataCacheConnector
import models.bankdetails.{NonUKAccountNumber, UKAccount, BankAccount}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.test.Helpers._
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

  val fieldElements = Array("accountName", "accountNumber", "sortCode", "IBANNumber")

  "BankAccountController" must {

    "get the blank page without values when the page is loaded first" in new Fixture {

      when(controller.dataCacheConnector.fetchDataShortLivedCache[Seq[BankAccount]](any())
        (any(), any(), any())).thenReturn(Future.successful(None))

      val result = controller.get()(request)
      val document: Document = Jsoup.parse(contentAsString(result))
      for (field <- fieldElements)
        document.select(s"input[name=$field]").`val` must be(empty)
    }

/*    "get the page with values when the page is loaded again " in new Fixture {

      val ukBankAccount = BankAccount("My Account", UKAccount("12345678", "202502"))
      val nonUKBankAccount = BankAccount("My Account", NonUKAccountNumber("00081050223857232"))

      when(controller.dataCacheConnector.fetchDataShortLivedCache[Seq[BankAccount]](any())
        (any(), any(), any())).thenReturn(Future.successful(Some(Seq(ukBankAccount))))
      val result = controller.get()(request)
      val document: Document = Jsoup.parse(contentAsString(result))

      //document.select(s"input[name=accountName]").`val` must be("My Account")
      document.select(s"input[name=sortCode]").`val` must be("202502")
      //document.select(s"input[name=nonUKAccountNumber]").`val` must be("12345678")

    }*/


  }

}
