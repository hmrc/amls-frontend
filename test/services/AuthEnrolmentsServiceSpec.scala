package services

import connectors.AuthConnector
import models.enrolment.{EnrolmentIdentifier, GovernmentGatewayEnrolment}
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import uk.gov.hmrc.play.http.HeaderCarrier
import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.Future

class AuthEnrolmentsServiceSpec extends PlaySpec with MockitoSugar with ScalaFutures {

  object AuthEnrolmentsService extends AuthEnrolmentsService {
    override private[services] val authConnector: AuthConnector = mock[AuthConnector]
  }

  implicit val hc = HeaderCarrier()

  private val amlsRegistrationNumber = "XXML00000100105"

  private val enrolmentsList = List[GovernmentGatewayEnrolment](GovernmentGatewayEnrolment("HMCE-VATVAR-ORG",
    List[EnrolmentIdentifier](EnrolmentIdentifier("VATRegNo", "999900449")), "Activated"), GovernmentGatewayEnrolment("HMRC-MLR-ORG",
    List[EnrolmentIdentifier](EnrolmentIdentifier("MLRRefNumber", amlsRegistrationNumber)), "Activated"))

  "AuthEnrolmentsService" must {

    "return an AMLS regsitration number" in {

      when(AuthEnrolmentsService.authConnector.enrollments(any())(any(),any())).thenReturn(Future.successful(enrolmentsList))

      whenReady(AuthEnrolmentsService.amlsRegistrationNumber("")){
        number => number.get mustEqual(amlsRegistrationNumber)
      }

    }

  }

}
