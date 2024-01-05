/*
 * Copyright 2024 HM Revenue & Customs
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

package models.notifications

import models.notifications.ContactType._
import org.scalatestplus.play.PlaySpec

class ContactTypeSpec  extends PlaySpec {

  val keyString = "key"

  "ContactType" must {
    "be created correctly from path parameter for RejectionReasons" in {

      ContactType.pathBinder.bind(keyString, RejectionReasons.toString) mustBe Right(RejectionReasons)
    }

    "be created correctly from path parameter for ApplicationApproval" in {

      ContactType.pathBinder.bind(keyString, ApplicationApproval.toString) mustBe Right(ApplicationApproval)
    }

    "be created correctly from path parameter for RenewalApproval" in {

      ContactType.pathBinder.bind(keyString, RenewalApproval.toString) mustBe Right(RenewalApproval)
    }

    "be created correctly from path parameter for NewRenewalReminder" in {

      ContactType.pathBinder.bind(keyString, NewRenewalReminder.toString) mustBe Right(NewRenewalReminder)
    }

    "be created correctly from path parameter for RevocationReasons" in {

      ContactType.pathBinder.bind(keyString, RevocationReasons.toString) mustBe Right(RevocationReasons)
    }


  }

}
