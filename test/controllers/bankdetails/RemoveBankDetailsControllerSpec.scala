package controllers.bankdetails

import connectors.DataCacheConnector
import models.bankdetails._
import org.jsoup.Jsoup
import org.mockito.Matchers.{eq => meq, _}
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.i18n.Messages
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.AuthorisedFixture

import scala.concurrent.Future

class RemoveBankDetailsControllerSpec extends PlaySpec with OneAppPerSuite with MockitoSugar {

  trait Fixture extends AuthorisedFixture {
    self =>

    val controller = new RemoveBankDetailsController {
      override val dataCacheConnector: DataCacheConnector =  mock[DataCacheConnector]
      override protected def authConnector: AuthConnector = self.authConnector
    }
  }

  "Get" must {

    "load the remove bank account page when section data is available" in new Fixture {
      val result = controller.get(1,"",false)(request)

      status(result) must be(OK)
    }

    "show bank account details on the remove bank account page" in new Fixture {

      val result = controller.get(1,"account Name",true) (request)

      val contentString = contentAsString(result)

      val document = Jsoup.parse(contentString)
      document.title() must be(Messages("bankdetails.remove.bank.account.title"))
    }

    "remove bank account from summary" in new Fixture {

      val emptyCache = CacheMap("", Map.empty)

      val accountType1 = PersonalAccount
      val bankAccount1 = BankAccount("My Account1", UKAccount("111111", "11-11-11"))

      val accountType2 = PersonalAccount
      val bankAccount2 = BankAccount("My Account2", UKAccount("222222", "22-22-22"))

      val accountType3 = PersonalAccount
      val bankAccount3 = BankAccount("My Account3", UKAccount("333333", "33-33-33"))

      val accountType4 = PersonalAccount
      val bankAccount4 = BankAccount("My Account4", UKAccount("444444", "44-44-44"))

      val completeModel1 = BankDetails(Some(accountType1), Some(bankAccount1))
      val completeModel2 = BankDetails(Some(accountType2), Some(bankAccount2))
      val completeModel3 = BankDetails(Some(accountType3), Some(bankAccount3))
      val completeModel4 = BankDetails(Some(accountType4), Some(bankAccount4))

      val bankAccounts = Seq(completeModel1,completeModel2,completeModel3,completeModel4)

      when(controller.dataCacheConnector.fetch[Seq[BankDetails]](any())(any(), any(), any()))
        .thenReturn(Future.successful(Some(bankAccounts)))

      when(controller.dataCacheConnector.save[Seq[BankDetails]](any(), any())(any(), any(), any()))
        .thenReturn(Future.successful(emptyCache))

      val result = controller.remove(1)(request)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be (Some(controllers.bankdetails.routes.SummaryController.get(false).url))

      verify(controller.dataCacheConnector).save[Seq[BankDetails]](any(), meq(Seq(completeModel2,completeModel3,completeModel4)))(any(), any(), any())

    }

  }
}
