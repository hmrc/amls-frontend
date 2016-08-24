package controllers

import models.registrationprogress.Section
import org.scalatest.WordSpec
import org.scalatest.MustMatchers
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.OneAppPerSuite
import services.ProgressService
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.AuthorisedFixture
import play.api.http.Status.OK
import org.mockito.Mockito._
import org.mockito.Matchers._


import scala.concurrent.Future

class RegistrationAmendmentControllerSpec extends WordSpec with MustMatchers with ScalaFutures with OneAppPerSuite {

  trait Fixture extends AuthorisedFixture {
    self =>

    private val mockProgressService = mock[ProgressService]

    when(mockProgressService.sections(any(), any(), any())).thenReturn(Future.successful(Seq.empty[Section]))

    val controller = new RegistrationAmendmentController {
      override protected def authConnector: AuthConnector = self.authConnector
      override private[controllers] def progressService: ProgressService = mockProgressService
    }
  }

  "RegistrationAmendmentController" when {
    "get is called" must {
      "respond with Okay" in new Fixture {
        val resultF = controller.get().apply(request)

        
        whenReady(resultF) { result =>
          result.header.status must be (OK)
        }
      }
    }
  }
}
