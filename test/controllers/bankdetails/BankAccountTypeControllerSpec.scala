package controllers.bankdetails

import connectors.DataCacheConnector
import models.bankdetails.{PersonalAccount, BankDetails}
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

class BankAccountTypeControllerSpec extends PlaySpec with  OneAppPerSuite with MockitoSugar {

  trait Fixture extends AuthorisedFixture {
    self =>

    val controller = new BankAccountTypeController {
      override val dataCacheConnector = mock[DataCacheConnector]
      override val authConnector = self.authConnector
    }
  }

  val emptyCache = CacheMap("", Map.empty)

  "BankAccountTypeController" must {

    "on get display kind of bank account page" in new Fixture {

      when(controller.dataCacheConnector.fetch[Seq[BankDetails]](any())
        (any(), any(), any())).thenReturn(Future.successful(Some(Seq(BankDetails(None, None)))))

      val result = controller.get(1, false)(request)
      status(result) must be(OK)
      contentAsString(result) must include(Messages("bankdetails.accounttype.title"))
    }

    "on get display kind of bank account page with pre populated data" in new Fixture {

      when(controller.dataCacheConnector.fetch[Seq[BankDetails]](any())
        (any(), any(), any())).thenReturn(Future.successful(Some(Seq(BankDetails(Some(PersonalAccount), None)))))

      val result = controller.get(1)(request)
      status(result) must be(OK)

      val document = Jsoup.parse(contentAsString(result))
      document.select("input[value=01]").hasAttr("checked") must be(true)
    }

    "on post with valid data" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "bankAccountType" -> "01"
      )

      when(controller.dataCacheConnector.fetch[Seq[BankDetails]](any())
        (any(), any(), any())).thenReturn(Future.successful(Some(Seq(BankDetails(Some(PersonalAccount), None)))))

      when(controller.dataCacheConnector.save[Seq[BankDetails]](any(), any())
        (any(), any(), any())).thenReturn(Future.successful(emptyCache))

      val result = controller.post(1, false)(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(routes.BankAccountController.get(1, false).url))
    }

    "on post with valid data in edit mode" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "bankAccountType" -> "04"
      )

      when(controller.dataCacheConnector.fetch[Seq[BankDetails]](any())
        (any(), any(), any())).thenReturn(Future.successful(Some(Seq(BankDetails(Some(PersonalAccount), None)))))

      when(controller.dataCacheConnector.save[Seq[BankDetails]](any(), any())
        (any(), any(), any())).thenReturn(Future.successful(emptyCache))

      val result = controller.post(1, true)(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(routes.BankAccountController.get(1, true).url))
    }

    "on post with invalid data" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "bankAccountType" -> "10"
      )

      when(controller.dataCacheConnector.fetch[Seq[BankDetails]](any())
        (any(), any(), any())).thenReturn(Future.successful(None))

      when(controller.dataCacheConnector.save[Seq[BankDetails]](any(), any())
        (any(), any(), any())).thenReturn(Future.successful(emptyCache))

      val result = controller.post(0, false)(newRequest)
      status(result) must be(BAD_REQUEST)

      val document = Jsoup.parse(contentAsString(result))
      document.select("span").html() must include("Invalid value")
    }

  }
}
