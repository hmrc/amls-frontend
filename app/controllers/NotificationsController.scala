package controllers

import config.{AMLSAuthConnector, ApplicationConfig}
import connectors.{AmlsNotificationConnector, DataCacheConnector}
import models.businessmatching.BusinessMatching
import models.notifications._
import services.AuthEnrolmentsService
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait NotificationsController extends BaseController {

  protected[controllers] val dataCacheConnector: DataCacheConnector
  protected[controllers] def authEnrolmentsService: AuthEnrolmentsService
  protected[controllers] val amlsNotificationConnector: AmlsNotificationConnector

  def getMessages() = Authorised.async {
    implicit authContext => implicit request =>
      if (ApplicationConfig.notificationsToggle) {
        authEnrolmentsService.amlsRegistrationNumber flatMap {
          case Some(amlsRegNo) => {
            dataCacheConnector.fetch[BusinessMatching](BusinessMatching.key) flatMap { businessMatching =>
              (for {
                bm <- businessMatching
                rd <- bm.reviewDetails
              } yield {
                getNotificationRows(amlsRegNo) map { records =>
                  Ok(views.html.notifications.your_messages(rd.businessName, records))
                }
              }) getOrElse (throw new Exception("Cannot retrieve business name"))
            }
          }
          case _ => throw new Exception("amlsRegNo does not exist")
        }
      } else {
        Future.successful(NotFound(notFoundView))
      }
  }

  def getNotificationRows(amlsRegNo: String)(implicit hc: HeaderCarrier, ac: AuthContext): Future[Seq[NotificationRow]] =
    amlsNotificationConnector.fetchAllByAmlsRegNo(amlsRegNo) map { notifications =>
      notifications match {
        case s :: sc => notifications.sortWith((x, y) => x.receivedAt.isAfter(y.receivedAt))
        case _ => notifications
      }
    }

  def messageDetails(id: String) = Authorised.async {
    implicit authContext => implicit request =>
      authEnrolmentsService.amlsRegistrationNumber flatMap {
        case Some(regNo) => {
          amlsNotificationConnector.getMessageDetails(regNo, id) map {
            case Some(msg) => Ok(views.html.notifications.message_details("fghgk", msg.messageText))
            case None => NotFound(notFoundView)
          }
        }
        case _ => Future.successful(BadRequest)
      }

  }

}

object NotificationsController extends NotificationsController {
  override protected[controllers] val dataCacheConnector = DataCacheConnector
  override protected[controllers] val amlsNotificationConnector = AmlsNotificationConnector
  override protected[controllers] val authEnrolmentsService = AuthEnrolmentsService
  override protected val authConnector = AMLSAuthConnector
}