package connectors

import uk.gov.hmrc.play.http.HttpGet

trait NotificationConnnector {
  private[connectors] def get : HttpGet

  def getMessageDetails(amlsReferenceNumber: String, contactNumber: String) = {

  }
}
