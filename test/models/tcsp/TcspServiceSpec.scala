package models.tcsp

import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.i18n.Messages

class TcspServiceSpec extends PlaySpec with MockitoSugar {

  "TcspService" should {

    "Provide the correct value" in {
      PhonecallHandling.value must be ("01")
      EmailHandling.value must be ("02")
      EmailServer.value must be ("03")
      SelfCollectMailboxes.value must be ("04")
      MailForwarding.value must be ("05")
      Receptionist.value must be ("06")
      ConferenceRooms.value must be ("07")
      Other("").value must be ("08")
    }

    "Provide the correct messages" in {
      val message = "tcsp.provided_services.service.lbl."
      PhonecallHandling.getMessage must be (Messages(message + "01"))
      EmailHandling.getMessage must be (Messages(message + "02"))
      EmailServer.getMessage must be (Messages(message + "03"))
      SelfCollectMailboxes.getMessage must be (Messages(message + "04"))
      MailForwarding.getMessage must be (Messages(message + "05"))
      Receptionist.getMessage must be (Messages(message + "06"))
      ConferenceRooms.getMessage must be (Messages(message + "07"))
      Other("test").getMessage must be (Messages(message + "08") + ":" + "test")
    }

  }

}