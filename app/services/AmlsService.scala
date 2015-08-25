package services

import connectors.{AmlsConnector}
import models.{LoginDetails}
import uk.gov.hmrc.play.audit.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global

trait AmlsService {

  val amlsConnector: AmlsConnector

  def submitLoginDetails(loginDetails: LoginDetails)(implicit headerCarrier: HeaderCarrier) = {
    for {
      logindtlsFromSvcs <- amlsConnector.submitLoginDetails(loginDetails)
    } yield (logindtlsFromSvcs)
  }
}

object AmlsService extends AmlsService {
  val amlsConnector = AmlsConnector
}
