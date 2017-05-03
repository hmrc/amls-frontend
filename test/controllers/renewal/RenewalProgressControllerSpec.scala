package controllers.renewal

import connectors.DataCacheConnector
import models.businessmatching._
import models.registrationprogress.{Completed, NotStarted, Section}
import org.jsoup.Jsoup
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import play.api.i18n.Messages
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.Call
import play.api.test.Helpers._
import services.{ProgressService, RenewalService}
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.{AuthorisedFixture, GenericTestHelper}
import play.api.inject.bind
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class RenewalProgressControllerSpec extends GenericTestHelper {

  trait Fixture extends AuthorisedFixture {
    self =>
    val request = addToken(authRequest)

    implicit val authContext = mock[AuthContext]
    implicit val headerCarrier = HeaderCarrier()

    val dataCacheConnector = mock[DataCacheConnector]
    val progressService = mock[ProgressService]
    val renewalService = mock[RenewalService]

    lazy val app = new GuiceApplicationBuilder()
      .disable[com.kenshoo.play.metrics.PlayModule]
      .overrides(bind[ProgressService].to(progressService))
      .overrides(bind[DataCacheConnector].to(dataCacheConnector))
      .bindings(bind[RenewalService].to(renewalService))
      .overrides(bind[AuthConnector].to(self.authConnector))
      .build()

    val controller = app.injector.instanceOf[RenewalProgressController]

    val cacheMap = mock[CacheMap]

    val defaultSection = Section("A new section", NotStarted, hasChanged = false, mock[Call])

    val renewalSection = Section("renewal", NotStarted, hasChanged = false, mock[Call])

    when {
      dataCacheConnector.fetchAll(any(), any())
    } thenReturn Future.successful(Some(cacheMap))

    when {
      progressService.sections(eqTo(cacheMap))
    } thenReturn Seq(defaultSection)

    when {
      renewalService.getSection(any(), any(), any())
    } thenReturn Future.successful(renewalSection)

    val BusinessActivitiesModel = BusinessActivities(Set(MoneyServiceBusiness, TrustAndCompanyServices, TelephonePaymentService))
    val bm = Some(BusinessMatching(activities = Some(BusinessActivitiesModel)))

    when(cacheMap.getEntry[BusinessMatching](BusinessMatching.key))
      .thenReturn(bm)

  }

  "The Renewal Progress Controller" must {

    "load the page" in new Fixture {

      val BusinessActivitiesModelWithoutTCSPOrMSB = BusinessActivities(Set(TelephonePaymentService))
      val bmWithoutTCSPOrMSB = Some(BusinessMatching(activities = Some(BusinessActivitiesModel)))

      when(cacheMap.getEntry[BusinessMatching](BusinessMatching.key))
        .thenReturn(bmWithoutTCSPOrMSB)

      val result = controller.get()(request)

      status(result) mustBe OK

      val html = Jsoup.parse(contentAsString(result))

      html.select(".page-header").text() must include(Messages("renewal.progress.title"))

    }

    "display all the available sections from a normal variation progress page" in new Fixture {
      val result = controller.get()(request)
      val html = Jsoup.parse(contentAsString(result))

      val element = html.select(".progress-step--details")
      element.text must include("A new section")
      element.size mustBe 2
    }

    "display the renewal section" in new Fixture {
      val result = controller.get()(request)
      val html = Jsoup.parse(contentAsString(result))

      html.select(".renewal-progress-section").text() must include(Messages("progress.renewal.name"))
    }

    "displays the renewal page with an empty sequence when no sections are returned" in new Fixture {
      when {
        dataCacheConnector.fetchAll(any(), any())
      } thenReturn Future.successful(None)

      val result = controller.get()(request)
      val html = Jsoup.parse(contentAsString(result))

      html.select(".progress-step--details").size() mustBe 1
    }

    "redirect to the declaration page when the form is posted" in new Fixture {
      val result = controller.post()(request)

      redirectLocation(result) mustBe Some(controllers.declaration.routes.WhoIsRegisteringController.get().url)
    }

  }

}
