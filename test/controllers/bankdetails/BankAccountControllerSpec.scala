package controllers.bankdetails

import connectors.DataCacheConnector
import models.bankdetails.BankAccount
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

  "BankAccountController" must {

    "get the blank page without values when the page is loaded first" in new Fixture {

      when(controller.dataCacheConnector.fetchDataShortLivedCache[Seq[BankAccount]](any())
        (any(), any(), any())).thenReturn(Future.successful(None))

      val result = controller.get()(request)
      status(result) must be(OK)

    }

    "get the page with values when the page is loaded again " in new Fixture {

      when(controller.dataCacheConnector.fetchDataShortLivedCache[Seq[BankAccount]](any())
        (any(), any(), any())).thenReturn(Future.successful(None))

      val result = controller.get()(request)
      status(result) must be(OK)

    }


  }

}
