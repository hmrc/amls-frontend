package controllers

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import models.businessmatching.BusinessMatching
import models.notifications._
import org.joda.time.{DateTime, DateTimeZone, LocalDate}

import scala.concurrent.Future

trait NotificationsController extends BaseController {

  protected[controllers] val dataCacheConnector: DataCacheConnector

  def getMessages() = Authorised.async {
    implicit authContext => implicit request =>
      val fetchBusinessMatching = dataCacheConnector.fetch[BusinessMatching](BusinessMatching.key)
      fetchBusinessMatching map { businessMatching =>
        (for {
          bm <- businessMatching
          rd <- bm.reviewDetails
        } yield {
          Ok(views.html.notifications.your_messages(rd.businessName, getNotificationRecords()))
        }) getOrElse(throw new Exception("Cannot retrieve business name"))
      }
  }

  def getNotificationRecords(notifications: List[Notification] = List()): List[Notification] =
    notifications match {
      case s :: sc => notifications.sortWith((x,y) => x.receivedAt.isAfter(y.receivedAt))
      case _ => notifications
    }

}

object NotificationsController extends NotificationsController {
  override protected[controllers] val dataCacheConnector = DataCacheConnector
  override protected val authConnector = AMLSAuthConnector
}