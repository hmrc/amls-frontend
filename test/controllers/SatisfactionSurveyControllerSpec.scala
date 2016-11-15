package controllers

import connectors.DataCacheConnector
import models.businessactivities.ExpectedAMLSTurnover.First
import models.businessactivities._
import models.businessmatching.{BusinessActivities => Activities, _}
import models.status.{NotCompleted, SubmissionDecisionApproved}
import org.jsoup.Jsoup
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.i18n.Messages
import play.api.test.Helpers._
import services.StatusService
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.AuthorisedFixture

import scala.concurrent.Future

class SatisfactionSurveyControllerSpec extends PlaySpec with OneAppPerSuite with MockitoSugar with ScalaFutures {

  trait Fixture extends AuthorisedFixture {
    self =>

    val controller = new SatisfactionSurveyController {
      override val authConnector = self.authConnector
    }

    def model: Option[BusinessActivities] = None

  }

  val emptyCache = CacheMap("", Map.empty)

  "ExpectedAMLSTurnoverController" must {

    "on get display the survey page" in new Fixture {

      val result = controller.get()(request)
      status(result) must be(OK)
      contentAsString(result) must include(Messages("survey.satisfaction.lbl.01"))
      contentAsString(result) must include(Messages("survey.satisfaction.lbl.02"))
      contentAsString(result) must include(Messages("survey.satisfaction.lbl.03"))
      contentAsString(result) must include(Messages("survey.satisfaction.lbl.04"))
      contentAsString(result) must include(Messages("survey.satisfaction.lbl.05"))
    }

    "on post with valid data go to the progress page" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "satisfaction" -> "01",
        "details" -> ""
      )

      val result = controller.post()(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(controllers.routes.LandingController.get().url))
    }

    "on post with invalid data" in new Fixture {

      val result = controller.post(true)(request)
      val document = Jsoup.parse(contentAsString(result))

      status(result) mustBe BAD_REQUEST
      document.select(".amls-error-summary").size mustEqual 1
    }

  }
}
