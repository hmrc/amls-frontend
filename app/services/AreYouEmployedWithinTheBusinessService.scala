package services

import connectors.AreYouEmployedWithinTheBusinessConnector
import models.{AreYouEmployedWithinTheBusinessModel, LoginDetails}
import uk.gov.hmrc.play.audit.http.HeaderCarrier
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http.HttpResponse

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait AreYouEmployedWithinTheBusinessService {

  def areYouEmployedWithinTheBusinessConnector: AreYouEmployedWithinTheBusinessConnector

  def submitDetails(areYouEmployedWithinTheBusiness: AreYouEmployedWithinTheBusinessModel)(implicit user: AuthContext, headerCarrier: HeaderCarrier): Future[HttpResponse] = {
    for {
      response <- areYouEmployedWithinTheBusinessConnector.submitDetails(areYouEmployedWithinTheBusiness)
    } yield (response)
  }
}

object AreYouEmployedWithinTheBusinessService extends AreYouEmployedWithinTheBusinessService {
  override lazy val areYouEmployedWithinTheBusinessConnector = AreYouEmployedWithinTheBusinessConnector
}
