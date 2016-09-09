package controllers.bankdetails

import connectors.DataCacheConnector
import models.bankdetails._
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfter
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.i18n.Messages
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.AuthorisedFixture

import scala.concurrent.Future

class BankAccountAddControllerSpec extends PlaySpec
  with OneAppPerSuite
  with MockitoSugar
  with BeforeAndAfter {

  trait Fixture extends AuthorisedFixture {
    self =>
    val controller = new BankAccountAddController {
      override val dataCacheConnector = mock[DataCacheConnector]
      override val authConnector = self.authConnector
    }

    when(controller.dataCacheConnector.fetch[Seq[BankDetails]](any())(any(), any(), any()))
      .thenReturn(Future.successful(Some(Seq(BankDetails(
        Some(PersonalAccount),
        Some(BankAccount("AccountName", UKAccount("12341234", "121212"))))))))
    when(controller.dataCacheConnector.save[Seq[BankDetails]](any(), any())(any(), any(), any()))
      .thenReturn(Future.successful(emptyCache))
  }

  val emptyCache = CacheMap("", Map.empty)


  "BankAccountAddController" when {
    "get is called" must {
      "respond with SEE_OTHER" when {
        "display guidance is true" in new Fixture {

          val result = controller.get(true)(request)

          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(routes.WhatYouNeedController.get(2).url))
        }

        "display guidance is false" in new Fixture {

          val result = controller.get(false)(request)

          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(routes.BankAccountTypeController.get(2, false).url))
        }
      }
    }
  }
}
