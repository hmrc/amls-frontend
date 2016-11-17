package models.securecommunications

import org.joda.time.{DateTime, DateTimeZone}
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import org.specs2.mock.mockito.MockitoMatchers

class SecureCommunicationSpec extends PlaySpec with MockitoMatchers with OneAppPerSuite {

  val testSecureComms = SecureCommunication(None, None, None, false, new DateTime(2017, 12, 1, 1, 3, DateTimeZone.UTC), false)

  "Secure Communication" must {
    "retrieve the corresponding subject from messages" when {
      "message type is APA1" in {
        testSecureComms.copy(messageType = Some(APA1)).subject mustBe "Application Approval"
      }
      "message type is not given and variation is true" in {
        testSecureComms.copy(isVariation = true).subject mustBe "Registration Variation Approval"
      }
      "message type is not given and variation is false" in {
        testSecureComms.subject mustBe "Application Auto-Rejection for Failure to Pay"
      }
      "message type is APR1" in {
        testSecureComms.copy(messageType = Some(APR1)).subject mustBe "Renewal Approval"
      }
      "message type is REJR" in {
        testSecureComms.copy(messageType = Some(REJR)).subject mustBe "Application Rejection"
      }

      "message type is REVR" in {
        testSecureComms.copy(messageType = Some(REVR)).subject mustBe "Registration Revocation"
      }

      "message type is EXPR" in {
        testSecureComms.copy(messageType = Some(EXPR)).subject mustBe "Registration Expiry"
      }

      "message type is RPA1" in {
        testSecureComms.copy(messageType = Some(RPA1)).subject mustBe "Reminder to Pay - Application"
      }

      "message type is RPV1" in {
        testSecureComms.copy(messageType = Some(RPV1)).subject mustBe "Reminder to Pay - Variation"
      }

      "message type is RPR1" in {
        testSecureComms.copy(messageType = Some(RPR1)).subject mustBe "Reminder to Pay - Renewal"
      }
      "message type is RPM1" in {
        testSecureComms.copy(messageType = Some(RPM1)).subject mustBe "Reminder to Pay - Manual Charge"
      }
      "message type is RREM" in {
        testSecureComms.copy(messageType = Some(RREM)).subject mustBe "Reminder to Renew"
      }
      "message type is MTRJ" in {
        testSecureComms.copy(messageType = Some(MTRJ)).subject mustBe "Rejection of Application being Considered"
      }
      "message type is MTRV" in {
        testSecureComms.copy(messageType = Some(MTRV)).subject mustBe "Revocation of Registration being Considered"
      }
      "message type is NMRJ" in {
        testSecureComms.copy(messageType = Some(NMRJ)).subject mustBe "Rejection of Application no longer being Considered"
      }
      "message type is NMRV" in {
        testSecureComms.copy(messageType = Some(NMRV)).subject mustBe "Revocation of Registration no longer being Considered"
      }
      "message type is OTHR" in {
        testSecureComms.copy(messageType = Some(OTHR)).subject mustBe "Generic communication"
      }
    }
    "format the date for the table of messages" in {
      testSecureComms.dateReceived mustBe "1 December 2017"
    }
  }

}
