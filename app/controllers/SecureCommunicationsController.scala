package controllers

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import models.businessmatching.BusinessMatching
import models.securecommunications._
import org.joda.time.LocalDate

import scala.concurrent.Future

trait SecureCommunicationsController extends BaseController {

  protected[controllers] val dataCacheConnector: DataCacheConnector

  def getMessages() = Authorised.async {
    implicit authContext => implicit request =>
      val fetchBusinessMatching = dataCacheConnector.fetch[BusinessMatching](BusinessMatching.key)
      fetchBusinessMatching map { businessMatching =>
        (for {
          bm <- businessMatching
          rd <- bm.reviewDetails
        } yield {
          Ok(views.html.securecommunications.your_messages(rd.businessName, getSecureComms))
        }) getOrElse Ok(views.html.securecommunications.your_messages("", getSecureComms))
      }
  }

  private def getSecureComms: List[SecureCommunication] = ???

}

object SecureCommunicationsController extends SecureCommunicationsController {
  override protected[controllers] val dataCacheConnector = DataCacheConnector
  override protected val authConnector = AMLSAuthConnector
}