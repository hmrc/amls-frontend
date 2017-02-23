package filters

import connectors.KeystoreConnector
import models.status.ConfirmationStatus
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.inject.bind
import play.api.mvc.{Action, Results}
import play.api.test.{FakeRequest, FutureAwaits}
import play.api.test.Helpers._
import uk.gov.hmrc.play.filters.MicroserviceFilterSupport
import org.mockito.Mockito._
import org.mockito.Matchers._

import scala.concurrent.Future

class ConfirmationFilterSpec extends PlaySpec with OneAppPerSuite with MockitoSugar with Results with MicroserviceFilterSupport {

  val keystore = mock[KeystoreConnector]

  override lazy val app = new GuiceApplicationBuilder()
    .disable[modules.Module]
    .bindings(bind[KeystoreConnector].to(keystore))
    .build()

  "The confirmation filter" must {

    "redirect to the landing controller when the submission has been set" in {

      when(keystore.confirmationStatus(any(), any())) thenReturn Future.successful(ConfirmationStatus(Some(true)))

      val filter = app.injector.instanceOf[ConfirmationFilter]
      val rh = FakeRequest()
      val action = Action(Ok("success"))

      val result = filter(action)(rh).run()

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(controllers.routes.LandingController.get().url)
    }

    "run the original request when the submission has not been set" in {

      when(keystore.confirmationStatus(any(), any())) thenReturn Future.successful(ConfirmationStatus(None))

      val filter = app.injector.instanceOf[ConfirmationFilter]
      val rh = FakeRequest()
      val action = Action(Ok("success"))

      val result = filter(action)(rh).run()

      status(result) mustBe OK

    }

    "run the original request if the user is already on the landing page" in {

      when(keystore.confirmationStatus(any(), any())) thenReturn Future.successful(ConfirmationStatus(Some(true)))

      val filter = app.injector.instanceOf[ConfirmationFilter]
      val rh = FakeRequest(GET, controllers.routes.LandingController.get().url)
      val action = Action(Ok("success"))

      val result = filter(action)(rh).run()

      status(result) mustBe OK

    }

    "run the original request if the user is already on the confirmation page" in {

      when(keystore.confirmationStatus(any(), any())) thenReturn Future.successful(ConfirmationStatus(Some(true)))

      val filter = app.injector.instanceOf[ConfirmationFilter]
      val rh = FakeRequest(GET, controllers.routes.ConfirmationController.get().url)
      val action = Action(Ok("success"))

      val result = filter(action)(rh).run()

      status(result) mustBe OK

    }

    "excludes anything that isn't a page page" in {

      val filter = app.injector.instanceOf[ConfirmationFilter]
      val rh = FakeRequest(GET, "/amls.js")
      val action = Action(Ok("success"))

      val result = filter(action)(rh).run()

      status(result) mustBe OK

    }

  }

}
