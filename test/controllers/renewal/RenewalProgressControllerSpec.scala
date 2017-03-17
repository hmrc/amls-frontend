package controllers.renewal

import connectors.DataCacheConnector
import models.registrationprogress.{Completed, NotStarted, Section}
import org.jsoup.Jsoup
import org.mockito.Matchers.{ eq => eqTo, _ }
import org.mockito.Mockito._
import play.api.i18n.Messages
import play.api.mvc.Call
import play.api.test.Helpers._
import services.ProgressService
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.{AuthorisedFixture, GenericTestHelper}

import scala.concurrent.Future

class RenewalProgressControllerSpec extends GenericTestHelper {

  trait Fixture extends AuthorisedFixture {
    self => val request = addToken(authRequest)

    val dataCacheConnector = mock[DataCacheConnector]
    val progressService = mock[ProgressService]

    val controller = new RenewalProgressController(self.authConnector, dataCacheConnector, progressService)

    val cacheMap = mock[CacheMap]

    val defaultSection = Section("A new section", NotStarted, hasChanged = false, mock[Call])

    when {
      dataCacheConnector.fetchAll(any(), any())
    } thenReturn Future.successful(Some(cacheMap))

    when {
      progressService.sections(eqTo(cacheMap))
    } thenReturn Seq(defaultSection)

  }

  "The Renewal Progress Controller" must {

    "load the page" in new Fixture {

      val result = controller.get()(request)

      status(result) mustBe OK

      val html = Jsoup.parse(contentAsString(result))

      html.select(".page-header").text() must include(Messages("renewal.progress.title"))

    }

    "display all the available sections from a normal variation progress page" in new Fixture {

      val result = controller.get()(request)

      val html = Jsoup.parse(contentAsString(result))

      html.select(".progress-step--details").text() must include("A new section")

    }

  }

}
