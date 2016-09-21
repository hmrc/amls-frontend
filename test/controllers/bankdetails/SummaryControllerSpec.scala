package controllers.bankdetails

import connectors.DataCacheConnector
import models.bankdetails._
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.mockito.Matchers.{eq => meq}
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.i18n.Messages
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.AuthorisedFixture

import scala.concurrent.Future

class SummaryControllerSpec extends PlaySpec with OneAppPerSuite with MockitoSugar {

  trait Fixture extends AuthorisedFixture {
    self =>

    val controller = new SummaryController {
      override val dataCacheConnector = mock[DataCacheConnector]
      override val authConnector = self.authConnector
    }
  }

  "Get" must {

    "load the summary page when section data is available" in new Fixture {

      val model = BankDetails(None, None)

      when(controller.dataCacheConnector.fetch[Seq[BankDetails]](any())(any(), any(), any()))
        .thenReturn(Future.successful(Some(Seq(model))))
      val result = controller.get()(request)

      status(result) must be(OK)
    }

    "redirect to the main amls summary page when section data is unavailable" in new Fixture {
      when(controller.dataCacheConnector.fetch[Seq[BankDetails]](any())(any(), any(), any()))
        .thenReturn(Future.successful(None))

      val result = controller.get()(request)

      redirectLocation(result) must be(Some(controllers.routes.RegistrationProgressController.get.url))
      status(result) must be(SEE_OTHER)
    }

    "show bank account details on the Check your Answers page" in new Fixture {
      val model = BankDetails(
        Some(PersonalAccount),
        Some(BankAccount("Account Name", UKAccount("12341234","121212")))
      )

      when(controller.dataCacheConnector.fetch[Seq[BankDetails]](any())(any(), any(), any()))
        .thenReturn(Future.successful(Some(Seq(model))))

      val result = controller.get()(request)

      val contentString = contentAsString(result)

      val document = Jsoup.parse(contentString)
      document.title() must be(Messages("summary.bankdetails.checkyouranswers.title"))

      contentString must include("Account Name")
      contentString must include("Account number: 12341234")
      contentString must include("Sort code: 12-12-12")
      contentString must include("UK Bank Account")
      contentString must include("Personal")
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
     // redirectLocation(result) must be ("dgdfdfgfd")

      verify(controller.dataCacheConnector).save[Seq[BankDetails]](any(), meq(Seq(completeModel2,completeModel3,completeModel4)))(any(), any(), any())

    }

    "show no bank account text" when {
      "no bank account is selected" in new Fixture {
        val model = BankDetails(None, None)

        when(controller.dataCacheConnector.fetch[Seq[BankDetails]](any())(any(), any(), any()))
          .thenReturn(Future.successful(Some(Seq(model))))
        val result = controller.get()(request)
        status(result) must be(OK)
        contentAsString(result) must include (Messages("bankdetails.summary.nobank.account"))
      }
    }
  }
}
