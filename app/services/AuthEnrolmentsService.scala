package services

import connectors.AuthConnector
import play.api.Logger
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

trait AuthEnrolmentsService {

  private[services] def authConnector: AuthConnector

  private val amlsKey = "HMRC-MLR-ORG"
  private val amlsNumberKey = "MLRRefNumber"

  def amlsRegistrationNumber(uri: String)(implicit
                                          headerCarrier: HeaderCarrier,
                                          ec: ExecutionContext): Future[Option[String]] = {

    val enrolments = authConnector.enrollments(uri)

    enrolments map {
      enrolmentsList => {
        for {
          amlsEnrolment <- enrolmentsList.find(enrolment => enrolment.key == amlsKey)
          amlsIdentifier <- amlsEnrolment.identifiers.find(identifier => identifier.key == amlsNumberKey)
        } yield {
          val prefix = "[AuthEnrolmentsService][amlsRegistrationNumber]"
          Logger.debug(s"$prefix : ${amlsIdentifier.value}")
          amlsIdentifier.value
        }
      }
    }

  }

}

object AuthEnrolmentsService extends AuthEnrolmentsService {

  override private[services] val authConnector = AuthConnector

}