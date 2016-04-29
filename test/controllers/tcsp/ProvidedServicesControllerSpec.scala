package controllers.tcsp

import connectors.DataCacheConnector
import models.tcsp.{Other, ProvidedServices, Tcsp}
import org.jsoup.Jsoup
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.i18n.Messages
import play.api.test.Helpers._
import utils.AuthorisedFixture
import org.mockito.Mockito._

import scala.concurrent.Future

class ProvidedServicesControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with ScalaFutures {

  trait Fixture extends AuthorisedFixture {
    self =>
    val controller = new ProvidedServicesController {
      override val authConnector = self.authConnector
      override val dataCacheConnector: DataCacheConnector = mock[DataCacheConnector]
    }
  }

  "ProvidedServicesController" must {

    "get" must {

      "load the provided services page" in new Fixture {
        val result = controller.get()(request)
        status(result) must be(OK)
        contentAsString(result) must include(Messages("tcsp.provided_services.title"))
      }

      "load the provided services page with existing data" in new Fixture {

        val tcsp = Tcsp(providedServices = Some(ProvidedServices(Set(Other("some other service")))))
        when(controller.dataCacheConnector.fetch[Tcsp](Tcsp.key)) thenReturn Future.successful(Some(tcsp))

        val result = controller.get()(request)
        status(result) must be(OK)

        val document = Jsoup.parse(contentAsString(result))

        document.title() must be (Messages("tcsp.provided_services.title"))

        document.select("input[id=positions-08]").attr("checked") must be("checked")
        document.select("input[id=details]").text() must be ("some other service")
      }


    }
  }
}
