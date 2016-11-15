package models.securecommunications

import org.joda.time.LocalDateTime
import org.scalatest.WordSpec
import org.scalatest.matchers.MustMatchers
import play.api.libs.json.Json

class NotificationResponseSpec extends WordSpec with MustMatchers {
  private val testProcessingDate = new LocalDateTime(2001, 12, 17, 9, 30, 47)

  val notificationJson = Json.obj(
    "processingDate" -> "2001-12-17T09:30:47Z",
    "secureCommText" -> "Some text"
  )

  val notificationModel = NotificationResponse(testProcessingDate, "Some text")

  "Notification Response Serialisation" must {
    "NotificationResponse" must {
      "correctly deserialise" in {
        notificationJson.as[NotificationResponse] must be(notificationModel)
      }
    }
  }
}
