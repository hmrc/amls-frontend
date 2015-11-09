package services

import connectors.AmlsConnector
import models.LoginDetails
import uk.gov.hmrc.play.audit.http.HeaderCarrier
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http.HttpResponse

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


trait AreYouEmployedWithinTheBusinessService {

  val amlsConnector: AmlsConnector

  def submitLoginDetails(loginDetails: LoginDetails)(implicit user: AuthContext, headerCarrier: HeaderCarrier): Future[HttpResponse] = {
    for {
      response <- amlsConnector.submitLoginDetails(loginDetails)
    } yield (response)
  }
}

object AreYouEmployedWithinTheBusinessService extends AreYouEmployedWithinTheBusinessService {
  val amlsConnector = AmlsConnector
}
