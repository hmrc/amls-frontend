package controllers

import config.AMLSAuthConnector
import models.YourName
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.test.Helpers._
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector

class AboutYouControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar{

  val yourName: YourName = YourName("FirstName", "middleName", "lastName")
  val mockAuthConnector = mock[AuthConnector]

  object MockAboutYouController extends AboutYouController {
    override protected def authConnector: AuthConnector = mockAuthConnector
  }

  "AboutYouController" must {
    "use correct service" in {
      AboutYouController.authConnector must be(AMLSAuthConnector)
    }

    "on load of page " must {
      "Authorised users" must {
        "load the Sample Login page" in {
          getWithAuthorisedUser {
            result =>
              status(result) must be(OK)
              contentAsString(result) must include("name")
          }
        }

  }


}
