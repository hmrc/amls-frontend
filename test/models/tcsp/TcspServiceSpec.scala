/*
 * Copyright 2018 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package models.tcsp

import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.i18n.Messages
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import utils.AmlsSpec

class TcspServiceSpec extends AmlsSpec {

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