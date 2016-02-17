package controllers.bankdetails

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import models.bankdetails._
import models.estateagentbusiness.EstateAgentBusiness
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.test.Helpers._
import utils.AuthorisedFixture

import scala.concurrent.Future

class SummaryControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar {

  trait Fixture extends AuthorisedFixture {
    self =>

    val controller = new SummaryController {
      override val dataCache = mock[DataCacheConnector]
      override val authConnector = self.authConnector
    }
  }

  "Get" must {

    "use correct services" in new Fixture {
      SummaryController.authConnector must be(AMLSAuthConnector)
      SummaryController.dataCache must be(DataCacheConnector)
    }

    "load the summary page when section data is available" in new Fixture {

      val model = BankDetails(None, None)

      when(controller.dataCache.fetchDataShortLivedCache[Seq[BankDetails]](any())
        (any(), any(), any())).thenReturn(Future.successful(Some(Seq(model))))

      val result = controller.get()(request)
      status(result) must be(OK)
    }

    "load the summary page when section data is available11" in new Fixture {

      val model = BankDetails(Some(NoBankAccount), None)
      val test = Seq(BankDetails(Some(NoBankAccount), None),
        BankDetails(Some(PersonalAccount), None),
        BankDetails(Some(BelongsToBusiness), None),
        BankDetails(Some(BelongsToOtherBusiness), None))

      val s = "Test data yuuu"
      val data = s.split(" ")

     val m =  data map { x =>
        val d = x.charAt(0).toUpper
        x.replace(x.charAt(0), d)
      }

     m.mkString("")

      println("----------------------------------------"+m.mkString(" "))

    }

    "redirect to the main summary page when section data is unavailable" in new Fixture {

      when(controller.dataCache.fetchDataShortLivedCache[Seq[BankDetails]](any())
        (any(), any(), any())).thenReturn(Future.successful(None))

      val result = controller.get()(request)
      status(result) must be(SEE_OTHER)
    }
  }
}
