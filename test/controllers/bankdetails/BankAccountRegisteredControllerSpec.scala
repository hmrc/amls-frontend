package controllers.bankdetails

import connectors.DataCacheConnector
import models.bankdetails.BankDetails
import org.jsoup.Jsoup
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.i18n.Messages
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.AuthorisedFixture

import scala.concurrent.Future

class BankAccountRegisteredControllerSpec extends PlaySpec with OneAppPerSuite with MockitoSugar {

  trait Fixture extends AuthorisedFixture {
    self =>
    val controller = new BankAccountRegisteredController {
      override val dataCacheConnector = mock[DataCacheConnector]
      override val authConnector = self.authConnector
    }
  }

  val emptyCache = CacheMap("", Map.empty)

  "BankAccountRegisteredController" must {

    "Get Option:" must {

      "load the Bank account Registered page" in new Fixture {

        when(controller.dataCacheConnector.fetch[Seq[BankDetails]](any())(any(), any(), any()))
          .thenReturn(Future.successful(None))

        val result = controller.get(1)(request)
        status(result) must be(OK)

        val htmlValue = Jsoup.parse(contentAsString(result))
        htmlValue.title mustBe Messages("bankdetails.bank.account.registered.title")
      }

      "load the Bank account Registered page1" in new Fixture {

        when(controller.dataCacheConnector.fetch[Seq[BankDetails]](any())(any(), any(), any()))
          .thenReturn(Future.successful(Some(Seq(BankDetails(None,None), BankDetails(None,None)))))

        val result = controller.get(1)(request)
        status(result) must be(OK)

        contentAsString(result) must include(Messages("bankdetails.have.registered.accounts.text", 2))
      }
    }

    "Post" must {

      "successfully redirect to the page on selection of 'Yes'" in new Fixture {

        val newRequest = request.withFormUrlEncodedBody("registerAnotherBank" -> "true")

        when(controller.dataCacheConnector.fetch[BankDetails](any())(any(), any(), any()))
          .thenReturn(Future.successful(None))

        when(controller.dataCacheConnector.save[BankDetails](any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(emptyCache))

        val result = controller.post(1)(newRequest)
        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some(controllers.bankdetails.routes.BankAccountAddController.get(false).url))
      }

      "successfully redirect to the page on selection of 'no'" in new Fixture {
        val newRequest = request.withFormUrlEncodedBody("registerAnotherBank" -> "false")

        when(controller.dataCacheConnector.fetch[BankDetails](any())(any(), any(), any()))
          .thenReturn(Future.successful(None))

        when(controller.dataCacheConnector.save[BankDetails](any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(emptyCache))

        val result = controller.post(1)(newRequest)
        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some(controllers.bankdetails.routes.SummaryController.get(false).url))
      }
    }

    "on post invalid data show error" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody()
      when(controller.dataCacheConnector.fetch[BankDetails](any())(any(), any(), any()))
        .thenReturn(Future.successful(None))

      val result = controller.post(1)(newRequest)
      status(result) must be(BAD_REQUEST)
      contentAsString(result) must include(Messages("bankdetails.want.to.register.another.account"))

    }

    "on post with invalid data show error" in new Fixture {
      val newRequest = request.withFormUrlEncodedBody(
        "registerAnotherBank" -> ""
      )
      when(controller.dataCacheConnector.fetch[BankDetails](any())(any(), any(), any()))
        .thenReturn(Future.successful(None))

      val result = controller.post(1)(newRequest)
      status(result) must be(BAD_REQUEST)
      contentAsString(result) must include(Messages("bankdetails.want.to.register.another.account"))

    }
  }
}
