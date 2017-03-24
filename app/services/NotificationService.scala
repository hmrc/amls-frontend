package services

import javax.inject.{Inject, Singleton}

import connectors.AmlsNotificationConnector
import models.notifications.{ContactType, NotificationDetails, NotificationRow}
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class NotificationService @Inject()(val amlsNotificationConnector: AmlsNotificationConnector,val messagesApi:MessagesApi){

  def getNotifications(amlsRegNo: String)(implicit hc: HeaderCarrier, ac: AuthContext): Future[Seq[NotificationRow]] =
    amlsNotificationConnector.fetchAllByAmlsRegNo(amlsRegNo) map {
      case notifications@(s :: sc) => notifications.sortWith((x, y) => x.receivedAt.isAfter(y.receivedAt))
      case notifications => notifications
    }

  def getMessageDetails(amlsRegNo: String, id: String, contactType:ContactType)
                       (implicit hc: HeaderCarrier, ac: AuthContext): Future[Option[NotificationDetails]] = {

    contactType match {
      case ContactType.ApplicationAutorejectionForFailureToPay => Future.successful(
        Some(NotificationDetails(
          Some(contactType),
          None,
          Some(messagesApi("notification.static.text.application-auto-rejection-for-failure-to-pay",
          controllers.routes.StatusController.get())),
          false)
        )
      )
      case ContactType.RegistrationVariationApproval => Future.successful(
        Some(NotificationDetails(
          Some(contactType),
          None,
          Some(messagesApi("notification.static.text.registration-variation-approval",
            controllers.routes.StatusController.get())),
          false)
        )
      )
      case ContactType.DeRegistrationEffectiveDateChange => Future.successful(
        Some(NotificationDetails(
          Some(contactType),
          None,
          Some(messagesApi("notification.static.text.de-registration-effective-date-change",
            controllers.routes.StatusController.get())),
          false)
        )
      )

      case _ => amlsNotificationConnector.getMessageDetails(amlsRegNo, id)
    }

  }

}
