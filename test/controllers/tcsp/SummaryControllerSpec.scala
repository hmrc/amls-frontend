package controllers.tcsp

import connectors.DataCacheConnector
import models.tcsp._
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.i18n.Messages
import play.api.test.Helpers._
import utils.AuthorisedFixture

import scala.concurrent.Future

class SummaryControllerSpec extends PlaySpec with OneAppPerSuite {

  trait Fixture extends AuthorisedFixture {
    self =>

    val controller = new SummaryController {
      override val dataCache = mock[DataCacheConnector]
      override val authConnector = self.authConnector
    }
  }

  "Get" must {

    val DefaultProvidedServices = ProvidedServices(Set(PhonecallHandling, Other("other service")))
    val DefaultCompanyServiceProviders = TcspTypes(Set(RegisteredOfficeEtc,
      CompanyFormationAgent(true, false)))
    val DefaultServicesOfAnotherTCSP = ServicesOfAnotherTCSPYes("12345678")

    "load the summary page when section data is available" in new Fixture {

      val model = Tcsp(
        Some(DefaultCompanyServiceProviders),
        Some(DefaultProvidedServices),
        Some(DefaultServicesOfAnotherTCSP)
      )

      when(controller.dataCache.fetch[Tcsp](any())
        (any(), any(), any())).thenReturn(Future.successful(Some(model)))

      val result = controller.get()(request)
      status(result) must be(OK)
      val document: Document = Jsoup.parseBodyFragment(contentAsString(result))
      document.title must be(s"${Messages("summary.checkyouranswers.title")} - ${Messages("summary.tcsp")} - ${Messages("title.amls")} - ${Messages("title.gov")}")
      val elements = document.getElementsByTag("table")
      elements.get(0).select("tr").get(0).text must include(Messages("tcsp.kind.of.service.provider.title"))
      elements.get(0).select("tr").get(1).text must include(Messages("tcsp.service.provider.lbl.03"))
      elements.get(0).select("tr").get(2).text must include(Messages("tcsp.service.provider.lbl.05"))

      elements.get(1).select("tr").get(0).text must include(Messages("tcsp.provided_services.title"))
      elements.get(1).select("tr").get(1).text must include(Messages("tcsp.provided_services.service.lbl.01"))
      elements.get(1).select("tr").get(2).text must include(Messages("tcsp.provided_services.service.lbl.08"))

      elements.get(2).select("tr").get(0).text must include(Messages("tcsp.servicesOfAnotherTcsp.title"))
      elements.get(2).select("tr").get(1).text must include(Messages("lbl.yes"))
      elements.get(2).select("tr").get(2).text must include("12345678")

    }

    "redirect to the main summary page when section data is unavailable" in new Fixture {

      when(controller.dataCache.fetch[Tcsp](any())
        (any(), any(), any())).thenReturn(Future.successful(None))

      val result = controller.get()(request)
      status(result) must be(SEE_OTHER)
    }
  }
}
