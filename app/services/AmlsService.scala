package services

import connectors.{DataCacheConnector, AmlsConnector}
import models. LoginDetails
import play.api.mvc.Request
import uk.gov.hmrc.play.audit.http.HeaderCarrier
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http.HttpResponse

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait AmlsService {

  val amlsConnector: AmlsConnector
  val dataCacheConnector: DataCacheConnector

  def submitLoginDetails(loginDetails: LoginDetails)(implicit user: AuthContext, headerCarrier: HeaderCarrier) : Future[HttpResponse]= {
    for {
      response <- amlsConnector.submitLoginDetails(loginDetails)
    } yield (response)
  }
}

object AmlsService extends AmlsService {
  val amlsConnector = AmlsConnector
  val dataCacheConnector = DataCacheConnector
}
