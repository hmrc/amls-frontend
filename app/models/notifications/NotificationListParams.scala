package models.notifications

case class NotificationListParams(currentApplicationNotification: Seq[NotificationRow],
                                  previousApplicationNotification: Seq[NotificationRow]
                                 ) {

}
