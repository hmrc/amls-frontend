package controllers.estateagentbusiness

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import models.estateagentbusiness._
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.i18n.Messages
import play.api.test.Helpers._
import utils.AuthorisedFixture

import scala.concurrent.Future

class BusinessServicesControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar {
  trait Fixture extends AuthorisedFixture {
    self =>

    val controller = new BusinessServicesController {
      override val dataCacheConnector = mock[DataCacheConnector]
      override val authConnector = self.authConnector
    }
  }

  "BusinessServicesController" must {

    "use correct services" in new Fixture {
      BusinessServicesController.authConnector must be(AMLSAuthConnector)
      BusinessServicesController.dataCacheConnector must be(DataCacheConnector)
    }

    "on get display Business services page" in new Fixture {
      when(controller.dataCacheConnector.fetchDataShortLivedCache[EstateAgentBusiness](any())
        (any(), any(), any())).thenReturn(Future.successful(None))
      val result = controller.get()(request)
      status(result) must be(OK)
      contentAsString(result) must include(Messages("estateagentbusiness.servicess.title"))
    }

    "load the page with data when the user revisits at a later time" in new Fixture {
      when(controller.dataCacheConnector.fetchDataShortLivedCache[EstateAgentBusiness](any())
        (any(), any(), any())).thenReturn(Future.successful(Some(EstateAgentBusiness(Some(Seq(Auction)), None, None, None))))
      val test = EstateAgentBusiness(Some(Seq(Auction)), None, None, None)
      val data:Seq[String] = test.services match {
        case Some(x) => x.map(servicesToString)
        case None => Seq("")
      }

      println(""+data map { line =>
        line
      })

      val result = controller.get()(request)
      status(result) must be(OK)

      val document: Document = Jsoup.parse(contentAsString(result))
      document.select("input[value=03]").hasAttr("checked") must be(true)
    }

    def servicesToString(obj : Service) : String = {
      obj match {
        case Residential => "01"
        case Commercial => "02"
        case Auction => "03"
        case Relocation => "04"
        case BusinessTransfer => "05"
        case AssetManagement => "06"
        case LandManagement => "07"
        case Development => "08"
        case SocialHousing => "09"
        case _ => ""
      }
    }

   /* "on post with valid data in edit mode" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "services" -> ("02", "01", "03")
      )

      when(controller.dataCacheConnector.fetchDataShortLivedCache[EstateAgentBusiness](any())
        (any(), any(), any())).thenReturn(Future.successful(None))

      when(controller.dataCacheConnector.saveDataShortLivedCache[EstateAgentBusiness](any(), any())
        (any(), any(), any())).thenReturn(Future.successful(None))

      val result = controller.post(true)(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(controllers.estateagentbusiness.routes.SummaryController.get().url))
    }*/
  }

}
